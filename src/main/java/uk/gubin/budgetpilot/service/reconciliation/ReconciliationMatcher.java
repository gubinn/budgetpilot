package uk.gubin.budgetpilot.service.reconciliation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gubin.budgetpilot.entity.ReconciliationItem;
import uk.gubin.budgetpilot.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 账单明细与交易流水的自动匹配。
 *
 * 规则：
 * 1. 金额精确到分、类型与收支方向一致、时间差在窗口内；
 * 2. 按时间差全局贪心分配，一笔流水只能匹配一条账单；
 * 3. 同金额同时间存在多个候选时标记 AMBIGUOUS，交给人工；
 * 4. 二分/三分金额之和等于一笔流水时做聚合匹配（支付宝拆单支付）。
 */
@Component
@RequiredArgsConstructor
public class ReconciliationMatcher {

    /** 精确匹配时间窗口：10 分钟 */
    static final long MATCH_WINDOW_SECONDS = 600;
    /** 聚合匹配时间窗口：2 分钟 */
    static final long AGGREGATE_WINDOW_SECONDS = 180;
    /**
 * 多个候选的时间差几乎相同（2 秒内）才视为歧义。
 * 银行与账单的落账时间通常只差 1 秒；差 10 秒以上基本可以按最近时间唯一确定。
 */
    static final long AMBIGUOUS_GAP_SECONDS = 2;

    private final ObjectMapper objectMapper;

    public record ItemMatch(Long itemId, String status, String matchType, Long transactionId) {
    }

    private record Candidate(long diffSeconds, Transaction transaction) {
    }

    private record Pair(long diffSeconds, Long itemId, Long transactionId) {
    }

    public List<ItemMatch> match(List<ReconciliationItem> items,
                                 List<Transaction> transactions,
                                 BillChannel channel) {
        Map<Long, Transaction> txById = new LinkedHashMap<>();
        for (Transaction tx : transactions) {
            // 已被其它批次对过账的流水不再自动匹配，避免重复挂账
            if (Boolean.TRUE.equals(tx.getReconciled())) {
                continue;
            }
            if (channel.matches(extChannel(tx), tx.getNote())) {
                txById.put(tx.getId(), tx);
            }
        }

        Set<Long> usedTx = new HashSet<>();
        Map<Long, List<Candidate>> candidatesByItem = new HashMap<>();
        Set<Long> ambiguous = new HashSet<>();
        List<Pair> pairs = new ArrayList<>();
        List<ItemMatch> result = new ArrayList<>();

        for (ReconciliationItem item : items) {
            if (ReconcileConstants.STATUS_IGNORED.equals(item.getMatchStatus())) {
                result.add(new ItemMatch(item.getId(), ReconcileConstants.STATUS_IGNORED, null, null));
                continue;
            }
            // 手工匹配/补录的结果在重新匹配时保留
            if (ReconcileConstants.STATUS_MATCHED.equals(item.getMatchStatus())
                    && (ReconcileConstants.TYPE_MANUAL.equals(item.getMatchType())
                    || ReconcileConstants.TYPE_CREATED.equals(item.getMatchType()))) {
                result.add(new ItemMatch(item.getId(), ReconcileConstants.STATUS_MATCHED,
                        item.getMatchType(), item.getMatchTransactionId()));
                if (item.getMatchTransactionId() != null) {
                    usedTx.add(item.getMatchTransactionId());
                }
                continue;
            }
            if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                result.add(new ItemMatch(item.getId(), ReconcileConstants.STATUS_IGNORED, null, null));
                continue;
            }
            Integer txType = txType(item);
            if (txType == null) {
                result.add(new ItemMatch(item.getId(), ReconcileConstants.STATUS_UNMATCHED, null, null));
                continue;
            }
            List<Candidate> candidates = collectCandidates(item, txType, txById.values());
            candidatesByItem.put(item.getId(), candidates);
            if (candidates.size() >= 2
                    && candidates.get(1).diffSeconds() - candidates.get(0).diffSeconds() <= AMBIGUOUS_GAP_SECONDS) {
                ambiguous.add(item.getId());
            } else if (!candidates.isEmpty()) {
                Candidate best = candidates.get(0);
                pairs.add(new Pair(best.diffSeconds(), item.getId(), best.transaction().getId()));
            }
        }

