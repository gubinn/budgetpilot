package uk.gubin.budgetpilot.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gubin.budgetpilot.common.TenantContext;
import uk.gubin.budgetpilot.entity.Account;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Merchant;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.MerchantMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.service.ReceivableService;
import uk.gubin.budgetpilot.vo.ReceivableGroupVO;
import uk.gubin.budgetpilot.vo.ReceivableOverviewVO;
import uk.gubin.budgetpilot.vo.ReceivableSummaryVO;
import uk.gubin.budgetpilot.vo.ReceivableTransactionVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 往来款聚合实现。
 *
 * <p>性能上只扫「往来款」父分类下的四类交易（走 category_id 索引），
 * 不会扫全量流水；结果再缓存 2 分钟，交易增删改提交后主动失效。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivableServiceImpl implements ReceivableService {

    static final String DIR_RECEIVABLE = "RECEIVABLE";
    static final String DIR_PAYABLE = "PAYABLE";
    static final String UNLABELED = "未标注对方";
    private static final String CACHE_PREFIX = "receivable:overview:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(2);
    private static final String LOAN_PARENT_NAME = "往来款";

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy.M.d"),
            DateTimeFormatter.ofPattern("yyyy年M月d日")
    };

    private static final Set<String> GENERIC_COUNTERPARTIES = Set.of(
            "转账", "网上银行", "手机银行", "银行", "云闪付", "银联", "银联代付",
            "支付宝", "微信", "微信支付", "财付通", "未知", "其他", "商户",
            "收款方", "付款方", "余额", "内部转账"
    );

    private static final Pattern NOTE_RECEIVER = Pattern.compile(
            "(?:收款人|收款方|对方户名|对方|付款人)[：:\\s]*([\\u4e00-\\u9fa5A-Za-z·]{2,20})");
    private static final Pattern NOTE_TRANSFER = Pattern.compile(
            "(?:转给|转账给|借给|借予)[：:\\s]*([\\u4e00-\\u9fa5A-Za-z·]{2,20})");
    private static final Pattern NOTE_REPAY = Pattern.compile(
            "([\\u4e00-\\u9fa5A-Za-z·]{2,20})(?:还款|归还|还了)");

    private final TransactionMapper transactionMapper;
    private final CategoryMapper categoryMapper;
    private final MerchantMapper merchantMapper;
    private final AccountMapper accountMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public ReceivableOverviewVO overview(boolean includeUnconfirmed) {
        return buildOverview(getUserId(), includeUnconfirmed);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceivableSummaryVO summary() {
        return overview(false).getSummary();
    }

    /**
     * 显式传入 userId，供定时任务和单元测试使用。
     * 同时设置 TenantContext，避免非请求线程查询到其他用户的数据。
     */
    ReceivableOverviewVO buildOverview(Long userId, boolean includeUnconfirmed) {
        ReceivableOverviewVO empty = emptyOverview();
        if (userId == null) {
            return empty;
        }

        String cacheKey = CACHE_PREFIX + userId + ":" + includeUnconfirmed;
        ReceivableOverviewVO cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        Long previous = TenantContext.get();
        try {
            TenantContext.set(userId);
            ReceivableOverviewVO result = compute(userId, includeUnconfirmed);
            writeCache(cacheKey, result);
            return result;
        } finally {
            if (previous != null) {
                TenantContext.set(previous);
            } else {
                TenantContext.clear();
            }
        }
    }

    private ReceivableOverviewVO compute(Long userId, boolean includeUnconfirmed) {
        ReceivableOverviewVO overview = emptyOverview();
        LocalDate today = LocalDate.now();
        overview.getSummary().setAsOf(today);

        Map<Long, Category> categoryById = loadLoanCategories();
        if (categoryById.isEmpty()) {
            return overview;
        }
        List<Long> categoryIds = new ArrayList<>(categoryById.keySet());

        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.in(Transaction::getCategoryId, categoryIds)
                .orderByAsc(Transaction::getTransactionDate)
                .orderByAsc(Transaction::getId);
        if (!includeUnconfirmed) {
            query.eq(Transaction::getIsConfirmed, true);
        }
        List<Transaction> transactions = transactionMapper.selectList(query);
        if (transactions.isEmpty()) {
            return overview;
        }

        Map<Long, String> merchantNames = loadMerchantNames(transactions);
        Map<Long, String> accountNames = loadAccountNames(transactions);

        Map<String, GroupBuilder> groups = new LinkedHashMap<>();
        for (Transaction tx : transactions) {
            Category category = categoryById.get(tx.getCategoryId());
            Effect effect = effectOf(category);
            if (effect == null) {
                continue;
            }

            Map<String, Object> ext = parseExt(tx.getExtFields());
            String counterparty = counterparty(tx, merchantNames, ext);
            String key = effect.direction + "\u0000" + counterparty;
            GroupBuilder group = groups.computeIfAbsent(key,
                    k -> new GroupBuilder(effect.direction, counterparty));

            ReceivableTransactionVO item = toItem(tx, effect, category, accountNames,
                    counterparty, expectedRepayDate(ext));
            group.add(effect, item, expectedRepayDate(ext));
        }

        List<ReceivableGroupVO> result = new ArrayList<>();
        for (GroupBuilder builder : groups.values()) {
            ReceivableGroupVO vo = builder.build(today);
            if (vo.getOutstanding().signum() > 0 || vo.getSettledTotal().signum() > 0) {
                result.add(vo);
            }
        }
        result.sort(groupComparator());
        overview.setGroups(result);

        ReceivableSummaryVO summary = overview.getSummary();
        BigDecimal receivable = ZERO;
        BigDecimal payable = ZERO;
        BigDecimal overdue = ZERO;
        Set<String> counterparties = new HashSet<>();
        int oldestDays = 0;
        int overdueCount = 0;
        int unlabeled = 0;

        for (ReceivableGroupVO group : result) {
            if (group.getOutstanding().signum() <= 0) {
                continue;
            }
            if (DIR_RECEIVABLE.equals(group.getDirection())) {
                receivable = receivable.add(group.getOutstanding());
            } else {
                payable = payable.add(group.getOutstanding());
            }
            counterparties.add(group.getCounterparty());
            oldestDays = Math.max(oldestDays, group.getAgeDays());
            if (group.getOverdueDays() > 0) {
                overdueCount++;
                overdue = overdue.add(group.getOutstanding());
            }
            if (UNLABELED.equals(group.getCounterparty())) {
                unlabeled += group.getTransactionCount();
            }
        }

        summary.setReceivableTotal(scale(receivable));
        summary.setPayableTotal(scale(payable));
        summary.setNet(scale(receivable.subtract(payable)));
        summary.setCounterpartyCount(counterparties.size());
        summary.setOldestDays(oldestDays);
        summary.setOverdueCount(overdueCount);
        summary.setOverdueTotal(scale(overdue));
        summary.setUnlabeledCount(unlabeled);
        return overview;
    }

    /**
     * 只查「往来款」父分类下的四个子分类；如果用户分类没有复制，回退到系统分类。
     */
    private Map<Long, Category> loadLoanCategories() {
        Map<Long, Category> all = new LinkedHashMap<>();
        List<Category> userCategories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().eq(Category::getIsActive, true));
        for (Category category : userCategories) {
            all.put(category.getId(), category);
        }

        boolean hasParent = all.values().stream()
                .anyMatch(c -> LOAN_PARENT_NAME.equals(c.getName())
                        && (c.getParentId() == null || c.getParentId() == 0L));
        if (!hasParent) {
            for (Category category : categoryMapper.selectSystemDefaults()) {
                all.putIfAbsent(category.getId(), category);
            }
        }

        Category parent = all.values().stream()
                .filter(c -> LOAN_PARENT_NAME.equals(c.getName())
                        && (c.getParentId() == null || c.getParentId() == 0L))
                .findFirst()
                .orElse(null);

        Map<Long, Category> result = new LinkedHashMap<>();
        if (parent != null) {
            for (Category category : all.values()) {
                if (parent.getId().equals(category.getParentId())
                        && effectOf(category) != null) {
                    result.put(category.getId(), category);
                }
            }
        } else {
            // 分类被改名/移动时，至少按四个已知名称兜底
            for (Category category : all.values()) {
                if (effectOf(category) != null) {
                    result.putIfAbsent(category.getId(), category);
                }
            }
        }
        if (result.isEmpty()) {
            for (Category category : categoryMapper.selectSystemDefaults()) {
                if (effectOf(category) != null) {
                    result.putIfAbsent(category.getId(), category);
                }
            }
        }
        return result;
    }

    private Effect effectOf(Category category) {
        if (category == null || category.getName() == null) {
            return null;
        }
        String name = category.getName().trim();
        if (name.contains("收回") && name.contains("借出")) {
            return Effect.COLLECT;
        }
        if (name.contains("归还") && name.contains("借入")) {
            return Effect.REPAY;
        }
        if (name.contains("借出")) {
            return Effect.LEND;
        }
        if (name.contains("借入")) {
            return Effect.BORROW;
        }
        return null;
    }

    private Map<Long, String> loadMerchantNames(List<Transaction> transactions) {
        Set<Long> ids = transactions.stream().map(Transaction::getMerchantId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return merchantMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Merchant::getId, Merchant::getName, (a, b) -> a));
    }

    private Map<Long, String> loadAccountNames(List<Transaction> transactions) {
        Set<Long> ids = transactions.stream().map(Transaction::getAccountId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return accountMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Account::getId, Account::getName, (a, b) -> a));
    }

    private ReceivableTransactionVO toItem(Transaction tx, Effect effect, Category category,
                                           Map<Long, String> accountNames, String counterparty,
                                           LocalDate expectedRepayDate) {
        ReceivableTransactionVO item = new ReceivableTransactionVO();
        item.setId(tx.getId());
        item.setDirection(effect.direction);
        item.setEffect(effect.name());
        item.setType(tx.getType());
        item.setAmount(scale(tx.getAmountBase() != null ? tx.getAmountBase() : tx.getAmount()));
        item.setCurrency(tx.getCurrency());
        item.setDate(tx.getTransactionDate());
        item.setCategoryName(category != null ? category.getName() : null);
        item.setAccountName(accountNames.get(tx.getAccountId()));
        item.setCounterparty(counterparty);
        item.setNote(tx.getNote());
        item.setExpectedRepayDate(expectedRepayDate);
        return item;
    }

    private String counterparty(Transaction tx, Map<Long, String> merchantNames,
                                Map<String, Object> ext) {
        String merchant = tx.getMerchantId() == null ? null : merchantNames.get(tx.getMerchantId());
        String normalized = normalizeCounterparty(merchant);
        if (normalized != null) {
            return normalized;
        }

        if (ext != null) {
            for (Map.Entry<String, Object> entry : ext.entrySet()) {
                String key = entry.getKey();
                if (key == null) {
                    continue;
                }
                if (key.contains("对手") || key.contains("对方") || key.contains("收款人")
                        || key.contains("付款人") || key.contains("户名")) {
                    normalized = normalizeCounterparty(stringValue(entry.getValue()));
                    if (normalized != null) {
                        return normalized;
                    }
                }
            }
        }

        normalized = counterpartyFromNote(tx.getNote());
        return normalized != null ? normalized : UNLABELED;
    }

    private String counterpartyFromNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        for (Pattern pattern : new Pattern[]{NOTE_RECEIVER, NOTE_TRANSFER, NOTE_REPAY}) {
            Matcher matcher = pattern.matcher(note);
            if (matcher.find()) {
                String value = normalizeCounterparty(matcher.group(1));
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private String normalizeCounterparty(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replaceAll("\\s+", "");
        value = value.replaceAll("^[：:，,。.\\-]+|[：:，,。.\\-]+$", "");
        if (value.startsWith("转账-") || value.startsWith("转账_")) {
            value = value.substring(3);
        } else if (value.startsWith("转账") && value.length() > 4) {
            value = value.substring(2);
        }
        if (value.length() < 2 || value.length() > 50) {
            return null;
        }
        if (value.matches("\\d+")) {
            return null;
        }
        if (GENERIC_COUNTERPARTIES.contains(value)) {
            return null;
        }
        // “招商银行”“中国银行储蓄卡”这类账户名不是借款人
        if (value.contains("银行") && value.length() <= 8) {
            return null;
        }
        return value;
    }

    private Map<String, Object> parseExt(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.debug("Failed to parse transaction ext_fields", e);
            return Map.of();
        }
    }

    private LocalDate expectedRepayDate(Map<String, Object> ext) {
        if (ext == null || ext.isEmpty()) {
            return null;
        }
        for (String key : new String[]{"expectedRepayDate", "约定还款日", "预计还款日", "还款日"}) {
            Object value = ext.get(key);
            if (value != null) {
                LocalDate date = parseDate(stringValue(value));
                if (date != null) {
                    return date;
                }
            }
        }
        return null;
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        try {
            return LocalDate.parse(value);
        } catch (Exception ignored) {
            // try other formats below
        }
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (Exception ignored) {
                // continue
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private ReceivableOverviewVO emptyOverview() {
        ReceivableOverviewVO overview = new ReceivableOverviewVO();
        overview.getSummary().setAsOf(LocalDate.now());
        return overview;
    }

    private Comparator<ReceivableGroupVO> groupComparator() {
        return (a, b) -> {
            int aOpen = a.getOutstanding().signum() > 0 ? 0 : 1;
            int bOpen = b.getOutstanding().signum() > 0 ? 0 : 1;
            if (aOpen != bOpen) {
                return Integer.compare(aOpen, bOpen);
            }
            int cmp = b.getOutstanding().compareTo(a.getOutstanding());
            if (cmp != 0) {
                return cmp;
            }
            cmp = Integer.compare(b.getAgeDays(), a.getAgeDays());
            if (cmp != 0) {
                return cmp;
            }
            String left = a.getCounterparty();
            String right = b.getCounterparty();
            if (left == null) {
                return right == null ? 0 : 1;
            }
            return right == null ? -1 : left.compareTo(right);
        };
    }

    private Long getUserId() {
        Long tenant = TenantContext.get();
        if (tenant != null) {
            return tenant;
        }
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    private ReceivableOverviewVO readCache(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null && !json.isBlank()) {
                return objectMapper.readValue(json, ReceivableOverviewVO.class);
            }
        } catch (Exception e) {
            log.debug("Receivable cache read failed", e);
        }
        return null;
    }

    private void writeCache(String key, ReceivableOverviewVO overview) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(overview), CACHE_TTL);
        } catch (Exception e) {
            log.debug("Receivable cache write failed", e);
        }
    }

    private enum Effect {
        LEND(DIR_RECEIVABLE, true),
        COLLECT(DIR_RECEIVABLE, false),
        BORROW(DIR_PAYABLE, true),
        REPAY(DIR_PAYABLE, false);

        private final String direction;
        private final boolean addsPrincipal;

        Effect(String direction, boolean addsPrincipal) {
            this.direction = direction;
            this.addsPrincipal = addsPrincipal;
        }
    }

    private static final class Lot {
        private final LocalDate date;
        private BigDecimal amount;
        private final LocalDate expectedRepayDate;

        private Lot(LocalDate date, BigDecimal amount, LocalDate expectedRepayDate) {
            this.date = date;
            this.amount = amount;
            this.expectedRepayDate = expectedRepayDate;
        }
    }

    private static final class GroupBuilder {
        private final String direction;
        private final String counterparty;
        private final List<Lot> lots = new LinkedList<>();
        private final List<ReceivableTransactionVO> transactions = new ArrayList<>();
        private BigDecimal principalTotal = ZERO;
        private BigDecimal settledTotal = ZERO;
        private BigDecimal credit = ZERO;
        private LocalDate firstLoanDate;
        private LocalDate lastActivityDate;
        private int cursor;

        private GroupBuilder(String direction, String counterparty) {
            this.direction = direction;
            this.counterparty = counterparty;
        }

        private void add(Effect effect, ReceivableTransactionVO item, LocalDate expectedRepayDate) {
            transactions.add(item);
            LocalDate date = item.getDate();
            if (date != null && (lastActivityDate == null || date.isAfter(lastActivityDate))) {
                lastActivityDate = date;
            }

            BigDecimal amount = item.getAmount() == null ? ZERO : item.getAmount();
            if (effect.addsPrincipal) {
                principalTotal = principalTotal.add(amount);
                if (firstLoanDate == null) {
                    firstLoanDate = date;
                }
                BigDecimal remaining = amount;
                if (credit.signum() > 0) {
                    BigDecimal offset = credit.min(remaining);
                    credit = credit.subtract(offset);
                    remaining = remaining.subtract(offset);
                }
                if (remaining.signum() > 0) {
                    lots.add(new Lot(date, remaining, expectedRepayDate));
                }
            } else {
                settledTotal = settledTotal.add(amount);
                BigDecimal remaining = amount;
                while (remaining.signum() > 0 && cursor < lots.size()) {
                    Lot lot = lots.get(cursor);
                    BigDecimal take = lot.amount.min(remaining);
                    lot.amount = lot.amount.subtract(take);
                    remaining = remaining.subtract(take);
                    if (lot.amount.signum() == 0) {
                        cursor++;
                    }
                }
                if (remaining.signum() > 0) {
                    credit = credit.add(remaining);
                }
            }
        }

        private ReceivableGroupVO build(LocalDate today) {
            BigDecimal outstanding = ZERO;
            LocalDate firstOutstanding = null;
            LocalDate expected = null;
            for (int i = cursor; i < lots.size(); i++) {
                Lot lot = lots.get(i);
                outstanding = outstanding.add(lot.amount);
                if (firstOutstanding == null) {
                    firstOutstanding = lot.date;
                }
                if (lot.expectedRepayDate != null
                        && (expected == null || lot.expectedRepayDate.isBefore(expected))) {
                    expected = lot.expectedRepayDate;
                }
            }

            ReceivableGroupVO vo = new ReceivableGroupVO();
            vo.setDirection(direction);
            vo.setCounterparty(counterparty);
            vo.setPrincipalTotal(scale(principalTotal));
            vo.setSettledTotal(scale(settledTotal));
            vo.setOutstanding(scale(outstanding));
            vo.setOverpaid(scale(credit));
            vo.setFirstLoanDate(firstLoanDate);
            vo.setFirstOutstandingDate(firstOutstanding);
            vo.setLastActivityDate(lastActivityDate);
            vo.setExpectedRepayDate(expected);
            vo.setAgeDays(outstanding.signum() > 0 && firstOutstanding != null
                    ? (int) Math.max(0, ChronoUnit.DAYS.between(firstOutstanding, today))
                    : 0);
            vo.setOverdueDays(outstanding.signum() > 0 && expected != null && expected.isBefore(today)
                    ? (int) ChronoUnit.DAYS.between(expected, today)
                    : 0);

            transactions.sort(Comparator
                    .comparing(ReceivableTransactionVO::getDate,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(ReceivableTransactionVO::getId,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed());
            vo.setTransactions(new ArrayList<>(transactions));
            vo.setTransactionCount(transactions.size());
            return vo;
        }

        private BigDecimal scale(BigDecimal value) {
            return value == null ? ZERO.setScale(2, RoundingMode.HALF_UP)
                    : value.setScale(2, RoundingMode.HALF_UP);
        }
    }
}