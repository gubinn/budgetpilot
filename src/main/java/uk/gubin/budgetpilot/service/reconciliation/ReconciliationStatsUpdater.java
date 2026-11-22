package uk.gubin.budgetpilot.service.reconciliation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gubin.budgetpilot.entity.ReconciliationBatch;
import uk.gubin.budgetpilot.entity.ReconciliationItem;
import uk.gubin.budgetpilot.mapper.ReconciliationBatchMapper;
import uk.gubin.budgetpilot.mapper.ReconciliationItemMapper;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 批次统计与交易解绑的共享维护逻辑。
 * 单独抽出来供对账服务和交易服务复用，避免交易删除后批次计数仍是旧值。
 */
@Component
@RequiredArgsConstructor
public class ReconciliationStatsUpdater {

    private final ReconciliationItemMapper itemMapper;
    private final ReconciliationBatchMapper batchMapper;

    /** 重新汇总批次计数，并写回数据库 */
    public ReconciliationBatch refresh(ReconciliationBatch batch) {
        if (batch == null || batch.getId() == null) {
            return batch;
        }
        List<ReconciliationItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<ReconciliationItem>()
                        .eq(ReconciliationItem::getBatchId, batch.getId()));

        int matched = 0;
        int unmatched = 0;
        int ambiguous = 0;
        int ignored = 0;
        BigDecimal expenseNet = BigDecimal.ZERO;
        for (ReconciliationItem item : items) {
            String status = item.getMatchStatus();
            if (ReconcileConstants.STATUS_MATCHED.equals(status)) {
                matched++;
            } else if (ReconcileConstants.STATUS_AMBIGUOUS.equals(status)) {
                ambiguous++;
            } else if (ReconcileConstants.STATUS_IGNORED.equals(status)) {
                ignored++;
            } else {
                unmatched++;
            }
            if (ReconcileConstants.DIRECTION_EXPENSE.equals(item.getDirection())
                    && item.getAmount() != null) {
                BigDecimal refunded = item.getRefunded() == null ? BigDecimal.ZERO : item.getRefunded();
                expenseNet = expenseNet.add(item.getAmount().subtract(refunded));
            }
        }
        batch.setTotalCount(items.size());
        batch.setTotalAmount(expenseNet);
        batch.setMatchedCount(matched);
        batch.setUnmatchedCount(unmatched + ambiguous);
        batch.setIgnoredCount(ignored);
        batch.setStatus(unmatched + ambiguous == 0
                ? ReconcileConstants.BATCH_DONE : ReconcileConstants.BATCH_PARTIAL);
        batchMapper.updateById(batch);
        return batch;
    }

    /** 交易被修改/删除时解绑对应账单行，并刷新所有受影响批次 */
    public void detachTransaction(Long transactionId, String reason) {
        if (transactionId == null) {
            return;
        }
        List<ReconciliationItem> linked = itemMapper.selectList(
                new LambdaQueryWrapper<ReconciliationItem>()
                        .eq(ReconciliationItem::getMatchTransactionId, transactionId));
        Set<Long> batchIds = new LinkedHashSet<>();
        for (ReconciliationItem item : linked) {
            if (item.getBatchId() != null) {
                batchIds.add(item.getBatchId());
            }
        }
        itemMapper.detachByTransactionId(transactionId, reason);
        for (Long batchId : batchIds) {
            refresh(batchMapper.selectById(batchId));
        }
    }
}