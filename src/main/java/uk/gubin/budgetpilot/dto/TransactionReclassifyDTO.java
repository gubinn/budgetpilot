package uk.gubin.budgetpilot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量重分类交易（只改分类，不触碰余额/预算/预警）
 */
@Data
public class TransactionReclassifyDTO {

    @NotEmpty(message = "重分类列表不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "交易ID不能为空")
        private Long id;

        @NotNull(message = "分类ID不能为空")
        private Long categoryId;
    }
}