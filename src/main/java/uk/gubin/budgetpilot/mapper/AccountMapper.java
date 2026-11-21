package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import uk.gubin.budgetpilot.entity.Account;

import java.math.BigDecimal;
import java.util.List;

public interface AccountMapper extends BaseMapper<Account> {

    /**
     * 定时任务没有登录上下文，必须显式忽略租户过滤。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM t_account WHERE is_active = 1")
    List<Account> selectAllActive();

    /**
     * 校准任务只更新余额，绕过租户过滤（定时任务线程没有登录用户）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE t_account SET current_balance = #{balance}, updated_at = NOW() WHERE id = #{id}")
    int updateBalanceForCalibration(@Param("id") Long id, @Param("balance") BigDecimal balance);

    @Update("UPDATE t_account SET current_balance = current_balance + #{adjustAmount} WHERE id = #{accountId}")
    int adjustBalance(@Param("accountId") Long accountId, @Param("adjustAmount") BigDecimal adjustAmount);

    /**
     * 原子扣减余额：仅当余额充足时成功，返回受影响行数
     */
    @Update("UPDATE t_account SET current_balance = current_balance - #{amount} WHERE id = #{accountId} AND current_balance >= #{amount}")
    int decreaseBalanceIfSufficient(@Param("accountId") Long accountId, @Param("amount") BigDecimal amount);

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_account WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
