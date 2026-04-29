package uk.gubin.budgetpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.dto.AccountCreateDTO;
import uk.gubin.budgetpilot.dto.AccountUpdateDTO;
import uk.gubin.budgetpilot.entity.Account;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.service.AccountService;
import uk.gubin.budgetpilot.service.CurrencyRateService;
import uk.gubin.budgetpilot.vo.AccountVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {

    private final ObjectMapper objectMapper;
    private final TransactionMapper transactionMapper;
    private final CategoryMapper categoryMapper;
    private final CurrencyRateService currencyRateService;
    private final StringRedisTemplate redisTemplate;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 清除当前用户的报表缓存（账户变更只影响当前用户）
     */
    private void clearAllReportCache() {
        Long userId = getUserId();
        try {
            if (userId != null) {
                scanAndDelete("report:monthly-summary:" + userId + ":*");
                scanAndDelete("report:category-detail:" + userId + ":*");
            }
            redisTemplate.delete("report:account-summary");
            log.info("Cleared report cache for user {}", userId);
        } catch (Exception e) {
            log.warn("Failed to clear report cache", e);
        }
    }

    /**
     * 使用 SCAN 命令安全删除匹配的 keys
     */
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

    @Override
    @Transactional
    public AccountVO create(AccountCreateDTO dto) {
        // 检查名称唯一性
        if (existsByName(dto.getName(), null)) {
            throw new BizException(ErrorCode.ACCOUNT_NAME_EXISTS, dto.getName());
        }

        Account entity = new Account();
        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setIcon(dto.getIcon());
        entity.setCurrency(dto.getCurrency());
        entity.setInitialBalance(dto.getInitialBalance());
        entity.setCurrentBalance(dto.getInitialBalance());
        entity.setCreditLimit(dto.getCreditLimit());
        entity.setBillingDay(dto.getBillingDay());
        entity.setPaymentDay(dto.getPaymentDay());
        entity.setSortOrder(dto.getSortOrder());
        entity.setIsActive(true);
        if (dto.getExtFields() != null) {
            try {
                entity.setExtFields(objectMapper.writeValueAsString(dto.getExtFields()));
            } catch (Exception e) {
                log.error("Failed to serialize ext_fields", e);
            }
        }
        if (dto.getMetadata() != null) {
            try {
                entity.setMetadata(objectMapper.writeValueAsString(dto.getMetadata()));
            } catch (Exception e) {
                log.error("Failed to serialize metadata", e);
            }
        }

        save(entity);
        log.info("Created account: {}", entity.getName());
        return toVO(entity);
    }

    @Override
    public List<AccountVO> listActive(Boolean activeOnly) {
        LambdaQueryWrapper<Account> query = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(activeOnly)) {
            query.eq(Account::getIsActive, true);
        }
        query.orderByAsc(Account::getSortOrder);
        return list(query).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public AccountVO getById(Long id) {
        Account entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        return toVO(entity);
    }

    @Override
    @Transactional
    public AccountVO update(Long id, AccountUpdateDTO dto) {
        Account entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        // 检查名称唯一性（排除自身）
        if (dto.getName() != null && !dto.getName().equals(entity.getName()) && existsByName(dto.getName(), id)) {
            throw new BizException(ErrorCode.ACCOUNT_NAME_EXISTS, dto.getName());
        }

        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getIcon() != null) entity.setIcon(dto.getIcon());
        if (dto.getCurrency() != null) entity.setCurrency(dto.getCurrency());
        if (dto.getInitialBalance() != null) entity.setInitialBalance(dto.getInitialBalance());
        if (dto.getCurrentBalance() != null) entity.setCurrentBalance(dto.getCurrentBalance());
        if (dto.getCreditLimit() != null) entity.setCreditLimit(dto.getCreditLimit());
        if (dto.getBillingDay() != null) entity.setBillingDay(dto.getBillingDay());
        if (dto.getPaymentDay() != null) entity.setPaymentDay(dto.getPaymentDay());
        if (dto.getSortOrder() != null) entity.setSortOrder(dto.getSortOrder());
        if (dto.getIsActive() != null) entity.setIsActive(dto.getIsActive());
        if (dto.getExtFields() != null) {
            try {
                entity.setExtFields(objectMapper.writeValueAsString(dto.getExtFields()));
            } catch (Exception e) {
                log.error("Failed to serialize ext_fields", e);
            }
        }
        if (dto.getMetadata() != null) {
            try {
                entity.setMetadata(objectMapper.writeValueAsString(dto.getMetadata()));
            } catch (Exception e) {
                log.error("Failed to serialize metadata", e);
            }
        }

        baseMapper.updateById(entity);

        // 清除报表缓存（账户名称变更会影响报表显示）
        clearAllReportCache();

        return toVO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Account entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        // 检查是否有关联交易
        if (hasTransactions(id)) {
            throw new BizException(ErrorCode.ACCOUNT_HAS_TRANSACTIONS);
        }
        // 逻辑删除：停用
        entity.setIsActive(false);
        baseMapper.updateById(entity);

        // 清除报表缓存
        clearAllReportCache();

        log.info("Deactivated account: {}", entity.getName());
    }

    private boolean hasTransactions(Long accountId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Transaction> query =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        query.eq(Transaction::getAccountId, accountId)
                .or().eq(Transaction::getTargetAccountId, accountId);
        return transactionMapper.selectCount(query) > 0;
    }

    @Override
    @Transactional
    public void adjustBalance(Long accountId, BigDecimal amount) {
        int rows = baseMapper.adjustBalance(accountId, amount);
        if (rows == 0) {
            throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public void adjustBalanceWithRecord(Long accountId, BigDecimal newBalance, String reason) {
        Account account = baseMapper.selectById(accountId);
        if (account == null) {
            throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        BigDecimal currentBalance = account.getCurrentBalance();
        BigDecimal diff = newBalance.subtract(currentBalance);

        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            return; // 余额没变化，不处理
        }

        // 创建交易记录
        Transaction transaction = new Transaction();
        // 交易类型：type=1支出, type=2收入
        transaction.setType(diff.compareTo(BigDecimal.ZERO) > 0 ? 2 : 1);
        transaction.setAmount(diff.abs());
        transaction.setCurrency(account.getCurrency());
        transaction.setAmountBase(diff.abs()); // 基础货币金额
        transaction.setAccountId(accountId);
        // 分类：按名称和类型动态查找（58=余额调整收入, 59=余额调整支出）
        transaction.setCategoryId(getBalanceAdjustCategoryId(diff.compareTo(BigDecimal.ZERO) > 0 ? 2 : 1));
        transaction.setTransactionDate(java.time.LocalDate.now());
        transaction.setTransactionTime(java.time.LocalTime.now());
        transaction.setNote("余额调整: " + reason);
        transaction.setIsConfirmed(true);
        transaction.setIsRecurring(false);

        transactionMapper.insert(transaction);

        // 更新账户余额
        account.setCurrentBalance(newBalance);
        baseMapper.updateById(account);

        // 清除报表缓存
        clearReportCache(transaction.getTransactionDate());

        log.info("Adjusted account balance: {} -> {}, reason: {}", currentBalance, newBalance, reason);
    }

    @Override
    public BigDecimal getTotalAssets() {
        LambdaQueryWrapper<Account> query = new LambdaQueryWrapper<>();
        query.eq(Account::getIsActive, true);
        List<Account> accounts = list(query);

        BigDecimal total = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();
        for (Account account : accounts) {
            if (account.getCurrentBalance() != null) {
                if ("CNY".equals(account.getCurrency())) {
                    total = total.add(account.getCurrentBalance());
                } else {
                    BigDecimal rate = currencyRateService.getRate(account.getCurrency(), today);
                    if (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
                        total = total.add(account.getCurrentBalance().multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP));
                    }
                }
            }
        }
        return total;
    }

    /**
     * 清除交易日期所在月份的报表缓存
     */
    private void clearReportCache(LocalDate date) {
        String month = date.format(MONTH_FMT);
        Long userId = getUserId();
        try {
            if (userId != null) {
                redisTemplate.delete("report:monthly-summary:" + userId + ":" + month);
            }
            redisTemplate.delete("report:account-summary");
            log.info("Cleared report cache for month {}", month);
        } catch (Exception e) {
            log.warn("Failed to clear report cache for month {}", month, e);
        }
    }

    private Long getUserId() {
        try {
            return cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 动态查找“余额调整”分类 ID（按名称+类型查询 user_id=0 的系统分类）
     */
    private Long getBalanceAdjustCategoryId(int type) {
        LambdaQueryWrapper<uk.gubin.budgetpilot.entity.Category> query = new LambdaQueryWrapper<>();
        query.eq(uk.gubin.budgetpilot.entity.Category::getName, "余额调整")
                .eq(uk.gubin.budgetpilot.entity.Category::getType, type)
                .eq(uk.gubin.budgetpilot.entity.Category::getUserId, 0L)
                .last("LIMIT 1");
        uk.gubin.budgetpilot.entity.Category cat = categoryMapper.selectOne(query);
        if (cat == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "余额调整分类未找到，请先初始化系统分类");
        }
        return cat.getId();
    }

    private boolean existsByName(String name, Long excludeId) {
        LambdaQueryWrapper<Account> query = new LambdaQueryWrapper<>();
        query.eq(Account::getName, name);
        if (excludeId != null) {
            query.ne(Account::getId, excludeId);
        }
        return count(query) > 0;
    }

    private AccountVO toVO(Account entity) {
        AccountVO vo = new AccountVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setType(entity.getType());
        vo.setIcon(entity.getIcon());
        vo.setCurrency(entity.getCurrency());
        vo.setInitialBalance(entity.getInitialBalance());
        vo.setCurrentBalance(entity.getCurrentBalance());
        vo.setCreditLimit(entity.getCreditLimit());
        vo.setBillingDay(entity.getBillingDay());
        vo.setPaymentDay(entity.getPaymentDay());
        vo.setSortOrder(entity.getSortOrder());
        vo.setIsActive(entity.getIsActive());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        if (entity.getExtFields() != null) {
            try {
                vo.setExtFields(objectMapper.readValue(entity.getExtFields(), new TypeReference<>() {}));
            } catch (Exception e) {
                log.warn("Failed to deserialize ext_fields for account {}", entity.getId(), e);
            }
        }
        if (entity.getMetadata() != null) {
            try {
                vo.setMetadata(objectMapper.readValue(entity.getMetadata(), new TypeReference<>() {}));
            } catch (Exception e) {
                log.warn("Failed to deserialize metadata for account {}", entity.getId(), e);
            }
        }
        return vo;
    }
}
