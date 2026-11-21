package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpResendDTO {

    @NotBlank(message = "challengeId 不能为空")
    private String challengeId;
}