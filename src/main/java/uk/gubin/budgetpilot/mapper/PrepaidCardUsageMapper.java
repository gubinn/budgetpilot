package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import uk.gubin.budgetpilot.entity.PrepaidCardUsage;

@Mapper
public interface PrepaidCardUsageMapper extends BaseMapper<PrepaidCardUsage> {

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_prepaid_card_usage WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}