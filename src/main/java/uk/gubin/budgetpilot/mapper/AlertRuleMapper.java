package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import uk.gubin.budgetpilot.entity.AlertRule;

public interface AlertRuleMapper extends BaseMapper<AlertRule> {

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_alert_rule WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
