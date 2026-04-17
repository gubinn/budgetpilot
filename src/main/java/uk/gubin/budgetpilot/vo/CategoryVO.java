package uk.gubin.budgetpilot.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CategoryVO {
    private Long id;
    private Long parentId;
    private String name;
    private Integer type;
    private String icon;
    private String color;
    private Integer sortOrder;
    private Boolean isSystem;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CategoryVO> children;
}
