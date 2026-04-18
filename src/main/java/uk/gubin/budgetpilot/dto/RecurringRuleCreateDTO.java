package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecurringRuleCreateDTO {
    @NotNull(message = "规则名称不能为空")
    private String name;

    @NotNull(message = "类型不能为空")
    private Integer type; // 1-支出 2-收入

    @NotNull @Positive(message = "金额必须大于0")
    private BigDecimal amount;

    private String currency = "CNY";

    @NotNull(message = "账户不能为空")
    private Long accountId;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private Long merchantId;

    @NotNull(message = "频率不能为空")
    private String frequency; // DAILY/WEEKLY/MONTHLY/YEARLY

    private Integer executeDay; // 月执行日(1-28), 周执行日(1-7)

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    private LocalDate endDate; // null = 永久

    private LocalDate nextExecute; // 首次执行日期，默认startDate

    private Boolean autoConfirm = false; // 是否自动确认

    private String note;
    private String extFields;
}