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
    /** DAILY / FIXED / SPECIAL / NONE */
    private String budgetType;
    /** 1=不计入收支报表 */
    private Boolean excludeFromReports;
    /** 当前分类及其子分类的交易笔数 */
    private Long transactionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CategoryVO> children;
}
