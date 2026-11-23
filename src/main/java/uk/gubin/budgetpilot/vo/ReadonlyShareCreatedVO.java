package uk.gubin.budgetpilot.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新建分享后返回一次，分享码和 PIN 只在此时返回明文。
 */
@Data
public class ReadonlyShareCreatedVO {
    private Long id;
    private String label;
    private String code;
    /** 前端拼成完整链接：/share/{code} */
    private String path;
    private String pin;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;
}