package uk.gubin.budgetpilot.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 商户名称归一化测试。
 */
class MerchantNameNormalizerTest {

    private final MerchantNameNormalizer normalizer = new MerchantNameNormalizer();

    @Test
    @DisplayName("支付渠道前缀、快捷支付后缀和公司后缀会被清洗")
    void cleansChannelAndLegalSuffix() {
        assertThat(normalizer.canonicalName("支付宝-北京某科技有限公司快捷支付"))
                .isEqualTo("北京某科技");
        assertThat(normalizer.canonicalName("财付通-微信支付-北京某餐饮有限公司"))
                .isEqualTo("北京某餐饮");
    }

    @Test
    @DisplayName("同一品牌的不同收单主体会聚合")
    void aggregatesBrandAliases() {
        assertThat(normalizer.canonicalName("支付宝-北京三快在线科技有限公司")).isEqualTo("美团");
        assertThat(normalizer.canonicalName("财付通-美团平台商户")).isEqualTo("美团");
        assertThat(normalizer.canonicalName("支付宝-上海拉扎斯信息科技有限公司")).isEqualTo("饿了么");
        assertThat(normalizer.canonicalName("支付宝-金拱门（中国）有限公司")).isEqualTo("麦当劳");
        assertThat(normalizer.canonicalName("财付通-北京麦当劳食品有限公司")).isEqualTo("麦当劳");
    }

    @Test
    @DisplayName("京东、携程等渠道有保底品牌，具体商品品牌优先")
    void channelFallbackAndSpecificBrand() {
        assertThat(normalizer.canonicalName("京东支付-glinet京东自营旗舰店")).isEqualTo("京东");
        assertThat(normalizer.canonicalName("财付通-京东商城平台商户")).isEqualTo("京东");
        assertThat(normalizer.canonicalName("京东支付-京东大药房（北京）有限公司")).isEqualTo("京东大药房");
        assertThat(normalizer.canonicalName("京东支付-小米京东自营旗舰店")).isEqualTo("小米");
        assertThat(normalizer.canonicalName("程支付-携程旅行网")).isEqualTo("携程");
    }

    @Test
    @DisplayName("北京大学与下属机构保持区分")
    void keepsUniversityEntitiesDistinct() {
        assertThat(normalizer.canonicalName("支付宝-北京大学")).isEqualTo("北京大学");
        assertThat(normalizer.canonicalName("支付宝-北京大学口腔医院")).isEqualTo("北京大学口腔医院");
        assertThat(normalizer.canonicalName("支付宝-北京大学软件与微电子学院"))
                .isEqualTo("北京大学软件与微电子学院");
    }

    @Test
    @DisplayName("Apple Pay、数字人民币和云闪付等渠道名称")
    void specialChannels() {
        assertThat(normalizer.canonicalName("Apple Pay, 线上支付消费，北京市政交通一卡通有限公司"))
                .isEqualTo("北京一卡通");
        assertThat(normalizer.canonicalName("Apple Pay, 线上支付消费，Octopus Cards Limited"))
                .isEqualTo("八达通");
        assertThat(normalizer.canonicalName("数字人民币随用随充消费")).isEqualTo("数字人民币");
        assertThat(normalizer.canonicalName("银联扣款-银联在线支付，银联转账（云闪付"))
                .isEqualTo("云闪付");
    }

    @Test
    @DisplayName("门店后缀和截断的省略号会被清理")
    void stripsBranchAndEllipsis() {
        assertThat(normalizer.canonicalName("支付宝-某咖啡（国贸店）")).isEqualTo("某咖啡");
        assertThat(normalizer.canonicalName("支付宝-支付宝-消费-北京三快在线科技…快捷支付"))
                .isEqualTo("美团");
    }

    @Test
    @DisplayName("归一化结果保持幂等")
    void idempotent() {
        String[] samples = {
                "支付宝-北京三快在线科技有限公司",
                "京东支付-glinet京东自营旗舰店",
                "支付宝-北京大学口腔医院",
                "财付通-微信支付-某商户快捷支付",
                "Apple Pay, 线上支付消费，北京市政交通一卡通有限公司",
                "数字人民币随用随充消费"
        };
        for (String sample : samples) {
            String once = normalizer.canonicalName(sample);
            assertThat(normalizer.canonicalName(once)).as(sample).isEqualTo(once);
        }
    }
}