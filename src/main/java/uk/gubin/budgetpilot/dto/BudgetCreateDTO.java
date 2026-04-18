package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BudgetCreateDTO {
    @NotBlank(message = "月份不能为空")
    private String yearMonth;

    @NotNull(message = "总预算不能为空")
    @Positive(message = "总预算必须大于0")
    private BigDecimal totalAmount;

    @Size(max = 200, message = "备注不能超过200个字符")
    private String note;
    private List<BudgetItemDTO> items;

    @Data
    public static class BudgetItemDTO {
        @NotNull(message = "分类ID不能为空")
        private Long categoryId;

        @NotNull(message = "分类预算不能为空")
        @Positive(message = "分类预算必须大于0")
        private BigDecimal amount;
    }
}
