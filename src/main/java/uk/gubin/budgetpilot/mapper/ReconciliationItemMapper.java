package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import uk.gubin.budgetpilot.entity.ReconciliationItem;

@Mapper
public interface ReconciliationItemMapper extends BaseMapper<ReconciliationItem> {

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_reconciliation_item WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /** 交易被修改/删除后，把对应账单行退回待处理 */
    @Update("UPDATE t_reconciliation_item SET match_status = 'UNMATCHED', match_type = NULL, " +
            "match_transaction_id = NULL, matched_at = NULL, ignore_reason = #{reason} " +
            "WHERE match_transaction_id = #{transactionId} AND match_status = 'MATCHED'")
    int detachByTransactionId(@Param("transactionId") Long transactionId,
                              @Param("reason") String reason);

    /** 取消某批次的所有自动匹配（保留手工匹配/补录/忽略） */
    @Update("UPDATE t_reconciliation_item SET match_status = 'UNMATCHED', match_type = NULL, " +
            "match_transaction_id = NULL, matched_at = NULL " +
            "WHERE batch_id = #{batchId} AND match_status = 'MATCHED' " +
            "AND match_type IN ('AUTO', 'AGGREGATE')")
    int resetAutoMatches(@Param("batchId") Long batchId);
}