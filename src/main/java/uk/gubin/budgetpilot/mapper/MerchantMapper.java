package uk.gubin.budgetpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import uk.gubin.budgetpilot.entity.Merchant;

import java.time.LocalDate;

/**
 * 商户 Mapper
 */
@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {

    /**
     * 增加使用次数并更新最近使用日期
     */
    @Update("UPDATE t_merchant SET usage_count = usage_count + 1, last_used_at = #{lastUsedAt} WHERE id = #{merchantId}")
    int incrementUsage(@Param("merchantId") Long merchantId, @Param("lastUsedAt") LocalDate lastUsedAt);
}