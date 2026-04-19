package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import uk.gubin.budgetpilot.entity.Account;

import java.math.BigDecimal;

public interface AccountMapper extends BaseMapper<Account> {

    @Update("UPDATE t_account SET current_balance = current_balance + #{adjustAmount} WHERE id = #{accountId}")
    int adjustBalance(@Param("accountId") Long accountId, @Param("adjustAmount") BigDecimal adjustAmount);

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_account WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
