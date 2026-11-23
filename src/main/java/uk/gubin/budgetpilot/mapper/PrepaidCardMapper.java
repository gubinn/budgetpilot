package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import uk.gubin.budgetpilot.entity.PrepaidCard;

@Mapper
public interface PrepaidCardMapper extends BaseMapper<PrepaidCard> {

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_prepaid_card WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}