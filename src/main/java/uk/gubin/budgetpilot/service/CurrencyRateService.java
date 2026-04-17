package uk.gubin.budgetpilot.service;

import uk.gubin.budgetpilot.entity.CurrencyRate;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CurrencyRateService {
    /**
     * 获取指定日期的汇率
     * 优先级: Redis → MySQL → 外部 API → 降级(最近3天)
     */
    BigDecimal getRate(String currency, LocalDate date);

    /**
     * 将金额转换为本位币
     */
    BigDecimal convertToBase(BigDecimal amount, String currency, LocalDate date);

    /**
     * 刷新所有支持币种的汇率
     */
    void refreshRates();

    /**
     * 获取或创建汇率记录
     */
    CurrencyRate getOrCreateRate(String currency, LocalDate date);

    /**
     * 清理过期汇率数据
     */
    void cleanupOldRates(int monthsBefore);
}
