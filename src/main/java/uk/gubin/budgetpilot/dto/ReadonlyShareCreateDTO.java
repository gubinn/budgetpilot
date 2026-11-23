package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReadonlyShareCreateDTO {
    @Size(max = 60, message = "备注不能超过60个字符")
    private String label;

    @NotNull(message = "有效期不能为空")
    @Min(value = 1, message = "有效期至少 1 天")
    @Max(value = 90, message = "有效期最多 90 天")
    private Integer expiresInDays = 7;

    /** 留空表示免 PIN；有值必须是 4-6 位数字 */
    @Pattern(regexp = "^$|^\\d{4,6}$", message = "PIN 必须是 4-6 位数字")
    private String pin;
}