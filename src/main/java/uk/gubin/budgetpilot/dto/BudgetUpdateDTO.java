package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BudgetUpdateDTO {
    @Positive(message = "总预算必须大于0")
    private BigDecimal totalAmount;

    private String note;

    private List<BudgetItemDTO> items;

    @Data
    public static class BudgetItemDTO {
        @jakarta.validation.constraints.NotNull(message = "分类ID不能为空")
        private Long categoryId;

        @jakarta.validation.constraints.NotNull(message = "分类预算不能为空")
        @Positive(message = "分类预算必须大于0")
        private BigDecimal amount;
    }
}
