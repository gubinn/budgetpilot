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
     * 按 parent_id+name+type 去重，取最小 id，避免 DataInitializer 多次运行产生重复；
     * 不能只按 name+type，否则各大类下同名的“其他”兜底分类会被误删
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT c.* FROM t_category c INNER JOIN (" +
            "  SELECT parent_id, name, type, MIN(id) AS min_id FROM t_category " +
            "  WHERE is_system = true AND is_active = true " +
            "  GROUP BY parent_id, name, type" +
            ") d ON c.parent_id = d.parent_id AND c.name = d.name " +
            "  AND c.type = d.type AND c.id = d.min_id " +
            "ORDER BY c.parent_id, c.sort_order, c.id")
    List<Category> selectSystemDefaults();

    /**
     * 忽略租户拦截器按ID查询分类（系统分类 user_id=0，用户分类 user_id=X）
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM t_category WHERE id = #{id}")
    Category selectByIdIgnoreTenant(@Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM t_category WHERE icon = 'adjust' AND type = #{type} " +
            "AND user_id = 0 AND is_active = 1 LIMIT 1")
    Category selectSystemBalanceAdjust(@Param("type") int type);

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_category WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
