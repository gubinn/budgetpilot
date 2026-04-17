package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_currency_rate")
public class CurrencyRate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String baseCurrency;
    private String targetCurrency;
    private java.math.BigDecimal rate;
    private LocalDate rateDate;
    private String source;
    private LocalDateTime createdAt;
}
