package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

import java.time.LocalDateTime;

/**
 * 只读共享账本链接。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_readonly_share")
public class ReadonlyShare extends BaseEntity {
    private Long userId;
    private String label;
    private String codeHash;
    private String codeHint;
    private String pinHash;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private LocalDateTime lastUsedAt;
    private String lastUsedIp;
    private Integer viewCount;
}