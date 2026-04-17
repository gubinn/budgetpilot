package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BalanceAdjustDTO {
    @NotNull(message = "新余额不能为空")
    private BigDecimal newBalance;

    @NotBlank(message = "调整原因不能为空")
    private String reason;
}