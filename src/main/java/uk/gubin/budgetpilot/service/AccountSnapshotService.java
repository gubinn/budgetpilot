package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gubin.budgetpilot.common.BalanceCalculator;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.common.TenantContext;
import uk.gubin.budgetpilot.entity.Account;
import uk.gubin.budgetpilot.entity.AccountBalanceSnapshot;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.AccountBalanceSnapshotMapper;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.vo.NetWorthSnapshotVO;
import uk.gubin.budgetpilot.vo.ReportOverviewVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 账户月末余额快照。
 *
 * <p>余额快照不依赖“导入是否及时”：用当前已校准余额反推月末余额
 * = 当前余额 − 月末之后的流水影响。定时任务提前快照，报表读取；
 * 缺失月份在查询时补齐，避免第一次打开报表没有数据。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountSnapshotService {

    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;
    private final AccountBalanceSnapshotMapper snapshotMapper;

    /**
     * 为指定月份写入/更新月末余额快照。
     */
    @Transactional
    public void snapshotMonths(Long userId, List<YearMonth> months) {
        if (userId == null || months == null || months.isEmpty()) {
            return;
        }
        YearMonth nowMonth = YearMonth.now();
        List<YearMonth> valid = months.stream()
                .filter(m -> !m.isAfter(nowMonth))
                .toList();
        if (valid.isEmpty()) {
            return;
        }
        months = valid;
        Long previous = TenantContext.get();
        TenantContext.set(userId);
        try {
            List<Account> accounts = accountMapper.selectList(
                    new LambdaQueryWrapper<Account>().eq(Account::getIsActive, true));
            if (accounts.isEmpty()) {
                return;
            }
            YearMonth oldest = months.stream().min(Comparator.naturalOrder()).orElseThrow();
            LocalDate after = oldest.atEndOfMonth().plusDays(1);
            List<Transaction> txs = transactionMapper.selectList(
                    new LambdaQueryWrapper<Transaction>()
                            .eq(Transaction::getIsConfirmed, true)
                            .ge(Transaction::getTransactionDate, after));

            for (Account account : accounts) {
                BigDecimal current = account.getCurrentBalance() == null
                        ? BigDecimal.ZERO : account.getCurrentBalance();
                List<Transaction> related = txs.stream()
                        .filter(t -> account.getId().equals(t.getAccountId())
                                || (t.getType() != null && t.getType() == 3
                                    && account.getId().equals(t.getTargetAccountId())))
                        .toList();
                for (YearMonth ym : months) {
                    LocalDate monthEnd = ym.atEndOfMonth();
                    List<Transaction> afterMonth = related.stream()
                            .filter(t -> t.getTransactionDate() != null
                                    && t.getTransactionDate().isAfter(monthEnd))
                            .toList();
                    BigDecimal effect = BalanceCalculator.balanceAt(
                            BigDecimal.ZERO, account.getId(), afterMonth);
                    upsert(userId, account.getId(), ym.toString(), current.subtract(effect));
                }
            }
        } catch (Exception e) {
            log.warn("Snapshot account balances failed for user {}: {}", userId, e.getMessage());
        } finally {
            if (previous == null) {
                TenantContext.clear();
            } else {
                TenantContext.set(previous);
            }
        }
    }

    /**
     * 最近 N 个月的净资产趋势。当前月用实时余额，历史月用快照；
     * 缺失的历史快照会先补齐再返回。
     */
    public List<ReportOverviewVO.AccountBlock.NetWorthItem> netWorthTrend(
            Long userId, YearMonth endMonth, int months) {
        List<ReportOverviewVO.AccountBlock.NetWorthItem> result = new ArrayList<>();
        if (userId == null) {
            return result;
        }
        Long previous = TenantContext.get();
        TenantContext.set(userId);
        try {
            YearMonth currentMonth = YearMonth.now();
            // 未来月份没有月末余额，趋势最多画到当前月
            YearMonth effectiveEnd = endMonth.isAfter(currentMonth) ? currentMonth : endMonth;
            YearMonth start = effectiveEnd.minusMonths(months - 1L);
            List<YearMonth> targets = new ArrayList<>();
            for (YearMonth ym = start; !ym.isAfter(effectiveEnd); ym = ym.plusMonths(1)) {
                targets.add(ym);
            }
            List<YearMonth> history = targets.stream()
                    .filter(ym -> !ym.equals(currentMonth))
                    .toList();

            if (!history.isEmpty()) {
                List<String> keys = history.stream().map(YearMonth::toString).toList();
                Set<String> existing = snapshotMapper.selectList(
                                new LambdaQueryWrapper<AccountBalanceSnapshot>()
                                        .eq(AccountBalanceSnapshot::getUserId, userId)
                                        .in(AccountBalanceSnapshot::getSnapshotMonth, keys))
                        .stream()
                        .map(AccountBalanceSnapshot::getSnapshotMonth)
                        .collect(Collectors.toSet());
                List<YearMonth> missing = history.stream()
                        .filter(ym -> !existing.contains(ym.toString()))
                        .toList();
                if (!missing.isEmpty()) {
                    snapshotMonths(userId, missing);
                }
            }

            Map<String, BigDecimal> sums = new HashMap<>();
            if (!history.isEmpty()) {
                List<String> keys = history.stream().map(YearMonth::toString).toList();
                for (AccountBalanceSnapshot snap : snapshotMapper.selectList(
                        new LambdaQueryWrapper<AccountBalanceSnapshot>()
                                .eq(AccountBalanceSnapshot::getUserId, userId)
                                .in(AccountBalanceSnapshot::getSnapshotMonth, keys))) {
                    sums.merge(snap.getSnapshotMonth(), snap.getBalance(), BigDecimal::add);
                }
            }

            if (targets.contains(currentMonth)) {
                BigDecimal current = accountMapper.selectList(
                                new LambdaQueryWrapper<Account>().eq(Account::getIsActive, true))
                        .stream()
                        .map(a -> a.getCurrentBalance() == null ? BigDecimal.ZERO : a.getCurrentBalance())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                sums.put(currentMonth.toString(), current);
            }

            for (YearMonth ym : targets) {
                ReportOverviewVO.AccountBlock.NetWorthItem item =
                        new ReportOverviewVO.AccountBlock.NetWorthItem();
                item.setMonth(ym.toString());
                item.setNetWorth(sums.getOrDefault(ym.toString(), BigDecimal.ZERO));
                result.add(item);
            }
            return result;
        } finally {
            if (previous == null) {
                TenantContext.clear();
            } else {
                TenantContext.set(previous);
            }
        }
    }

    /**
     * 某个月的净资产明细：每个账户的月末余额 + 相对上个月的变化。
     * 当前月用实时余额，历史月用快照；缺失快照会即时补算。
     */
    public NetWorthSnapshotVO snapshotDetail(Long userId, YearMonth month) {
        YearMonth current = YearMonth.now();
        if (month.isAfter(current)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不能查询未来月份的余额快照");
        }
        Long previous = TenantContext.get();
        TenantContext.set(userId);
        try {
            YearMonth prevMonth = month.minusMonths(1);
            List<YearMonth> ensure = new ArrayList<>();
            if (!month.equals(current)) {
                ensure.add(month);
            }
            if (!prevMonth.equals(current)) {
                ensure.add(prevMonth);
            }
            if (!ensure.isEmpty()) {
                snapshotMonths(userId, ensure);
            }

            Map<Long, BigDecimal> balances = month.equals(current)
                    ? liveBalances() : snapshotBalances(month);
            Map<Long, BigDecimal> prevBalances = prevMonth.equals(current)
                    ? liveBalances() : snapshotBalances(prevMonth);

            Set<Long> ids = new HashSet<>(balances.keySet());
            ids.addAll(prevBalances.keySet());
            boolean includeZero = month.equals(current);
            if (includeZero) {
                activeAccounts().forEach(a -> ids.add(a.getId()));
            }
            Map<Long, Account> accounts = ids.isEmpty() ? Map.of()
                    : accountMapper.selectBatchIds(ids).stream()
                    .collect(Collectors.toMap(Account::getId, a -> a, (a, b) -> a));

            BigDecimal net = balances.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal prevNet = prevBalances.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<NetWorthSnapshotVO.Row> rows = new ArrayList<>();
            for (Long id : ids) {
                BigDecimal balance = balances.getOrDefault(id, BigDecimal.ZERO);
                BigDecimal prevBalance = prevBalances.getOrDefault(id, BigDecimal.ZERO);
                if (!includeZero && balance.signum() == 0 && prevBalance.signum() == 0) {
                    continue;
                }
                Account account = accounts.get(id);
                NetWorthSnapshotVO.Row row = new NetWorthSnapshotVO.Row();
                row.setAccountId(id);
                row.setName(account != null ? account.getName() : "已删除账户 #" + id);
                row.setType(account != null ? accountTypeLabel(account.getType()) : "未知");
                row.setCurrency(account != null && account.getCurrency() != null
                        ? account.getCurrency() : "CNY");
                row.setBalance(scale2(balance));
                row.setPrevBalance(scale2(prevBalance));
                row.setChange(scale2(balance.subtract(prevBalance)));
                rows.add(row);
            }
            rows.sort(Comparator.comparing((NetWorthSnapshotVO.Row r) -> r.getBalance().abs())
                    .reversed().thenComparing(NetWorthSnapshotVO.Row::getName));

            NetWorthSnapshotVO vo = new NetWorthSnapshotVO();
            vo.setMonth(month.toString());
            vo.setLabel(monthLabel(month));
            vo.setCurrentMonth(month.equals(current));
            vo.setNetWorth(scale2(net));
            vo.setPrevNetWorth(scale2(prevNet));
            vo.setChange(scale2(net.subtract(prevNet)));
            vo.setAccounts(rows);
            vo.setMonths(buildMonthOptions(userId, current));
            return vo;
        } finally {
            if (previous == null) {
                TenantContext.clear();
            } else {
                TenantContext.set(previous);
            }
        }
    }

    private List<NetWorthSnapshotVO.MonthOption> buildMonthOptions(Long userId, YearMonth current) {
        List<NetWorthSnapshotVO.MonthOption> options = new ArrayList<>();
        for (ReportOverviewVO.AccountBlock.NetWorthItem item
                : netWorthTrend(userId, current, 12)) {
            YearMonth ym = YearMonth.parse(item.getMonth());
            NetWorthSnapshotVO.MonthOption option = new NetWorthSnapshotVO.MonthOption();
            option.setMonth(item.getMonth());
            option.setLabel(monthLabel(ym) + (ym.equals(current) ? "（实时）" : ""));
            option.setNetWorth(scale2(item.getNetWorth()));
            option.setCurrentMonth(ym.equals(current));
            options.add(0, option);
        }
        return options;
    }

    private Map<Long, BigDecimal> liveBalances() {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Account account : activeAccounts()) {
            map.put(account.getId(), account.getCurrentBalance() == null
                    ? BigDecimal.ZERO : account.getCurrentBalance());
        }
        return map;
    }

    private List<Account> activeAccounts() {
        return accountMapper.selectList(
                new LambdaQueryWrapper<Account>().eq(Account::getIsActive, true));
    }

    private Map<Long, BigDecimal> snapshotBalances(YearMonth month) {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (AccountBalanceSnapshot snap : snapshotMapper.selectList(
                new LambdaQueryWrapper<AccountBalanceSnapshot>()
                        .eq(AccountBalanceSnapshot::getSnapshotMonth, month.toString()))) {
            map.merge(snap.getAccountId(),
                    snap.getBalance() == null ? BigDecimal.ZERO : snap.getBalance(),
                    BigDecimal::add);
        }
        return map;
    }

    private String monthLabel(YearMonth ym) {
        return ym.getYear() + "年" + ym.getMonthValue() + "月";
    }

    private String accountTypeLabel(Integer type) {
        if (type == null) {
            return "未知";
        }
        return switch (type) {
            case 1 -> "现金";
            case 2 -> "储蓄卡";
            case 3 -> "信用卡";
            case 4 -> "电子钱包";
            case 5 -> "投资账户";
            default -> "未知";
        };
    }

    private BigDecimal scale2(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private void upsert(Long userId, Long accountId, String month, BigDecimal balance) {
        AccountBalanceSnapshot existing = snapshotMapper.selectOne(
                new LambdaQueryWrapper<AccountBalanceSnapshot>()
                        .eq(AccountBalanceSnapshot::getUserId, userId)
                        .eq(AccountBalanceSnapshot::getAccountId, accountId)
                        .eq(AccountBalanceSnapshot::getSnapshotMonth, month));
        if (existing == null) {
            AccountBalanceSnapshot snap = new AccountBalanceSnapshot();
            snap.setUserId(userId);
            snap.setAccountId(accountId);
            snap.setSnapshotMonth(month);
            snap.setBalance(balance);
            snapshotMapper.insert(snap);
        } else if (existing.getBalance() == null || existing.getBalance().compareTo(balance) != 0) {
            existing.setBalance(balance);
            snapshotMapper.updateById(existing);
        }
    }
}