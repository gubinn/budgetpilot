package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

/**
 * 商户原始名称别名。
 *
 * <p>支付渠道返回的原始名称（例如“支付宝-北京三快在线科技有限公司”）保存在这里，
 * 指向聚合后的商户。这样既能聚合商户，又不会丢掉分类学习所需的原始名称。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_merchant_alias")
public class MerchantAlias extends BaseEntity {

    private Long userId;

    private Long merchantId;

    private String rawName;
}