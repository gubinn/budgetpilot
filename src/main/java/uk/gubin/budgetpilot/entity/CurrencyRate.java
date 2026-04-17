package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_currency_rate")
public class CurrencyRate extends BaseEntity {
    private String baseCurrency;
    private String targetCurrency;
    private java.math.BigDecimal rate;
    private LocalDate rateDate;
    private String source;
}
