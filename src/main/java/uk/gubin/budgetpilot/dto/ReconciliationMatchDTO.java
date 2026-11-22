package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReconciliationMatchDTO {
    @NotNull(message = "请选择要匹配的交易")
    private Long transactionId;
}