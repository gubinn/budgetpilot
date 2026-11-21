package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import uk.gubin.budgetpilot.entity.RecurringRule;

public interface RecurringRuleMapper extends BaseMapper<RecurringRule> {

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_recurring_rule WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Update("UPDATE t_recurring_rule SET merchant_id = #{targetId} WHERE merchant_id = #{sourceId}")
    int reassignMerchant(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);
}
