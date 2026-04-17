package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import uk.gubin.budgetpilot.entity.Account;

import java.math.BigDecimal;

public interface AccountMapper extends BaseMapper<Account> {

    @Update("UPDATE t_account SET current_balance = current_balance + #{adjustAmount} WHERE id = #{accountId}")
    int adjustBalance(@Param("accountId") Long accountId, @Param("adjustAmount") BigDecimal adjustAmount);
}
