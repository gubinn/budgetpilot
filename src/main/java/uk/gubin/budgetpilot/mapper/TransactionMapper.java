package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import uk.gubin.budgetpilot.dto.MerchantUsageStatDTO;
import uk.gubin.budgetpilot.entity.Transaction;

import java.time.LocalDate;
import java.util.List;

public interface TransactionMapper extends BaseMapper<Transaction> {

    /**
     * 校准任务按账户读取全部已确认交易（含转入），绕过租户过滤。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM t_transaction WHERE is_confirmed = 1 AND transaction_date >= #{start} " +
            "AND (account_id = #{accountId} OR target_account_id = #{accountId})")
    List<Transaction> selectForCalibration(@Param("accountId") Long accountId, @Param("start") LocalDate start);

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_transaction WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Update("UPDATE t_transaction SET merchant_id = #{targetId} WHERE merchant_id = #{sourceId}")
    int reassignMerchant(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);

    @Select("<script>" +
            "SELECT merchant_id AS merchantId, COUNT(*) AS txCount, MAX(transaction_date) AS lastDate " +
            "FROM t_transaction WHERE merchant_id IN " +
            "<foreach collection='merchantIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY merchant_id" +
            "</script>")
    List<MerchantUsageStatDTO> countByMerchantIds(@Param("merchantIds") List<Long> merchantIds);

    @Select("SELECT category_id FROM t_transaction " +
            "WHERE merchant_id = #{merchantId} AND category_id IS NOT NULL " +
            "GROUP BY category_id ORDER BY COUNT(*) DESC, MAX(transaction_date) DESC LIMIT 1")
    Long findDominantCategoryId(@Param("merchantId") Long merchantId);

    @Select("SELECT category_id AS categoryId, COUNT(*) AS txCount FROM t_transaction " +
            "WHERE category_id IS NOT NULL GROUP BY category_id")
    List<uk.gubin.budgetpilot.dto.CategoryUsageStatDTO> countGroupedByCategory();

    @Update("UPDATE t_transaction SET category_id = #{targetId} WHERE category_id = #{sourceId}")
    int reassignCategory(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);
}
