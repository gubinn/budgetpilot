package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名不能超过50个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 100, message = "密码不能超过100个字符")
    private String password;

    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;

    @NotNull(message = "角色不能为空")
    private String role;

    private Boolean isActive = true;
}