        // 时间差最小的先分配，避免按账单行顺序造成抢占
        pairs.sort(Comparator.comparingLong(Pair::diffSeconds));
        Map<Long, Pair> assigned = new LinkedHashMap<>();
        for (Pair pair : pairs) {
            if (assigned.containsKey(pair.itemId()) || usedTx.contains(pair.transactionId())) {
                continue;
            }
            assigned.put(pair.itemId(), pair);
            usedTx.add(pair.transactionId());
        }

        for (ReconciliationItem item : items) {
            if (result.stream().anyMatch(r -> r.itemId().equals(item.getId()))) {
                continue;
            }
            if (ambiguous.contains(item.getId())) {
                result.add(new ItemMatch(item.getId(), ReconcileConstants.STATUS_AMBIGUOUS, null, null));
                continue;
            }
            Pair pair = assigned.get(item.getId());
            if (pair != null) {
                result.add(new ItemMatch(item.getId(), ReconcileConstants.STATUS_MATCHED,
                        ReconcileConstants.TYPE_AUTO, pair.transactionId()));
                continue;
            }
            // 有候选但被其它账单行占用：同样交给人工确认
            List<Candidate> candidates = candidatesByItem.get(item.getId());
            if (candidates != null && !candidates.isEmpty()) {
                result.add(new ItemMatch(item.getId(), ReconcileConstants.STATUS_AMBIGUOUS, null, null));
            } else {
                result.add(new ItemMatch(item.getId(), ReconcileConstants.STATUS_UNMATCHED, null, null));
            }
        }

