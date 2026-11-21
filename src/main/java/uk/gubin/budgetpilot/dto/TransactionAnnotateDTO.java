package uk.gubin.budgetpilot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 批量补充标签/扩展字段（只改 tags、ext_fields，不触碰余额和预算）
 */
@Data
public class TransactionAnnotateDTO {

    @NotEmpty(message = "标注列表不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "交易ID不能为空")
        private Long id;

        /** 追加标签（与已有标签合并去重） */
        private List<String> addTags;

        /** 合并扩展字段（保留已有 key） */
        private Map<String, Object> extFields;
    }
}