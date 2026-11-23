package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import uk.gubin.budgetpilot.entity.ReconciliationBatch;

@Mapper
public interface ReconciliationBatchMapper extends BaseMapper<ReconciliationBatch> {

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_reconciliation_batch WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}