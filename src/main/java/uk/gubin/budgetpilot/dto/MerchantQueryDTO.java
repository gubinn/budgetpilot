package uk.gubin.budgetpilot.dto;

import lombok.Data;

/**
 * 商户查询 DTO
 */
@Data
public class MerchantQueryDTO {

    /**
     * 关键字搜索（名称/别名模糊匹配）
     */
    private String keyword;

    /**
     * 按分类筛选
     */
    private Long categoryId;

    /**
     * 活跃状态筛选
     */
    private Boolean isActive;

    /**
     * 页码（默认 1）
     */
    private Integer page = 1;

    /**
     * 每页条数（默认 20）
     */
    private Integer size = 20;

    /**
     * 排序字段（默认按使用频次排序）
     * 格式: field,order 如 usage_count,desc
     */
    private String sort = "usage_count,desc";
}