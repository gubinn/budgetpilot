package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PrepaidCardCreateDTO {
    @NotBlank(message = "卡券名称不能为空")
    @Size(max = 60, message = "卡券名称不能超过60个字符")
    private String name;

    @NotBlank(message = "发卡方不能为空")
    @Size(max = 60, message = "发卡方不能超过60个字符")
    private String issuer;

    @Size(max = 40, message = "渠道不能超过40个字符")
    private String channel;

    @Size(max = 60, message = "账号提示不能超过60个字符")
    private String accountHint;

    @Min(value = 1, message = "卡券类型无效")
    @Max(value = 4, message = "卡券类型无效")
    private Integer kind = 1;

    @Min(value = 1, message = "计量单位无效")
    @Max(value = 2, message = "计量单位无效")
    private Integer unit = 1;

    @Size(max = 3, message = "币种无效")
    private String currency = "CNY";

    @DecimalMin(value = "0", message = "面值不能为负数")
    @Digits(integer = 10, fraction = 2, message = "面值格式不正确")
    private BigDecimal faceValue;

    @DecimalMin(value = "0", message = "余额不能为负数")
    @Digits(integer = 10, fraction = 2, message = "余额格式不正确")
    private BigDecimal balance;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    @Min(value = 1, message = "来源无效")
    @Max(value = 4, message = "来源无效")
    private Integer source = 4;

    @Size(max = 200, message = "备注不能超过200个字符")
    private String note;
}