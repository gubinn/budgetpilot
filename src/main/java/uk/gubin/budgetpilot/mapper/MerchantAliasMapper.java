package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import uk.gubin.budgetpilot.entity.MerchantAlias;

/**
 * 商户别名 Mapper
 */
@Mapper
public interface MerchantAliasMapper extends BaseMapper<MerchantAlias> {

    @Delete("DELETE FROM t_merchant_alias WHERE merchant_id = #{merchantId}")
    int deleteByMerchantId(@Param("merchantId") Long merchantId);

    @Update("UPDATE t_merchant_alias SET merchant_id = #{targetId} WHERE merchant_id = #{sourceId}")
    int reassignMerchant(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM t_merchant_alias WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}