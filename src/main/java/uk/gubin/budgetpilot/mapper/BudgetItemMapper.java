package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import uk.gubin.budgetpilot.entity.BudgetItem;

public interface BudgetItemMapper extends BaseMapper<BudgetItem> {

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_budget_item WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
