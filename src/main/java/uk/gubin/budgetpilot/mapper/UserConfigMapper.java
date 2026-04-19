package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import uk.gubin.budgetpilot.entity.UserConfig;

public interface UserConfigMapper extends BaseMapper<UserConfig> {

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_user_config WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
