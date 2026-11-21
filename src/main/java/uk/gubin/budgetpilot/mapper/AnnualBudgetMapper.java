package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import uk.gubin.budgetpilot.entity.AnnualBudget;

public interface AnnualBudgetMapper extends BaseMapper<AnnualBudget> {

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_annual_budget WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}