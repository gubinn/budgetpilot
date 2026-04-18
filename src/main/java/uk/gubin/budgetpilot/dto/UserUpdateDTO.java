package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateDTO {
    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;
    @Pattern(regexp = "ADMIN|USER", message = "角色必须为ADMIN或USER之一")
    private String role;
    private Boolean isActive;
}
