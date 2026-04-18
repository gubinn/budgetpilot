package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryCreateDTO {
    private Long parentId = 0L;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 30, message = "分类名称不能超过30个字符")
    private String name;

    @NotNull(message = "分类类型不能为空")
    @Min(value = 1, message = "分类类型必须为1-3之间的值")
    @Max(value = 3, message = "分类类型必须为1-3之间的值")
    private Integer type;

    private String icon;
    private String color;
    private Integer sortOrder = 0;
}
