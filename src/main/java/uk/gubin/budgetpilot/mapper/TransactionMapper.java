package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import uk.gubin.budgetpilot.entity.Transaction;

public interface TransactionMapper extends BaseMapper<Transaction> {

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_transaction WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
