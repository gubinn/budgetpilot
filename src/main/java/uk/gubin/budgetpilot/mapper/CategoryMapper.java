package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import uk.gubin.budgetpilot.entity.Category;

import java.util.List;

public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 忽略租户拦截器查询系统预置分类（is_system=true 的分类）
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM t_category WHERE is_system = true AND is_active = true ORDER BY sort_order")
    List<Category> selectSystemDefaults();

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_category WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
