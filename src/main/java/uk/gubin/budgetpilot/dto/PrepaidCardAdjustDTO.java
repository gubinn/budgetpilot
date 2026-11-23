package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 使用 / 充值卡券。value 始终为正数，方向由接口决定。
 */
@Data
public class PrepaidCardAdjustDTO {
    @NotNull(message = "金额/次数不能为空")
    @DecimalMin(value = "0.01", message = "金额/次数必须大于0")
    @Digits(integer = 10, fraction = 2, message = "金额/次数格式不正确")
    private BigDecimal value;

    private LocalDate occurredAt;

    @Size(max = 100, message = "备注不能超过100个字符")
    private String note;
}