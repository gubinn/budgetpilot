package uk.gubin.budgetpilot.dto;

import lombok.Data;

@Data
public class UserUpdateDTO {
    private String nickname;
    private String role;
    private Boolean isActive;
}
