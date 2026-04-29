package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import uk.gubin.budgetpilot.entity.AlertRule;

import java.util.List;

public interface AlertRuleMapper extends BaseMapper<AlertRule> {

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_alert_rule WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM t_alert_rule WHERE user_id = #{userId} AND is_active = 1 ORDER BY type")
    List<AlertRule> selectActiveByUserId(@Param("userId") Long userId);
}
