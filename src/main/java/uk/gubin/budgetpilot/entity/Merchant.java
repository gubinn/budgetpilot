package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

import java.time.LocalDate;

/**
 * 商户实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_merchant")
public class Merchant extends BaseEntity {

    /**
     * 商户名称（唯一）
     */
    private String name;

    /**
     * 商户别名（用于模糊匹配）
     */
    private String alias;

    /**
     * 关联分类ID（可选，用于智能推荐）
     */
    private Long categoryId;

    /**
     * 图标标识
     */
    private String icon;

    /**
     * 颜色 #RRGGBB
     */
    private String color;

    /**
     * 商户描述
     */
    private String description;

    /**
     * 标签数组 JSON
     */
    @TableField("tags")
    private String tags;

    /**
     * 使用次数（用于排序推荐）
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
}