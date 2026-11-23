package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReadonlyShareVerifyDTO {
    @Size(max = 10, message = "PIN 格式不正确")
    @Pattern(regexp = "^$|^\\d{4,6}$", message = "PIN 格式不正确")
    private String pin;
}