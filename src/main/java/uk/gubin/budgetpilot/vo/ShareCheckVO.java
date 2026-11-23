package uk.gubin.budgetpilot.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分享页打开时的轻量检查；只有验证码通过后才会返回主人信息。
 */
@Data
public class ShareCheckVO {
    private boolean valid;
    private boolean needsPin;
    private String label;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;
}