        // 聚合匹配：处理一笔流水对应多笔账单（拆单支付）
        aggregate(items, result, txById.values(), usedTx);
        return result;
    }

    private void aggregate(List<ReconciliationItem> items,
                           List<ItemMatch> result,
                           Iterable<Transaction> transactions,
                           Set<Long> usedTx) {
        Map<Long, ItemMatch> byItem = new HashMap<>();
        for (ItemMatch m : result) {
            byItem.put(m.itemId(), m);
        }
        List<ReconciliationItem> pool = items.stream()
                .filter(i -> {
                    ItemMatch m = byItem.get(i.getId());
                    return m != null && ReconcileConstants.STATUS_UNMATCHED.equals(m.status())
                            && i.getAmount() != null && i.getAmount().compareTo(BigDecimal.ZERO) > 0
                            && txType(i) != null;
                })
                .sorted(Comparator.comparing(i -> itemTime(i)))
                .toList();

        for (Transaction tx : transactions) {
            if (usedTx.contains(tx.getId())) {
                continue;
            }
            BigDecimal target = txAmount(tx);
            if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            LocalDateTime txTime = txDateTime(tx);
            List<ReconciliationItem> sameDay = pool.stream()
                    .filter(i -> byItem.get(i.getId()) != null)
                    .filter(i -> ReconcileConstants.STATUS_UNMATCHED.equals(byItem.get(i.getId()).status()))
                    .filter(i -> Objects.equals(i.getTradeDate(), tx.getTransactionDate()))
                    .filter(i -> Objects.equals(txType(i), tx.getType()))
                    .filter(i -> Math.abs(ChronoUnit.SECONDS.between(itemTime(i), txTime)) <= AGGREGATE_WINDOW_SECONDS)
                    .toList();

            List<ReconciliationItem> combo = findCombo(sameDay, target);
            if (combo.isEmpty()) {
                continue;
            }
            usedTx.add(tx.getId());
            for (ReconciliationItem item : combo) {
                byItem.put(item.getId(), new ItemMatch(item.getId(), ReconcileConstants.STATUS_MATCHED,
                        ReconcileConstants.TYPE_AGGREGATE, tx.getId()));
            }
        }

        for (int i = 0; i < result.size(); i++) {
            ItemMatch updated = byItem.get(result.get(i).itemId());
            if (updated != null) {
                result.set(i, updated);
            }
        }
    }

    /** 在时间窗口内找 2~3 条账单金额之和等于流水金额 */
    private List<ReconciliationItem> findCombo(List<ReconciliationItem> pool, BigDecimal target) {
        for (int i = 0; i < pool.size(); i++) {
            for (int j = i + 1; j < pool.size(); j++) {
                if (!sameCounterparty(pool.get(i), pool.get(j))) {
                    continue;
                }
                BigDecimal sum = pool.get(i).getAmount().add(pool.get(j).getAmount());
                if (sum.compareTo(target) == 0) {
                    return List.of(pool.get(i), pool.get(j));
                }
                for (int k = j + 1; k < pool.size(); k++) {
                    if (!sameCounterparty(pool.get(i), pool.get(k))) {
                        continue;
                    }
                    if (sum.add(pool.get(k).getAmount()).compareTo(target) == 0) {
                        return List.of(pool.get(i), pool.get(j), pool.get(k));
                    }
                }
            }
        }
        return List.of();
    }

    private boolean sameCounterparty(ReconciliationItem a, ReconciliationItem b) {
        String left = a.getCounterparty() == null ? "" : a.getCounterparty().trim();
        String right = b.getCounterparty() == null ? "" : b.getCounterparty().trim();
        return left.isEmpty() || right.isEmpty() || left.equals(right);
    }

    private List<Candidate> collectCandidates(ReconciliationItem item, Integer txType,
                                              Iterable<Transaction> transactions) {
        LocalDateTime itemTime = itemTime(item);
        List<Candidate> candidates = new ArrayList<>();
        for (Transaction tx : transactions) {
            if (!Objects.equals(tx.getType(), txType)) {
                continue;
            }
            BigDecimal txAmount = txAmount(tx);
            if (txAmount == null || txAmount.compareTo(item.getAmount()) != 0) {
                continue;
            }
            LocalDateTime txTime = txDateTime(tx);
            // 允许跨日 1 天，但实际时间差仍要落在 10 分钟内
            if (Math.abs(ChronoUnit.DAYS.between(itemTime.toLocalDate(), txTime.toLocalDate())) > 1) {
                continue;
            }
            long diff = Math.abs(ChronoUnit.SECONDS.between(itemTime, txTime));
            if (diff <= MATCH_WINDOW_SECONDS) {
                candidates.add(new Candidate(diff, tx));
            }
        }
        candidates.sort(Comparator.comparingLong(Candidate::diffSeconds));
        return candidates;
    }

    private Integer txType(ReconciliationItem item) {
        return switch (item.getDirection()) {
            case ReconcileConstants.DIRECTION_EXPENSE -> 1;
            case ReconcileConstants.DIRECTION_INCOME -> 2;
            case ReconcileConstants.DIRECTION_NEUTRAL ->
                    item.getTradeStatus() != null && item.getTradeStatus().contains("退款") ? 4 : null;
            default -> null;
        };
    }

    private LocalDateTime itemTime(ReconciliationItem item) {
        if (item.getPaidTime() != null) {
            return item.getPaidTime();
        }
        if (item.getCreatedTime() != null) {
            return item.getCreatedTime();
        }
        LocalDate date = item.getTradeDate() != null ? item.getTradeDate() : LocalDate.now();
        return date.atTime(LocalTime.NOON);
    }

    private LocalDateTime txDateTime(Transaction tx) {
        LocalDate date = tx.getTransactionDate() != null ? tx.getTransactionDate() : LocalDate.now();
        return tx.getTransactionTime() != null ? date.atTime(tx.getTransactionTime()) : date.atTime(LocalTime.NOON);
    }

    private BigDecimal txAmount(Transaction tx) {
        return tx.getAmountBase() != null ? tx.getAmountBase() : tx.getAmount();
    }

    private String extChannel(Transaction tx) {
        if (tx.getExtFields() == null || tx.getExtFields().isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(tx.getExtFields());
            JsonNode channel = node.get("channel");
            return channel == null || channel.isNull() ? null : channel.asText();
        } catch (Exception e) {
            return null;
        }
    }
}