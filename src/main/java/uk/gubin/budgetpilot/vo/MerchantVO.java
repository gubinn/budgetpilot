package uk.gubin.budgetpilot.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商户响应 VO
 */
@Data
public class MerchantVO {

    private Long id;

    /**
     * 商户名称
     */
    private String name;

    /**
     * 商户别名
     */
    private String alias;

    /**
     * 关联分类ID
     */
    private Long categoryId;

    /**
     * 关联分类名称
     */
    private String categoryName;

    /**
     * 关联分类颜色
     */
    private String categoryColor;

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
    private String description;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 使用次数
     */
    private Integer usageCount;

    /**
     * 最近使用日期
     */
    private LocalDate lastUsedAt;

    /**
     * 是否启用
     */
    private Boolean isActive;

    /**
     * 系统预设商户
     */
    private Boolean isSystem;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}