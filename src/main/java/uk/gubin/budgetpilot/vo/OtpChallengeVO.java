package uk.gubin.budgetpilot.vo;

import lombok.Data;

/**
 * 公网登录验证码挑战。
 */
@Data
public class OtpChallengeVO {
    private boolean needVerify = true;
    private String challengeId;
    /** 展示用渠道名称，如 Telegram / WhatsApp */
    private String channel;
    /** 脱敏后的接收者，如 ***1234 */
    private String recipient;
    /** 验证码有效期（秒） */
    private int expireIn;
}