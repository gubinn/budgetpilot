package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import uk.gubin.budgetpilot.entity.ReconciliationBatch;

@Mapper
public interface ReconciliationBatchMapper extends BaseMapper<ReconciliationBatch> {
}