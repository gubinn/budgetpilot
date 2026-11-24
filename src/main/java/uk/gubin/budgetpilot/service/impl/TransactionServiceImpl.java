package uk.gubin.budgetpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.common.PageResult;
import uk.gubin.budgetpilot.dto.TransactionAnnotateDTO;
import uk.gubin.budgetpilot.dto.TransactionCreateDTO;
import uk.gubin.budgetpilot.dto.TransactionQueryDTO;
import uk.gubin.budgetpilot.dto.TransactionReclassifyDTO;
import uk.gubin.budgetpilot.dto.TransactionUpdateDTO;
import uk.gubin.budgetpilot.entity.Account;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Merchant;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.event.TransactionEvent;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.MerchantMapper;
import uk.gubin.budgetpilot.service.reconciliation.ReconciliationStatsUpdater;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.service.AccountService;
import uk.gubin.budgetpilot.service.BudgetService;
import uk.gubin.budgetpilot.service.CurrencyRateService;
import uk.gubin.budgetpilot.service.MerchantService;
import uk.gubin.budgetpilot.service.TransactionService;
import uk.gubin.budgetpilot.service.reconciliation.ReconcileConstants;
import uk.gubin.budgetpilot.vo.MerchantVO;
import uk.gubin.budgetpilot.vo.TransactionSummaryVO;
import uk.gubin.budgetpilot.vo.TransactionVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl extends ServiceImpl<TransactionMapper, Transaction> implements TransactionService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ObjectMapper objectMapper;
    private final CurrencyRateService currencyRateService;
    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;
    private final MerchantMapper merchantMapper;
    private final ReconciliationStatsUpdater reconciliationStatsUpdater;
    private final AccountService accountService;
    private final BudgetService budgetService;
    private final MerchantService merchantService;
    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public TransactionVO create(TransactionCreateDTO dto) {
        // 外部幂等：同一用户的相同 externalId 直接返回已有交易，不重复入账
        String externalId = normalizeExternalId(dto.getExternalId());
        if (externalId != null) {
            Transaction existing = findByExternalId(externalId);
            if (existing != null) {
                log.info("Idempotent hit for externalId={}, returning transaction {}", externalId, existing.getId());
                return getById(existing.getId());
            }
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
            // 0 元自转账用于购汇等内部换算记录：不对余额产生任何影响
            if (dto.getAccountId().equals(dto.getTargetAccountId())
                    && dto.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                throw new BizException(ErrorCode.TRANSACTION_SAME_ACCOUNT);
            }
        }
        // 支出/收入/退款必须大于 0；转账允许 0（自转账记录）
        if (dto.getType() != 3 && dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非转账交易金额必须大于0");
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
            // 校验分类类型与交易类型匹配；退款(type=4)属于支出分类
            int expectedCategoryType = dto.getType() == 4 ? 1 : dto.getType();
            if (category.getType() != null && !category.getType().equals(expectedCategoryType)) {
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
        entity.setExternalId(externalId);
        try {
            baseMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            // 并发下两个请求同时通过幂等预查，唯一索引兜底；返回已创建的那条
            if (externalId != null) {
                Transaction existing = findByExternalId(externalId);
                if (existing != null) {
                    log.info("Concurrent idempotent hit for externalId={}, returning transaction {}",
                            externalId, existing.getId());
                    return getById(existing.getId());
                }
            }
            throw e;
        }

        // 更新商户使用统计
        if (merchantId != null && Boolean.TRUE.equals(entity.getIsConfirmed())) {
            merchantService.incrementUsage(merchantId);
        }

        // 只有确认的交易才更新账户余额
        if (Boolean.TRUE.equals(entity.getIsConfirmed())) {
            BigDecimal adjustAmount = entity.getAmount();
            boolean needsBalanceCheck = (entity.getType() == 1 || entity.getType() == 3)
                    && account.getType() != null && account.getType() != 3; // 非信用卡需检查余额

            switch (entity.getType()) {
                case 1 -> { // 支出：原子扣减
                    if (needsBalanceCheck) {
                        int rows = accountMapper.decreaseBalanceIfSufficient(entity.getAccountId(), adjustAmount);
                        if (rows == 0) {
                            throw new BizException(ErrorCode.ACCOUNT_BALANCE_NOT_ENOUGH);
                        }
                    } else {
                        accountMapper.adjustBalance(entity.getAccountId(), adjustAmount.negate());
                    }
                }
                case 2 -> accountMapper.adjustBalance(entity.getAccountId(), adjustAmount);           // 收入
                case 4 -> accountMapper.adjustBalance(entity.getAccountId(), adjustAmount);           // 退款：资金回到账户
                case 3 -> {                                                                            // 转账
                    // 源账户原子扣减
                    if (needsBalanceCheck) {
                        int rows = accountMapper.decreaseBalanceIfSufficient(entity.getAccountId(), adjustAmount);
                        if (rows == 0) {
                            throw new BizException(ErrorCode.ACCOUNT_BALANCE_NOT_ENOUGH);
                        }
                    } else {
                        accountMapper.adjustBalance(entity.getAccountId(), adjustAmount.negate());
                    }
                    // 目标账户增加（不需要余额检查）
                    accountMapper.adjustBalance(entity.getTargetAccountId(), adjustAmount);
                }
            }

            // 异步事件：预算更新 + 预警检查
            eventPublisher.publishEvent(new TransactionEvent(this, entity, TransactionEvent.Action.CREATE));

            // 事务提交后同步清除报表缓存（确保查询能看到新数据）
            registerCacheClearSync(entity.getTransactionDate());
        }

        log.info("Created transaction: id={} type={} amount={} currency={} confirmed={}",
                entity.getId(), entity.getType(), entity.getAmount(), entity.getCurrency(), entity.getIsConfirmed());
        return toVO(entity, account, category, merchant);
    }

    @Override
    public PageResult<TransactionVO> query(TransactionQueryDTO dto) {
        LambdaQueryWrapper<Transaction> query = buildQuery(dto);

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
        Map<Long, Category> parentCategoryMap = loadParentCategories(categoryMap.values());
        Map<Long, Merchant> merchantMap = loadMerchants(result.getRecords());

        List<TransactionVO> items = result.getRecords().stream()
                .map(t -> {
                    Category category = t.getCategoryId() != null ? categoryMap.get(t.getCategoryId()) : null;
                    Category parent = category != null && category.getParentId() != null
                            && category.getParentId() != 0 ? parentCategoryMap.get(category.getParentId()) : null;
                    return toVO(t,
                            accountMap.get(t.getAccountId()),
                            category,
                            t.getMerchantId() != null ? merchantMap.get(t.getMerchantId()) : null,
                            parent, accountMap);
                })
                .collect(Collectors.toList());

        return PageResult.of(items, result.getTotal(), dto.getPage(), dto.getSize());
    }

    @Override
    public TransactionSummaryVO summary(TransactionQueryDTO dto) {
        LambdaQueryWrapper<Transaction> query = buildQuery(dto);
        query.select(Transaction::getType, Transaction::getAmount, Transaction::getAmountBase);

        TransactionSummaryVO vo = new TransactionSummaryVO();
        for (Transaction t : baseMapper.selectList(query)) {
            BigDecimal amount = t.getAmountBase() != null ? t.getAmountBase() : t.getAmount();
            if (amount == null) amount = BigDecimal.ZERO;
            vo.setCount(vo.getCount() + 1);
            if (t.getType() == null) continue;
            switch (t.getType()) {
                case 1 -> {
                    vo.setExpense(vo.getExpense().add(amount));
                    vo.setExpenseCount(vo.getExpenseCount() + 1);
                }
                case 2 -> {
                    vo.setIncome(vo.getIncome().add(amount));
                    vo.setIncomeCount(vo.getIncomeCount() + 1);
                }
                case 3 -> {
                    vo.setTransfer(vo.getTransfer().add(amount));
                    vo.setTransferCount(vo.getTransferCount() + 1);
                }
                case 4 -> {
                    vo.setRefund(vo.getRefund().add(amount));
                    vo.setRefundCount(vo.getRefundCount() + 1);
                }
                default -> {
                }
            }
        }
        vo.setNetExpense(vo.getExpense().subtract(vo.getRefund()));
        vo.setNet(vo.getIncome().subtract(vo.getNetExpense()));
        return vo;
    }

    /** 列表查询与汇总共用同一套筛选条件，保证两处口径一致 */
    private LambdaQueryWrapper<Transaction> buildQuery(TransactionQueryDTO dto) {
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        // 只有明确指定 confirmed 参数时才过滤，否则查询全部
        if (dto.getConfirmed() != null) {
            query.eq(Transaction::getIsConfirmed, dto.getConfirmed());
        }

        if (dto.getType() != null) query.eq(Transaction::getType, dto.getType());
        if (dto.getReconciled() != null) query.eq(Transaction::getReconciled, dto.getReconciled());
        if (dto.getReviewStatus() != null && !dto.getReviewStatus().isBlank()) {
            query.eq(Transaction::getReviewStatus, dto.getReviewStatus().trim().toUpperCase());
        }
        if (dto.getAccountId() != null) {
            // 账户筛选要包含“转入”交易：目标账户也是该账户
            query.and(q -> q.eq(Transaction::getAccountId, dto.getAccountId())
                    .or().eq(Transaction::getTargetAccountId, dto.getAccountId()));
        }
        if (dto.getCategoryId() != null) {
            // 包含子分类
            List<Long> childIds = getChildCategoryIds(dto.getCategoryId());
            if (childIds.isEmpty()) {
                query.eq(Transaction::getCategoryId, dto.getCategoryId());
            } else {
                query.and(q -> q.eq(Transaction::getCategoryId, dto.getCategoryId())
                        .or().in(Transaction::getCategoryId, childIds));
            }
        }
        if (dto.getStartDate() != null) query.ge(Transaction::getTransactionDate, dto.getStartDate());
        if (dto.getEndDate() != null) query.le(Transaction::getTransactionDate, dto.getEndDate());
        if (dto.getMinAmount() != null) query.ge(Transaction::getAmountBase, dto.getMinAmount());
        if (dto.getMaxAmount() != null) query.le(Transaction::getAmountBase, dto.getMaxAmount());
        if (dto.getCurrency() != null) query.eq(Transaction::getCurrency, dto.getCurrency());
        if (dto.getKeyword() != null) {
            query.and(q -> q.like(Transaction::getNote, dto.getKeyword()));
        }
        // 标签过滤：任意一个标签命中即可
        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            List<String> tags = dto.getTags().stream()
                    .filter(t -> t != null && !t.isBlank())
                    .map(String::trim).distinct().toList();
            if (!tags.isEmpty()) {
                query.and(q -> {
                    for (int i = 0; i < tags.size(); i++) {
                        if (i == 0) {
                            q.apply("JSON_CONTAINS(tags, JSON_QUOTE({0}))", tags.get(i));
                        } else {
                            q.or().apply("JSON_CONTAINS(tags, JSON_QUOTE({0}))", tags.get(i));
                        }
                    }
                });
            }
        }
        // 扩展字段精确查询：extKey 必填，extValue 可选（只传 key 时按存在性过滤）
        if (dto.getExtKey() != null && !dto.getExtKey().isBlank()) {
            String path = "$." + dto.getExtKey().trim();
            if (dto.getExtValue() != null) {
                query.apply("JSON_UNQUOTE(JSON_EXTRACT(ext_fields, {0})) = {1}", path, dto.getExtValue());
            } else {
                query.apply("JSON_CONTAINS_PATH(ext_fields, 'one', {0})", path);
            }
        }
        return query;
    }

    @Override
    public TransactionVO getById(Long id) {
        Transaction entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.TRANSACTION_NOT_FOUND);
        }
        Account account = accountMapper.selectById(entity.getAccountId());
        Category category = entity.getCategoryId() != null ? categoryMapper.selectById(entity.getCategoryId()) : null;
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
        ensureEditable(entity);

        // 记录旧的交易字段，用于预算重算
        LocalDate oldDate = entity.getTransactionDate();
        LocalTime oldTime = entity.getTransactionTime();
        String oldCurrency = entity.getCurrency();
        Long oldCategoryId = entity.getCategoryId();
        Long oldAccountId = entity.getAccountId();
        Integer oldType = entity.getType();
        BigDecimal oldAmount = entity.getAmount();
        boolean wasConfirmed = Boolean.TRUE.equals(entity.getIsConfirmed());
        boolean wasReconciled = Boolean.TRUE.equals(entity.getReconciled());

        // 只有已确认的交易才需要回滚余额
        if (wasConfirmed) {
            rollbackBalance(entity);
        }

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
        // 只有显式传了 merchantId 或 merchantName 才改变商户关联；
        // 只改分类等字段时保留原商户，避免误清空
        if (dto.getMerchantId() != null || dto.getMerchantName() != null) {
            entity.setMerchantId(merchantId);
        }

        // 更新字段
        if (dto.getType() != null) entity.setType(dto.getType());
        if (dto.getAmount() != null) entity.setAmount(dto.getAmount());
        if (dto.getCurrency() != null) entity.setCurrency(dto.getCurrency());
        if (dto.getTransactionDate() != null) entity.setTransactionDate(dto.getTransactionDate());
        if (dto.getTransactionTime() != null) entity.setTransactionTime(dto.getTransactionTime());
        if (dto.getNote() != null) entity.setNote(dto.getNote());
        if (dto.getAccountId() != null) entity.setAccountId(dto.getAccountId());
        if (dto.getTargetAccountId() != null) entity.setTargetAccountId(dto.getTargetAccountId());
        if (dto.getCategoryId() != null) {
            entity.setCategoryId(dto.getCategoryId());
        } else if (Boolean.TRUE.equals(dto.getClearCategory())) {
            // 自己转自己时不需要收支分类；显式置空，避免沿用导入时的“借出款”
            entity.setCategoryId(null);
        }

        // 学习闭环：用户修改交易分类时，同步更新关联商户的默认分类
        if (dto.getCategoryId() != null && entity.getMerchantId() != null
                && Boolean.TRUE.equals(dto.getSyncMerchantCategory())) {
            Merchant linked = merchantMapper.selectById(entity.getMerchantId());
            if (linked != null && !dto.getCategoryId().equals(linked.getCategoryId())) {
                linked.setCategoryId(dto.getCategoryId());
                merchantMapper.updateById(linked);
                log.info("Learned merchant '{}' -> category {}", linked.getName(), dto.getCategoryId());
            }
        }

        if (dto.getTags() != null) {
            try { entity.setTags(objectMapper.writeValueAsString(dto.getTags())); } catch (Exception e) { log.warn("Failed to serialize tags", e); }
        }
        if (dto.getAttachmentUrls() != null) {
            try { entity.setAttachmentUrls(objectMapper.writeValueAsString(dto.getAttachmentUrls())); } catch (Exception e) { log.warn("Failed to serialize attachmentUrls", e); }
        }
        if (dto.getIsConfirmed() != null) entity.setIsConfirmed(dto.getIsConfirmed());
        if (dto.getReviewStatus() != null) entity.setReviewStatus(normalizeReviewStatus(dto.getReviewStatus()));
        if (dto.getExtFields() != null) {
            try { entity.setExtFields(objectMapper.writeValueAsString(dto.getExtFields())); } catch (Exception e) { log.warn("Failed to serialize extFields", e); }
        }
        if (dto.getMetadata() != null) {
            try { entity.setMetadata(objectMapper.writeValueAsString(dto.getMetadata())); } catch (Exception e) { log.warn("Failed to serialize metadata", e); }
        }

        // 校验账户与金额：update 允许改账户/类型，必须和 create 使用同一套规则，
        // 否则可能把交易改到不存在的账户，或产生缺少目标账户的转账。
        Account account = accountMapper.selectById(entity.getAccountId());
        if (account == null) {
            throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        // 只改备注/分类时，历史交易所属账户可能已停用，允许继续编辑；
        // 但把交易改到另一个账户时，目标账户必须是启用状态。
        boolean accountChanged = !Objects.equals(oldAccountId, entity.getAccountId());
        if (accountChanged && !account.getIsActive()) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (entity.getType() == 3) {
            if (entity.getTargetAccountId() == null) {
                throw new BizException(ErrorCode.TRANSACTION_TARGET_ACCOUNT_REQUIRED);
            }
            if (entity.getAccountId().equals(entity.getTargetAccountId())
                    && entity.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                throw new BizException(ErrorCode.TRANSACTION_SAME_ACCOUNT);
            }
            if (accountMapper.selectById(entity.getTargetAccountId()) == null) {
                throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
            }
        } else if (entity.getAmount() != null
                && entity.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非转账交易金额必须大于0");
        }

        // 重新计算本位币金额
        Category category = entity.getCategoryId() != null ? categoryMapper.selectById(entity.getCategoryId()) : null;
        if (category != null && entity.getType() != null && entity.getType() != 3) {
            int expectedCategoryType = entity.getType() == 4 ? 1 : entity.getType();
            if (category.getType() != null && !category.getType().equals(expectedCategoryType)) {
                throw new BizException(ErrorCode.TRANSACTION_CATEGORY_MISMATCH);
            }
        }
        calculateBaseAmount(entity, account);

        // 只有确认的交易才应用余额变动
        boolean nowConfirmed = Boolean.TRUE.equals(entity.getIsConfirmed());
        if (nowConfirmed) {
            boolean needsBalanceCheck = (entity.getType() == 1 || entity.getType() == 3)
                    && account != null && account.getType() != null && account.getType() != 3;

            switch (entity.getType()) {
                case 1 -> { // 支出
                    if (needsBalanceCheck) {
                        int rows = accountMapper.decreaseBalanceIfSufficient(entity.getAccountId(), entity.getAmount());
                        if (rows == 0) {
                            throw new BizException(ErrorCode.ACCOUNT_BALANCE_NOT_ENOUGH);
                        }
                    } else {
                        accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount().negate());
                    }
                }
                case 2 -> accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount());
                case 4 -> accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount()); // 退款
                case 3 -> { // 转账
                    if (needsBalanceCheck) {
                        int rows = accountMapper.decreaseBalanceIfSufficient(entity.getAccountId(), entity.getAmount());
                        if (rows == 0) {
                            throw new BizException(ErrorCode.ACCOUNT_BALANCE_NOT_ENOUGH);
                        }
                    } else {
                        accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount().negate());
                    }
                    accountMapper.adjustBalance(entity.getTargetAccountId(), entity.getAmount());
                }
            }
        }

        baseMapper.updateById(entity);

        // MyBatis-Plus updateById 默认忽略 null 字段，清空分类需要显式 update。
        if (Boolean.TRUE.equals(dto.getClearCategory()) && dto.getCategoryId() == null) {
            baseMapper.update(null, new LambdaUpdateWrapper<Transaction>()
                    .eq(Transaction::getId, id)
                    .set(Transaction::getCategoryId, null));
        }

        // 对账是快照：金额/日期/时间/币种/账户/类型变了，原来的对账关系不再成立。
        // 把账单行退回待处理，避免继续挂着「已对账」造成账实不符。
        boolean reconcileRelevantChange = !Objects.equals(oldType, entity.getType())
                || oldAmount == null || entity.getAmount() == null
                || oldAmount.compareTo(entity.getAmount()) != 0
                || !Objects.equals(oldCurrency, entity.getCurrency())
                || !Objects.equals(oldDate, entity.getTransactionDate())
                || !Objects.equals(oldTime, entity.getTransactionTime())
                || !Objects.equals(oldAccountId, entity.getAccountId());
        if (wasReconciled && reconcileRelevantChange) {
            reconciliationStatsUpdater.detachTransaction(id, ReconcileConstants.IGNORE_TRANSACTION_CHANGED);
            baseMapper.clearReconciled(id);
            entity.setReconciled(false);
            entity.setReconcileChannel(null);
            entity.setReconcileBatchId(null);
            entity.setReconciledAt(null);
            log.info("Reconciliation detached because transaction {} was modified materially", id);
        }

        // 更新商户使用统计
        if (entity.getMerchantId() != null && Boolean.TRUE.equals(entity.getIsConfirmed())) {
            merchantService.incrementUsage(entity.getMerchantId());
        }

        // 预算 spent：编辑可能同时改变金额、分类、月份或类型，增量无法正确表达；
        // 按交易事实重算旧/新两侧，幂等且不会重复累加。
        String oldMonth = oldDate != null ? oldDate.format(MONTH_FMT) : null;
        String newMonth = entity.getTransactionDate() != null
                ? entity.getTransactionDate().format(MONTH_FMT) : null;
        boolean budgetRelevantChange = !Objects.equals(oldType, entity.getType())
                || oldAmount == null || entity.getAmount() == null
                || oldAmount.compareTo(entity.getAmount()) != 0
                || !Objects.equals(oldCategoryId, entity.getCategoryId())
                || !Objects.equals(oldMonth, newMonth);
        if (budgetRelevantChange) {
            boolean oldAffectsBudget = wasConfirmed && oldCategoryId != null && oldMonth != null
                    && oldType != null && (oldType == 1 || oldType == 4);
            boolean newAffectsBudget = Boolean.TRUE.equals(entity.getIsConfirmed())
                    && entity.getCategoryId() != null && newMonth != null
                    && (entity.getType() == 1 || entity.getType() == 4);
            if (oldAffectsBudget) {
                budgetService.recalculateItemSpent(oldCategoryId, oldMonth);
            }
            if (newAffectsBudget) {
                budgetService.recalculateItemSpent(entity.getCategoryId(), newMonth);
            }
        }

        eventPublisher.publishEvent(new TransactionEvent(this, entity, TransactionEvent.Action.UPDATE));
        registerCacheClearSync(entity.getTransactionDate());
        log.info("Updated transaction: id={} type={} amount={} category={} account={} confirmed={}",
                id, entity.getType(), entity.getAmount(), entity.getCategoryId(),
                entity.getAccountId(), entity.getIsConfirmed());
        return toVO(entity, account, category, merchant);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Transaction entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.TRANSACTION_NOT_FOUND);
        }
        ensureEditable(entity);

        // 只有已确认的交易才回滚余额（未确认的交易从未影响余额）
        if (Boolean.TRUE.equals(entity.getIsConfirmed())) {
            rollbackBalance(entity);
        }
        baseMapper.deleteById(id);

        if (Boolean.TRUE.equals(entity.getReconciled())) {
            reconciliationStatsUpdater.detachTransaction(id, ReconcileConstants.IGNORE_TRANSACTION_CHANGED);
        }

        eventPublisher.publishEvent(new TransactionEvent(this, entity, TransactionEvent.Action.DELETE));
        registerCacheClearSync(entity.getTransactionDate());
        log.info("Deleted transaction: {}", id);
    }

    @Override
    @Transactional
    public TransactionVO confirm(Long id) {
        Transaction entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.TRANSACTION_NOT_FOUND);
        }
        ensureEditable(entity);
        if (Boolean.TRUE.equals(entity.getIsConfirmed())) {
            throw new BizException(ErrorCode.TRANSACTION_ALREADY_CONFIRMED);
        }

        // 余额校验：确认前检查账户余额是否充足（信用卡账户允许透支）
        Account account = accountMapper.selectById(entity.getAccountId());
        boolean needsBalanceCheck = (entity.getType() == 1 || entity.getType() == 3)
                && account != null && account.getType() != null && account.getType() != 3;

        entity.setIsConfirmed(true);
        baseMapper.updateById(entity);

        // 原子应用余额
        switch (entity.getType()) {
            case 1 -> { // 支出
                if (needsBalanceCheck) {
                    int rows = accountMapper.decreaseBalanceIfSufficient(entity.getAccountId(), entity.getAmount());
                    if (rows == 0) {
                        throw new BizException(ErrorCode.ACCOUNT_BALANCE_NOT_ENOUGH);
                    }
                } else {
                    accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount().negate());
                }
            }
            case 2 -> accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount());
            case 4 -> accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount()); // 退款
            case 3 -> { // 转账
                if (needsBalanceCheck) {
                    int rows = accountMapper.decreaseBalanceIfSufficient(entity.getAccountId(), entity.getAmount());
                    if (rows == 0) {
                        throw new BizException(ErrorCode.ACCOUNT_BALANCE_NOT_ENOUGH);
                    }
                } else {
                    accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount().negate());
                }
                accountMapper.adjustBalance(entity.getTargetAccountId(), entity.getAmount());
            }
        }

        // 发布事件以清除缓存和更新预算
        eventPublisher.publishEvent(new TransactionEvent(this, entity, TransactionEvent.Action.CREATE));

        // 事务提交后同步清除报表缓存
        registerCacheClearSync(entity.getTransactionDate());
        log.info("Confirmed transaction: id={} type={} amount={}",
                id, entity.getType(), entity.getAmount());

        Category category = entity.getCategoryId() != null ? categoryMapper.selectById(entity.getCategoryId()) : null;
        Merchant merchant = entity.getMerchantId() != null ? merchantMapper.selectById(entity.getMerchantId()) : null;

        return toVO(entity, account, category, merchant);
    }

    @Override
    @Transactional
    public int reclassify(TransactionReclassifyDTO dto) {
        int updated = 0;
        Set<String> months = new HashSet<>();
        for (TransactionReclassifyDTO.Item item : dto.getItems()) {
            Transaction entity = baseMapper.selectById(item.getId());
            if (entity == null || entity.getType() == null || entity.getType() == 3) {
                continue;
            }
            Category category = categoryMapper.selectById(item.getCategoryId());
            if (category == null) {
                continue;
            }
            int expectedCategoryType = entity.getType() == 4 ? 1 : entity.getType();
            if (category.getType() != null && !category.getType().equals(expectedCategoryType)) {
                continue;
            }
            if (item.getCategoryId().equals(entity.getCategoryId())) {
                continue;
            }
            // 只更新分类，不触碰余额/预算/预警
            baseMapper.update(null, new LambdaUpdateWrapper<Transaction>()
                    .eq(Transaction::getId, entity.getId())
                    .set(Transaction::getCategoryId, item.getCategoryId())
                    .set(Transaction::getUpdatedAt, LocalDateTime.now()));
            if (entity.getTransactionDate() != null) {
                months.add(entity.getTransactionDate().format(MONTH_FMT));
            }
            updated++;
        }
        Long userId = getCurrentUserId();
        for (String month : months) {
            clearReportCache(month, userId);
        }
        log.info("Reclassified {} transactions", updated);
        return updated;
    }

    @Override
    public List<String> listTags() {
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.select(Transaction::getTags).isNotNull(Transaction::getTags);
        Set<String> tags = new java.util.TreeSet<>();
        for (Transaction row : baseMapper.selectList(query)) {
            if (row.getTags() == null) continue;
            try {
                List<String> parsed = objectMapper.readValue(row.getTags(), new TypeReference<List<String>>() {});
                for (String tag : parsed) {
                    if (tag != null && !tag.isBlank()) tags.add(tag.trim());
                }
            } catch (Exception ignored) {
                // 忽略历史脏数据
            }
        }
        return new java.util.ArrayList<>(tags);
    }

    @Override
    public List<String> listExtFieldKeys() {
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.select(Transaction::getExtFields).isNotNull(Transaction::getExtFields);
        Set<String> keys = new java.util.TreeSet<>();
        for (Transaction row : baseMapper.selectList(query)) {
            if (row.getExtFields() == null) continue;
            try {
                Map<String, Object> map = objectMapper.readValue(
                        row.getExtFields(), new TypeReference<Map<String, Object>>() {});
                keys.addAll(map.keySet());
            } catch (Exception ignored) {
                // 忽略历史脏数据
            }
        }
        return new ArrayList<>(keys);
    }

    @Override
    @Transactional
    public int annotate(TransactionAnnotateDTO dto) {
        int updated = 0;
        Set<String> months = new HashSet<>();
        for (TransactionAnnotateDTO.Item item : dto.getItems()) {
            Transaction tx = baseMapper.selectById(item.getId());
            if (tx == null) continue;

            List<String> tags = parseTags(tx.getTags());
            boolean changed = false;
            if (item.getAddTags() != null) {
                for (String raw : item.getAddTags()) {
                    if (raw == null || raw.isBlank()) continue;
                    String tag = raw.trim();
                    if (!tags.contains(tag)) {
                        tags.add(tag);
                        changed = true;
                    }
                }
            }

            Map<String, Object> ext = parseExtFields(tx.getExtFields());
            if (item.getExtFields() != null) {
                for (Map.Entry<String, Object> entry : item.getExtFields().entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (key == null || key.isBlank() || value == null) continue;
                    if (!value.equals(ext.get(key))) {
                        ext.put(key, value);
                        changed = true;
                    }
                }
            }

            if (!changed) continue;
            try {
                baseMapper.update(null, new LambdaUpdateWrapper<Transaction>()
                        .eq(Transaction::getId, tx.getId())
                        .set(Transaction::getTags, objectMapper.writeValueAsString(tags))
                        .set(Transaction::getExtFields, objectMapper.writeValueAsString(ext))
                        .set(Transaction::getUpdatedAt, LocalDateTime.now()));
                if (tx.getTransactionDate() != null) {
                    months.add(tx.getTransactionDate().format(MONTH_FMT));
                }
                updated++;
            } catch (Exception e) {
                log.warn("Failed to annotate transaction {}", tx.getId(), e);
            }
        }
        Long userId = getCurrentUserId();
        for (String month : months) {
            clearReportCache(month, userId);
        }
        log.info("Annotated {} transactions", updated);
        return updated;
    }

    private List<String> parseTags(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return new ArrayList<>(objectMapper.readValue(json, new TypeReference<List<String>>() {}));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Map<String, Object> parseExtFields(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return new LinkedHashMap<>(objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {}));
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    @Override
    public Map<String, Object> getDailySpent(LocalDate date) {
        Map<String, Object> row = baseMapper.sumDailyExpense(date);
        return Map.of("date", date.toString(), "totalSpent", mapTotal(row), "count", mapCount(row));
    }

    @Override
    public Map<String, Object> getMonthlySpent(String yearMonth) {
        LocalDate start = LocalDate.parse(yearMonth + "-01");
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        Map<String, Object> row = baseMapper.sumExpenseBetween(start, end);
        return Map.of("month", yearMonth, "totalSpent", mapTotal(row), "count", mapCount(row));
    }

    @Override
    public Map<String, Object> getWeeklySpent(LocalDate date) {
        LocalDate weekStart = date.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = date.with(java.time.DayOfWeek.SUNDAY);
        Map<String, Object> row = baseMapper.sumExpenseBetween(weekStart, weekEnd);
        return Map.of("weekStart", weekStart.toString(), "totalSpent", mapTotal(row), "count", mapCount(row));
    }

    private static Object mapValue(Map<String, Object> row, String key) {
        if (row == null) return null;
        Object value = row.get(key);
        if (value != null) return value;
        return row.entrySet().stream()
                .filter(e -> key.equalsIgnoreCase(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    private static BigDecimal mapTotal(Map<String, Object> row) {
        Object value = mapValue(row, "totalSpent");
        if (value instanceof BigDecimal b) return b;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    private static int mapCount(Map<String, Object> row) {
        Object value = mapValue(row, "cnt");
        return value instanceof Number n ? n.intValue() : 0;
    }

    // ==================== Internal Methods ====================

    private String normalizeExternalId(String externalId) {
        if (externalId == null) {
            return null;
        }
        String trimmed = externalId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Transaction findByExternalId(String externalId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<Transaction>()
                .eq(Transaction::getExternalId, externalId)
                .last("LIMIT 1"));
    }

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
        entity.setReviewStatus(normalizeReviewStatus(dto.getReviewStatus()));

        if (dto.getTags() != null) {
            try { entity.setTags(objectMapper.writeValueAsString(dto.getTags())); } catch (Exception e) { log.warn("Failed to serialize tags", e); }
        }
        if (dto.getAttachmentUrls() != null) {
            try { entity.setAttachmentUrls(objectMapper.writeValueAsString(dto.getAttachmentUrls())); } catch (Exception e) { log.warn("Failed to serialize attachmentUrls", e); }
        }
        if (dto.getExtFields() != null) {
            try { entity.setExtFields(objectMapper.writeValueAsString(dto.getExtFields())); } catch (Exception e) { log.warn("Failed to serialize extFields", e); }
        }
        if (dto.getMetadata() != null) {
            try { entity.setMetadata(objectMapper.writeValueAsString(dto.getMetadata())); } catch (Exception e) { log.warn("Failed to serialize metadata", e); }
        }
        if (dto.getIsRecurring() != null) entity.setIsRecurring(dto.getIsRecurring());
        if (dto.getRecurringId() != null) entity.setRecurringId(dto.getRecurringId());

        // 计算本位币金额
        calculateBaseAmount(entity, account);

        return entity;
    }

    private String normalizeReviewStatus(String value) {
        if (value == null || value.isBlank()) return "REVIEWED";
        String normalized = value.trim().toUpperCase();
        return "PENDING".equals(normalized) ? "PENDING" : "REVIEWED";
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

    private void ensureEditable(Transaction entity) {
        if (entity.getType() != null && entity.getType() == 3
                && entity.getTargetAccountId() == null) {
            throw new BizException(ErrorCode.TRANSACTION_TARGET_ACCOUNT_REQUIRED);
        }
    }

    private void rollbackBalance(Transaction entity) {
        switch (entity.getType()) {
            case 1 -> accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount());       // 回滚支出
            case 2 -> accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount().negate()); // 回滚收入
            case 4 -> accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount().negate()); // 回滚退款
            case 3 -> {                                                                               // 回滚转账
                accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount());
                accountMapper.adjustBalance(entity.getTargetAccountId(), entity.getAmount().negate());
            }
        }
    }

    
    private Map<Long, Account> loadAccounts(List<Transaction> transactions) {
        // 同时加载转出账户和转入账户，避免 toVO 对每行转账再查一次目标账户。
        List<Long> ids = transactions.stream()
                .flatMap(t -> java.util.stream.Stream.of(t.getAccountId(), t.getTargetAccountId()))
                .filter(Objects::nonNull)
                .distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return accountMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));
    }

    private Map<Long, Category> loadCategories(List<Transaction> transactions) {
        List<Long> ids = transactions.stream()
                .map(Transaction::getCategoryId)
                .filter(id -> id != null)
                .distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return categoryMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
    }

    private Map<Long, Category> loadParentCategories(java.util.Collection<Category> categories) {
        List<Long> ids = categories.stream()
                .map(Category::getParentId)
                .filter(id -> id != null && id != 0)
                .distinct().toList();
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

    /**
     * 注册事务提交后缓存清除回调，确保提交后再清除缓存
     */
    private void registerCacheClearSync(LocalDate date) {
        String month = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        Long userId = getCurrentUserId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                clearReportCache(month, userId);
            }
        });
    }

    private void clearReportCache(String month, Long userId) {
        try {
            if (userId != null) {
                scanAndDelete("report:monthly-summary:" + userId + ":" + month + "*");
                // 往来款汇总依赖交易流水，交易变更后立即失效
                scanAndDelete("receivable:overview:" + userId + ":*");
            }
            scanAndDelete("report:category-detail:" + month + ":*");
            redisTemplate.delete("report:account-summary");
            log.info("Cleared report cache for month {}", month);
        } catch (Exception e) {
            log.warn("Failed to clear report cache for month {}", month, e);
        }
    }

    private void scanAndDelete(String pattern) {
        try {
            var cursor = redisTemplate.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100)
                    .build());
            while (cursor.hasNext()) {
                redisTemplate.delete(cursor.next());
            }
            cursor.close();
        } catch (Exception e) {
            log.warn("Failed to scan and delete keys with pattern {}", pattern, e);
        }
    }

    private Long getCurrentUserId() {
        try {
            return cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    private com.baomidou.mybatisplus.core.toolkit.support.SFunction<Transaction, ?> mapSortField(String field) {
        return switch (field) {
            case "amount" -> Transaction::getAmountBase;
            case "created_at" -> Transaction::getCreatedAt;
            default -> Transaction::getTransactionDate;
        };
    }

    private TransactionVO toVO(Transaction entity, Account account, Category category, Merchant merchant) {
        Category parent = null;
        if (category != null && category.getParentId() != null && category.getParentId() != 0) {
            parent = categoryMapper.selectById(category.getParentId());
        }
        return toVO(entity, account, category, merchant, parent);
    }

    private TransactionVO toVO(Transaction entity, Account account, Category category,
                               Merchant merchant, Category parentCategory) {
        return toVO(entity, account, category, merchant, parentCategory, null);
    }

    private TransactionVO toVO(Transaction entity, Account account, Category category,
                               Merchant merchant, Category parentCategory,
                               Map<Long, Account> accountLookup) {
        TransactionVO vo = new TransactionVO();
        vo.setId(entity.getId());
        vo.setExternalId(entity.getExternalId());
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
        vo.setReviewStatus(entity.getReviewStatus());
        vo.setIsRecurring(entity.getIsRecurring());
        vo.setRecurringId(entity.getRecurringId());
        vo.setReconciled(entity.getReconciled());
        vo.setReconcileChannel(entity.getReconcileChannel());
        vo.setReconcileBatchId(entity.getReconcileBatchId());
        vo.setReconciledAt(entity.getReconciledAt());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());

        if (account != null) {
            vo.setAccountName(account.getName());
        }
        if (entity.getTargetAccountId() != null) {
            Account target = accountLookup != null
                    ? accountLookup.get(entity.getTargetAccountId())
                    : accountMapper.selectById(entity.getTargetAccountId());
            if (target != null) vo.setTargetAccountName(target.getName());
        }
        if (category != null) {
            vo.setCategoryName(category.getName());
            vo.setCategoryIcon(category.getIcon());
            vo.setCategoryColor(category.getColor());
            if (parentCategory != null) {
                vo.setParentCategoryId(parentCategory.getId());
                vo.setParentCategoryName(parentCategory.getName());
                vo.setParentCategoryColor(parentCategory.getColor());
            }
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
            try { vo.setTags(objectMapper.readValue(entity.getTags(), new TypeReference<>() {})); } catch (Exception e) { log.warn("Failed to deserialize tags", e); }
        }
        if (entity.getAttachmentUrls() != null) {
            try { vo.setAttachmentUrls(objectMapper.readValue(entity.getAttachmentUrls(), new TypeReference<>() {})); } catch (Exception e) { log.warn("Failed to deserialize attachmentUrls", e); }
        }
        if (entity.getExtFields() != null) {
            try { vo.setExtFields(objectMapper.readValue(entity.getExtFields(), new TypeReference<>() {})); } catch (Exception e) { log.warn("Failed to deserialize extFields", e); }
        }
        if (entity.getMetadata() != null) {
            try { vo.setMetadata(objectMapper.readValue(entity.getMetadata(), new TypeReference<>() {})); } catch (Exception e) { log.warn("Failed to deserialize metadata", e); }
        }

        return vo;
    }
}
