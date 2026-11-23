package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import uk.gubin.budgetpilot.entity.PrepaidCardUsage;

@Mapper
public interface PrepaidCardUsageMapper extends BaseMapper<PrepaidCardUsage> {
}