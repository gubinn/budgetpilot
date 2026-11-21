package uk.gubin.budgetpilot.dto;

import lombok.Data;

@Data
public class CategoryUsageStatDTO {
    private Long categoryId;
    private Long txCount;
}