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
     * 按 name+type 去重，取最小 id，避免 DataInitializer 多次运行产生重复
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT c.* FROM t_category c INNER JOIN (" +
            "  SELECT name, type, MIN(id) AS min_id FROM t_category " +
            "  WHERE is_system = true AND is_active = true " +
            "  GROUP BY name, type" +
            ") d ON c.name = d.name AND c.type = d.type AND c.id = d.min_id " +
            "ORDER BY c.sort_order")
    List<Category> selectSystemDefaults();

    /**
     * 忽略租户拦截器按ID查询分类（系统分类 user_id=0，用户分类 user_id=X）
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM t_category WHERE id = #{id}")
    Category selectByIdIgnoreTenant(@Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_category WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
