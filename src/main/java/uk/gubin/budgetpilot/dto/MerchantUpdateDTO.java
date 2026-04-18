package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 商户更新 DTO
 */
@Data
public class MerchantUpdateDTO {

    /**
     * 商户名称
     */
    @Size(max = 100, message = "商户名称最多100字符")
    private String name;

    /**
     * 商户别名
     */
    @Size(max = 200, message = "商户别名最多200字符")
    private String alias;

    /**
     * 关联分类ID
     */
    private Long categoryId;

    /**
     * 图标标识
     */
    private String icon;

    /**
     * 颜色
     */
    private String color;

    /**
     * 商户描述
     */
    @Size(max = 200, message = "描述最多200字符")
    private String description;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 是否启用
     */
    private Boolean isActive;
}