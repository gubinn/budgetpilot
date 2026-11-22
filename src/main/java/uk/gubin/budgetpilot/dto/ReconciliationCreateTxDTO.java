package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 账单有、流水无时手工补录交易。
 * 账户决定余额变动方向，必须由用户明确选择；分类不填时后端按收支方向给默认分类。
 */
@Data
public class ReconciliationCreateTxDTO {
    @NotNull(message = "请选择入账账户")
    private Long accountId;

    @NotNull(message = "请选择分类")
    private Long categoryId;

    private Boolean confirmed = true;
}