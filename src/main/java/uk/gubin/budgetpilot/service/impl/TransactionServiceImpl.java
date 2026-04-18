package uk.gubin.budgetpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.common.PageResult;
import uk.gubin.budgetpilot.dto.TransactionCreateDTO;
import uk.gubin.budgetpilot.dto.TransactionQueryDTO;
import uk.gubin.budgetpilot.dto.TransactionUpdateDTO;
import uk.gubin.budgetpilot.entity.Account;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Merchant;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.event.TransactionEvent;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.MerchantMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.service.AccountService;
import uk.gubin.budgetpilot.service.BudgetService;
import uk.gubin.budgetpilot.service.CurrencyRateService;
import uk.gubin.budgetpilot.service.MerchantService;
import uk.gubin.budgetpilot.service.TransactionService;
import uk.gubin.budgetpilot.vo.MerchantVO;
import uk.gubin.budgetpilot.vo.TransactionVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl extends ServiceImpl<TransactionMapper, Transaction> implements TransactionService {

    private final ObjectMapper objectMapper;
    private final CurrencyRateService currencyRateService;
    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;
    private final MerchantMapper merchantMapper;
    private final AccountService accountService;
    private final BudgetService budgetService;
    private final MerchantService merchantService;
    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public TransactionVO create(TransactionCreateDTO dto) {
        // 幂等校验：防止短时间重复提交
        String idempotentKey = "txn:duplicate:" + cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong()
                + ":" + dto.getType() + ":" + dto.getAmount() + ":" + dto.getAccountId()
                + ":" + dto.getCategoryId() + ":" + dto.getTransactionDate() + ":" + dto.getNote();
        String keyHash = cn.dev33.satoken.secure.SaSecureUtil.md5(idempotentKey);
        if (Boolean.FALSE.equals(redisTemplate.opsForValue().setIfAbsent("idempotency:" + keyHash, "1", 3, java.util.concurrent.TimeUnit.SECONDS))) {
            log.warn("Duplicate transaction submission detected: {}", keyHash);
            throw new BizException(ErrorCode.DUPLICATE_NAME, "交易");
        }
        // 校验账户
        Account account = accountMapper.selectById(dto.getAccountId());
        if (account == null) {
            throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        if (!account.getIsActive()) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 转账类型校验（在分类校验之前，避免同账户转账报分类错误）
        if (dto.getType() == 3) {
            if (dto.getTargetAccountId() == null) {
                throw new BizException(ErrorCode.TRANSACTION_TARGET_ACCOUNT_REQUIRED);
            }
            if (dto.getAccountId().equals(dto.getTargetAccountId())) {
                throw new BizException(ErrorCode.TRANSACTION_SAME_ACCOUNT);
            }
        }

        // 校验分类（转账不需要分类）
        Category category = null;
        if (dto.getType() != 3) {
            if (dto.getCategoryId() == null) {
                throw new BizException(ErrorCode.TRANSACTION_CATEGORY_NOT_FOUND);
            }
            category = categoryMapper.selectById(dto.getCategoryId());
            if (category == null) {
                throw new BizException(ErrorCode.TRANSACTION_CATEGORY_NOT_FOUND);
            }
            // 校验分类类型与交易类型匹配
            if (category.getType() != null && !category.getType().equals(dto.getType())) {
                throw new BizException(ErrorCode.TRANSACTION_CATEGORY_MISMATCH);
            }
        }

        // 商户处理逻辑
        Long merchantId = dto.getMerchantId();
        Merchant merchant = null;
        if (merchantId == null && dto.getMerchantName() != null && !dto.getMerchantName().trim().isEmpty()) {
            // 模糊匹配或自动创建商户
            MerchantVO merchantVO = merchantService.findOrCreate(
                    dto.getMerchantName().trim(),
                    dto.getCategoryId(),
                    Boolean.TRUE.equals(dto.getAutoCreateMerchant())
            );
            if (merchantVO != null) {
                merchantId = merchantVO.getId();
            }
        }

        // 验证商户是否存在
        if (merchantId != null) {
            merchant = merchantMapper.selectById(merchantId);
            if (merchant == null || !merchant.getIsActive()) {
                throw new BizException(ErrorCode.MERCHANT_NOT_FOUND);
            }
        }

        // 构建实体
        Transaction entity = buildEntity(dto, account, category, merchantId);
        baseMapper.insert(entity);

        // 更新商户使用统计
        if (merchantId != null && Boolean.TRUE.equals(entity.getIsConfirmed())) {
            merchantService.incrementUsage(merchantId);
        }

        // 只有确认的交易才更新账户余额
        if (Boolean.TRUE.equals(entity.getIsConfirmed())) {
            // 余额校验：仅对支出和转账类型，且仅对非信用卡账户（type=3 信用卡允许透支）
            if (entity.getType() == 1 || entity.getType() == 3) {
                Integer accountType = account.getType();
                if (accountType == null || accountType != 3) {
                    BigDecimal balance = account.getCurrentBalance();
                    if (balance != null && balance.compareTo(entity.getAmount()) < 0) {
                        throw new BizException(ErrorCode.ACCOUNT_BALANCE_NOT_ENOUGH);
                    }
                }
            }

            BigDecimal adjustAmount = entity.getAmount();
            switch (entity.getType()) {
                case 1 -> accountMapper.adjustBalance(entity.getAccountId(), adjustAmount.negate()); // 支出
                case 2 -> accountMapper.adjustBalance(entity.getAccountId(), adjustAmount);           // 收入
                case 3 -> {                                                                            // 转账
                    accountMapper.adjustBalance(entity.getAccountId(), adjustAmount.negate());
                    accountMapper.adjustBalance(entity.getTargetAccountId(), adjustAmount);
                }
            }

            // 异步事件：预算更新 + 预警检查
            eventPublisher.publishEvent(new TransactionEvent(this, entity, TransactionEvent.Action.CREATE));
        }

        log.info("Created transaction: {} {} {} (confirmed={})", entity.getType(), entity.getAmount(), entity.getCurrency(), entity.getIsConfirmed());
        return toVO(entity, account, category, merchant);
    }

    @Override
    public PageResult<TransactionVO> query(TransactionQueryDTO dto) {
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        // 只有明确指定 confirmed 参数时才过滤，否则查询全部
        if (dto.getConfirmed() != null) {
            query.eq(Transaction::getIsConfirmed, dto.getConfirmed());
        }

        if (dto.getType() != null) query.eq(Transaction::getType, dto.getType());
        if (dto.getAccountId() != null) query.eq(Transaction::getAccountId, dto.getAccountId());
        if (dto.getCategoryId() != null) {
            // 包含子分类
            query.and(q -> q.eq(Transaction::getCategoryId, dto.getCategoryId())
                    .or().in(Transaction::getCategoryId, getChildCategoryIds(dto.getCategoryId())));
        }
        if (dto.getStartDate() != null) query.ge(Transaction::getTransactionDate, dto.getStartDate());
        if (dto.getEndDate() != null) query.le(Transaction::getTransactionDate, dto.getEndDate());
        if (dto.getMinAmount() != null) query.ge(Transaction::getAmountBase, dto.getMinAmount());
        if (dto.getMaxAmount() != null) query.le(Transaction::getAmountBase, dto.getMaxAmount());
        if (dto.getCurrency() != null) query.eq(Transaction::getCurrency, dto.getCurrency());
        if (dto.getKeyword() != null) {
            query.and(q -> q.like(Transaction::getNote, dto.getKeyword()));
        }

        // 解析排序
        String[] sortParts = dto.getSort().split(",");
        String sortField = sortParts[0];
        boolean isAsc = sortParts.length < 2 || "asc".equalsIgnoreCase(sortParts[1]);

        Page<Transaction> page = new Page<>(dto.getPage(), dto.getSize());
        if (isAsc) {
            query.orderByAsc(mapSortField(sortField));
        } else {
            query.orderByDesc(mapSortField(sortField));
        }

        Page<Transaction> result = baseMapper.selectPage(page, query);

        // 批量加载关联数据
        Map<Long, Account> accountMap = loadAccounts(result.getRecords());
        Map<Long, Category> categoryMap = loadCategories(result.getRecords());
        Map<Long, Merchant> merchantMap = loadMerchants(result.getRecords());

        List<TransactionVO> items = result.getRecords().stream()
                .map(t -> toVO(t, accountMap.get(t.getAccountId()), categoryMap.get(t.getCategoryId()),
                        t.getMerchantId() != null ? merchantMap.get(t.getMerchantId()) : null))
                .collect(Collectors.toList());

        return PageResult.of(items, result.getTotal(), dto.getPage(), dto.getSize());
    }

    @Override
    public TransactionVO getById(Long id) {
        Transaction entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.TRANSACTION_NOT_FOUND);
        }
        Account account = accountMapper.selectById(entity.getAccountId());
        Category category = categoryMapper.selectById(entity.getCategoryId());
        Merchant merchant = entity.getMerchantId() != null ? merchantMapper.selectById(entity.getMerchantId()) : null;
        return toVO(entity, account, category, merchant);
    }

    @Override
    @Transactional
    public TransactionVO update(Long id, TransactionUpdateDTO dto) {
        Transaction entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.TRANSACTION_NOT_FOUND);
        }

        // 记录旧的交易日期和分类，用于跨月预算调整
        LocalDate oldDate = entity.getTransactionDate();
        Long oldCategoryId = entity.getCategoryId();

        // 回滚旧余额
        rollbackBalance(entity);

        // 商户处理逻辑（与 create 保持一致）
        Merchant merchant = null;
        Long merchantId = dto.getMerchantId();
        if (merchantId == null && dto.getMerchantName() != null && !dto.getMerchantName().trim().isEmpty()) {
            MerchantVO merchantVO = merchantService.findOrCreate(
                    dto.getMerchantName().trim(),
                    entity.getCategoryId(),
                    Boolean.TRUE.equals(dto.getAutoCreateMerchant())
            );
            if (merchantVO != null) {
                merchantId = merchantVO.getId();
            }
        }
        // 如果 merchantId 为 null，表示清空商户；否则验证商户是否有效
        if (merchantId != null) {
            merchant = merchantMapper.selectById(merchantId);
            if (merchant == null || !merchant.getIsActive()) {
                throw new BizException(ErrorCode.MERCHANT_NOT_FOUND);
            }
        }
        entity.setMerchantId(merchantId);

        // 更新字段
        if (dto.getType() != null) entity.setType(dto.getType());
        if (dto.getAmount() != null) entity.setAmount(dto.getAmount());
        if (dto.getCurrency() != null) entity.setCurrency(dto.getCurrency());
        if (dto.getTransactionDate() != null) entity.setTransactionDate(dto.getTransactionDate());
        if (dto.getTransactionTime() != null) entity.setTransactionTime(dto.getTransactionTime());
        if (dto.getNote() != null) entity.setNote(dto.getNote());
        if (dto.getAccountId() != null) entity.setAccountId(dto.getAccountId());
        if (dto.getTargetAccountId() != null) entity.setTargetAccountId(dto.getTargetAccountId());
        if (dto.getCategoryId() != null) entity.setCategoryId(dto.getCategoryId());
        if (dto.getTags() != null) {
            try { entity.setTags(objectMapper.writeValueAsString(dto.getTags())); } catch (Exception ignored) {}
        }
        if (dto.getAttachmentUrls() != null) {
            try { entity.setAttachmentUrls(objectMapper.writeValueAsString(dto.getAttachmentUrls())); } catch (Exception ignored) {}
        }
        if (dto.getIsConfirmed() != null) entity.setIsConfirmed(dto.getIsConfirmed());
        if (dto.getExtFields() != null) {
            try { entity.setExtFields(objectMapper.writeValueAsString(dto.getExtFields())); } catch (Exception ignored) {}
        }
        if (dto.getMetadata() != null) {
            try { entity.setMetadata(objectMapper.writeValueAsString(dto.getMetadata())); } catch (Exception ignored) {}
        }

        // 重新计算本位币金额
        Account account = accountMapper.selectById(entity.getAccountId());
        Category category = categoryMapper.selectById(entity.getCategoryId());
        calculateBaseAmount(entity, account);

        // 余额校验（与 create 保持一致）
        if (Boolean.TRUE.equals(entity.getIsConfirmed()) && (entity.getType() == 1 || entity.getType() == 3)) {
            if (account != null && account.getType() != null && account.getType() != 2) {
                if (account.getCurrentBalance() != null &&
                    account.getCurrentBalance().compareTo(entity.getAmount()) < 0) {
                    throw new BizException(ErrorCode.ACCOUNT_BALANCE_NOT_ENOUGH);
                }
            }
        }

        baseMapper.updateById(entity);

        // 重新应用余额
        applyBalance(entity);

        // 更新商户使用统计
        if (entity.getMerchantId() != null && Boolean.TRUE.equals(entity.getIsConfirmed())) {
            merchantService.incrementUsage(entity.getMerchantId());
        }

        // 跨月调整预算 spent：旧月退回，新月加上
        if (entity.getIsConfirmed() && entity.getType() == 1) {
            LocalDate newDate = entity.getTransactionDate();
            String oldYearMonth = oldDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            String newYearMonth = newDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            if (!oldYearMonth.equals(newYearMonth)) {
                Long oldTopId = getTopCategoryId(oldCategoryId);
                Long newTopId = getTopCategoryId(entity.getCategoryId());
                if (oldTopId != null) {
                    budgetService.updateItemSpent(oldTopId, oldYearMonth, entity.getAmountBase().negate());
                }
                if (newTopId != null) {
                    budgetService.updateItemSpent(newTopId, newYearMonth, entity.getAmountBase());
                }
            }
        }

        eventPublisher.publishEvent(new TransactionEvent(this, entity, TransactionEvent.Action.UPDATE));
        return toVO(entity, account, category, merchant);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Transaction entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.TRANSACTION_NOT_FOUND);
        }

        // 只有已确认的交易才回滚余额（未确认的交易从未影响余额）
        if (Boolean.TRUE.equals(entity.getIsConfirmed())) {
            rollbackBalance(entity);
        }
        baseMapper.deleteById(id);

        eventPublisher.publishEvent(new TransactionEvent(this, entity, TransactionEvent.Action.DELETE));
        log.info("Deleted transaction: {}", id);
    }

    @Override
    @Transactional
    public TransactionVO confirm(Long id) {
        Transaction entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.TRANSACTION_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(entity.getIsConfirmed())) {
            throw new BizException(ErrorCode.TRANSACTION_ALREADY_CONFIRMED);
        }

        entity.setIsConfirmed(true);
        baseMapper.updateById(entity);

        // 应用余额
        applyBalance(entity);

        // 发布事件以清除缓存和更新预算
        eventPublisher.publishEvent(new TransactionEvent(this, entity, TransactionEvent.Action.CREATE));

        Account account = accountMapper.selectById(entity.getAccountId());
        Category category = categoryMapper.selectById(entity.getCategoryId());
        Merchant merchant = entity.getMerchantId() != null ? merchantMapper.selectById(entity.getMerchantId()) : null;
        return toVO(entity, account, category, merchant);
    }

    @Override
    public Map<String, Object> getDailySpent(LocalDate date) {
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.eq(Transaction::getType, 1)
                .eq(Transaction::getIsConfirmed, true)
                .eq(Transaction::getTransactionDate, date);
        List<Transaction> txs = baseMapper.selectList(query);
        BigDecimal total = txs.stream().map(Transaction::getAmountBase).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("date", date.toString(), "totalSpent", total, "count", txs.size());
    }

    @Override
    public Map<String, Object> getMonthlySpent(String yearMonth) {
        LocalDate start = LocalDate.parse(yearMonth + "-01");
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.eq(Transaction::getType, 1)
                .eq(Transaction::getIsConfirmed, true)
                .ge(Transaction::getTransactionDate, start)
                .le(Transaction::getTransactionDate, end);
        List<Transaction> txs = baseMapper.selectList(query);
        BigDecimal total = txs.stream().map(Transaction::getAmountBase).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("month", yearMonth, "totalSpent", total, "count", txs.size());
    }

    @Override
    public Map<String, Object> getWeeklySpent(LocalDate date) {
        LocalDate weekStart = date.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = date.with(java.time.DayOfWeek.SUNDAY);
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.eq(Transaction::getType, 1)
                .eq(Transaction::getIsConfirmed, true)
                .ge(Transaction::getTransactionDate, weekStart)
                .le(Transaction::getTransactionDate, weekEnd);
        List<Transaction> txs = baseMapper.selectList(query);
        BigDecimal total = txs.stream().map(Transaction::getAmountBase).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("weekStart", weekStart.toString(), "totalSpent", total, "count", txs.size());
    }

    // ==================== Internal Methods ====================

    private Transaction buildEntity(TransactionCreateDTO dto, Account account, Category category, Long merchantId) {
        Transaction entity = new Transaction();
        entity.setType(dto.getType());
        entity.setAmount(dto.getAmount());
        entity.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : account.getCurrency());
        entity.setAccountId(dto.getAccountId());
        entity.setTargetAccountId(dto.getTargetAccountId());
        entity.setCategoryId(dto.getCategoryId());
        entity.setMerchantId(merchantId);
        entity.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDate.now());
        entity.setTransactionTime(dto.getTransactionTime());
        entity.setNote(dto.getNote());
        entity.setIsConfirmed(dto.getIsConfirmed());

        if (dto.getTags() != null) {
            try { entity.setTags(objectMapper.writeValueAsString(dto.getTags())); } catch (Exception ignored) {}
        }
        if (dto.getAttachmentUrls() != null) {
            try { entity.setAttachmentUrls(objectMapper.writeValueAsString(dto.getAttachmentUrls())); } catch (Exception ignored) {}
        }
        if (dto.getExtFields() != null) {
            try { entity.setExtFields(objectMapper.writeValueAsString(dto.getExtFields())); } catch (Exception ignored) {}
        }
        if (dto.getMetadata() != null) {
            try { entity.setMetadata(objectMapper.writeValueAsString(dto.getMetadata())); } catch (Exception ignored) {}
        }
        if (dto.getIsRecurring() != null) entity.setIsRecurring(dto.getIsRecurring());
        if (dto.getRecurringId() != null) entity.setRecurringId(dto.getRecurringId());

        // 计算本位币金额
        calculateBaseAmount(entity, account);

        return entity;
    }

    private void calculateBaseAmount(Transaction entity, Account account) {
        if ("CNY".equals(entity.getCurrency())) {
            entity.setAmountBase(entity.getAmount());
            entity.setExchangeRate(BigDecimal.ONE);
        } else {
            BigDecimal rate = currencyRateService.getRate(entity.getCurrency(), entity.getTransactionDate());
            if (rate == null) {
                rate = BigDecimal.ONE;
            }
            entity.setExchangeRate(rate);
            entity.setAmountBase(entity.getAmount().multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP));
        }
    }

    private void rollbackBalance(Transaction entity) {
        switch (entity.getType()) {
            case 1 -> accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount());       // 回滚支出
            case 2 -> accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount().negate()); // 回滚收入
            case 3 -> {                                                                               // 回滚转账
                accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount());
                accountMapper.adjustBalance(entity.getTargetAccountId(), entity.getAmount().negate());
            }
        }
    }

    private void applyBalance(Transaction entity) {
        switch (entity.getType()) {
            case 1 -> accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount().negate());
            case 2 -> accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount());
            case 3 -> {
                accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount().negate());
                accountMapper.adjustBalance(entity.getTargetAccountId(), entity.getAmount());
            }
        }
    }

    private Map<Long, Account> loadAccounts(List<Transaction> transactions) {
        List<Long> ids = transactions.stream()
                .map(Transaction::getAccountId).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return accountMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));
    }

    private Map<Long, Category> loadCategories(List<Transaction> transactions) {
        List<Long> ids = transactions.stream()
                .map(Transaction::getCategoryId).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return categoryMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
    }

    private Map<Long, Merchant> loadMerchants(List<Transaction> transactions) {
        List<Long> ids = transactions.stream()
                .map(Transaction::getMerchantId)
                .filter(id -> id != null)
                .distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return merchantMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Merchant::getId, m -> m));
    }

    private List<Long> getChildCategoryIds(Long parentId) {
        LambdaQueryWrapper<uk.gubin.budgetpilot.entity.Category> query = new LambdaQueryWrapper<>();
        query.eq(uk.gubin.budgetpilot.entity.Category::getParentId, parentId)
                .select(uk.gubin.budgetpilot.entity.Category::getId);
        return categoryMapper.selectList(query).stream()
                .map(uk.gubin.budgetpilot.entity.Category::getId).toList();
    }

    private Long getTopCategoryId(Long categoryId) {
        uk.gubin.budgetpilot.entity.Category cat = categoryMapper.selectById(categoryId);
        if (cat == null) return null;
        return cat.getParentId() == 0 ? cat.getId() : cat.getParentId();
    }

    private com.baomidou.mybatisplus.core.toolkit.support.SFunction<Transaction, ?> mapSortField(String field) {
        return switch (field) {
            case "amount" -> Transaction::getAmountBase;
            case "created_at" -> Transaction::getCreatedAt;
            default -> Transaction::getTransactionDate;
        };
    }

    private TransactionVO toVO(Transaction entity, Account account, Category category, Merchant merchant) {
        TransactionVO vo = new TransactionVO();
        vo.setId(entity.getId());
        vo.setType(entity.getType());
        vo.setAmount(entity.getAmount());
        vo.setCurrency(entity.getCurrency());
        vo.setAmountBase(entity.getAmountBase());
        vo.setExchangeRate(entity.getExchangeRate());
        vo.setAccountId(entity.getAccountId());
        vo.setTargetAccountId(entity.getTargetAccountId());
        vo.setCategoryId(entity.getCategoryId());
        vo.setMerchantId(entity.getMerchantId());
        vo.setTransactionDate(entity.getTransactionDate());
        vo.setTransactionTime(entity.getTransactionTime());
        vo.setNote(entity.getNote());
        vo.setIsConfirmed(entity.getIsConfirmed());
        vo.setIsRecurring(entity.getIsRecurring());
        vo.setRecurringId(entity.getRecurringId());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());

        if (account != null) {
            vo.setAccountName(account.getName());
        }
        if (entity.getTargetAccountId() != null) {
            Account target = accountMapper.selectById(entity.getTargetAccountId());
            if (target != null) vo.setTargetAccountName(target.getName());
        }
        if (category != null) {
            vo.setCategoryName(category.getName());
            vo.setCategoryIcon(category.getIcon());
            vo.setCategoryColor(category.getColor());
        }

        // 商户信息
        if (merchant != null) {
            vo.setMerchantName(merchant.getName());
            vo.setMerchantColor(merchant.getColor());
        } else if (entity.getMerchantId() != null) {
            Merchant m = merchantMapper.selectById(entity.getMerchantId());
            if (m != null) {
                vo.setMerchantName(m.getName());
                vo.setMerchantColor(m.getColor());
            }
        }

        if (entity.getTags() != null) {
            try { vo.setTags(objectMapper.readValue(entity.getTags(), new TypeReference<>() {})); } catch (Exception ignored) {}
        }
        if (entity.getAttachmentUrls() != null) {
            try { vo.setAttachmentUrls(objectMapper.readValue(entity.getAttachmentUrls(), new TypeReference<>() {})); } catch (Exception ignored) {}
        }
        if (entity.getExtFields() != null) {
            try { vo.setExtFields(objectMapper.readValue(entity.getExtFields(), new TypeReference<>() {})); } catch (Exception ignored) {}
        }
        if (entity.getMetadata() != null) {
            try { vo.setMetadata(objectMapper.readValue(entity.getMetadata(), new TypeReference<>() {})); } catch (Exception ignored) {}
        }

        return vo;
    }
}
