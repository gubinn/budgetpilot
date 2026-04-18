package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_category")
public class Category extends BaseEntity {
    private Long userId;
    private Long parentId;
    private String name;
    private Integer type;
    private String icon;
    private String color;
    private Integer sortOrder;
    private Boolean isSystem;
    private Boolean isActive;
}
