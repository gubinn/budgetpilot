package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import uk.gubin.budgetpilot.entity.ReadonlyShare;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReadonlyShareMapper extends BaseMapper<ReadonlyShare> {

    /**
     * 分享页没有登录态，按分享码哈希公开查询。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM t_readonly_share WHERE code_hash = #{codeHash} LIMIT 1")
    ReadonlyShare selectByCodeHash(@Param("codeHash") String codeHash);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM t_readonly_share WHERE user_id = #{userId} ORDER BY id DESC")
    List<ReadonlyShare> selectByUserId(@Param("userId") Long userId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM t_readonly_share WHERE id = #{id} AND user_id = #{userId}")
    ReadonlyShare selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE t_readonly_share SET revoked_at = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId} AND revoked_at IS NULL")
    int revoke(@Param("id") Long id, @Param("userId") Long userId);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE t_readonly_share SET last_used_at = #{lastUsedAt}, last_used_ip = #{ip}, " +
            "view_count = view_count + 1 WHERE id = #{id}")
    int touch(@Param("id") Long id,
              @Param("lastUsedAt") LocalDateTime lastUsedAt,
              @Param("ip") String ip);

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_readonly_share WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}