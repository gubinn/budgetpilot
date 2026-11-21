package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.entity.User;
import uk.gubin.budgetpilot.entity.UserConfig;
import uk.gubin.budgetpilot.ingress.IngressInfo;
import uk.gubin.budgetpilot.ingress.IngressProperties;
import uk.gubin.budgetpilot.mapper.UserConfigMapper;
import uk.gubin.budgetpilot.vo.OtpChallengeVO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 公网登录验证码服务。
 *
 * <p>验证码通过用户自己配置的通知渠道（t_user_config）发送，
 * 未配置时回退到系统全局通知配置。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginOtpService {

    private static final String CHALLENGE_PREFIX = "auth:otp:challenge:";
    private static final String SEND_USER_PREFIX = "auth:otp:send:user:";
    private static final String SEND_IP_PREFIX = "auth:otp:send:ip:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final UserService userService;
    private final UserConfigMapper userConfigMapper;
    private final TelegramNotifyService telegramNotifyService;
    private final WhatsAppNotifyService whatsAppNotifyService;
    private final IngressProperties ingressProperties;

    /**
     * 密码校验通过后，为公网登录创建验证码挑战并发送验证码。
     */
    public OtpChallengeVO start(User user, IngressInfo ingress) {
        acquireSendSlot(user.getId(), ingress.clientIp());
        ChannelTarget target;
        try {
            target = resolveTarget(user.getId());
        } catch (RuntimeException e) {
            releaseSendSlot(user.getId());
            throw e;
        }
        String code = generateCode();
        String challengeId = UUID.randomUUID().toString();
        String key = challengeKey(challengeId);

        Map<String, String> data = new HashMap<>();
        data.put("uid", String.valueOf(user.getId()));
        data.put("codeHash", sha256(code));
        data.put("attempts", "0");
        data.put("channel", target.channel());
        data.put("clientIp", ingress.clientIp());
        redisTemplate.opsForHash().putAll(key, data);
        redisTemplate.expire(key, ingressProperties.getOtpTtl(), TimeUnit.SECONDS);

        if (!send(target, code, ingress.clientIp())) {
            redisTemplate.delete(key);
            releaseSendSlot(user.getId());
            throw new BizException(ErrorCode.AUTH_OTP_SEND_FAILED);
        }

        log.info("登录验证码已发送 userId={} channel={} recipient={}",
                user.getId(), target.channel(), target.maskedRecipient());
        return buildChallenge(challengeId, target);
    }

    /**
     * 校验验证码，成功后返回用户，由调用方创建会话。
     */
    public User verify(String challengeId, String code) {
        String key = challengeKey(challengeId);
        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
        if (data == null || data.isEmpty()) {
            throw new BizException(ErrorCode.AUTH_OTP_INVALID);
        }

        Long attempts = redisTemplate.opsForHash().increment(key, "attempts", 1);
        if (attempts != null && attempts > ingressProperties.getOtpMaxAttempts()) {
            redisTemplate.delete(key);
            throw new BizException(ErrorCode.AUTH_OTP_TOO_MANY);
        }

        String expected = (String) data.get("codeHash");
        if (expected == null || !constantTimeEquals(expected, sha256(code))) {
            if (attempts != null && attempts >= ingressProperties.getOtpMaxAttempts()) {
                redisTemplate.delete(key);
                throw new BizException(ErrorCode.AUTH_OTP_TOO_MANY);
            }
            throw new BizException(ErrorCode.AUTH_OTP_INVALID);
        }

        Long userId = parseUserId(data.get("uid"));
        redisTemplate.delete(key);
        User user = userId == null ? null : userService.getById(userId);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            throw new BizException(ErrorCode.AUTH_USER_DISABLED);
        }
        return user;
    }

    /**
     * 重新发送验证码（同一 challenge）。
     */
    public OtpChallengeVO resend(String challengeId, IngressInfo ingress) {
        String key = challengeKey(challengeId);
        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
        if (data == null || data.isEmpty()) {
            throw new BizException(ErrorCode.AUTH_OTP_INVALID);
        }

        Long userId = parseUserId(data.get("uid"));
        if (userId == null) {
            redisTemplate.delete(key);
            throw new BizException(ErrorCode.AUTH_OTP_INVALID);
        }

        acquireSendSlot(userId, ingress.clientIp());
        ChannelTarget target;
        try {
            target = resolveTarget(userId);
        } catch (RuntimeException e) {
            releaseSendSlot(userId);
            throw e;
        }
        String code = generateCode();

        redisTemplate.opsForHash().put(key, "codeHash", sha256(code));
        redisTemplate.opsForHash().put(key, "attempts", "0");
        redisTemplate.opsForHash().put(key, "channel", target.channel());
        redisTemplate.opsForHash().put(key, "clientIp", ingress.clientIp());
        redisTemplate.expire(key, ingressProperties.getOtpTtl(), TimeUnit.SECONDS);

        if (!send(target, code, ingress.clientIp())) {
            releaseSendSlot(userId);
            throw new BizException(ErrorCode.AUTH_OTP_SEND_FAILED);
        }

        return buildChallenge(challengeId, target);
    }

    /**
     * 只读取该用户自己的配置，不回退到系统全局配置。
     *
     * <p>用户可以在「默认通知渠道」里选择 TELEGRAM / WHATSAPP / BOTH；
     * 未选择时按已配置完整的渠道自动推断。</p>
     */
    private ChannelTarget resolveTarget(Long userId) {
        String telegramBotToken = userConfig(userId, "telegram_bot_token");
        String telegramChatId = userConfig(userId, "telegram_chat_id");
        String whatsappApiKey = userConfig(userId, "whatsapp_api_key");
        String whatsappTo = userConfig(userId, "whatsapp_to");
        String whatsappBaseUrl = userConfig(userId, "whatsapp_base_url");
        String configuredChannel = userConfig(userId, "notify_channel");

        boolean telegramReady = !isBlank(telegramBotToken) && !isBlank(telegramChatId);
        boolean whatsappReady = !isBlank(whatsappApiKey) && !isBlank(whatsappTo);

        if (!telegramReady && !whatsappReady) {
            throw new BizException(ErrorCode.AUTH_OTP_NO_CHANNEL);
        }

        String channel = configuredChannel == null ? "" : configuredChannel.trim().toUpperCase();
        if (channel.isEmpty()) {
            if (telegramReady && whatsappReady) {
                channel = "BOTH";
            } else {
                channel = telegramReady ? "TELEGRAM" : "WHATSAPP";
            }
        }

        return switch (channel) {
            case "TELEGRAM" -> {
                if (!telegramReady) {
                    throw new BizException(ErrorCode.AUTH_OTP_NO_CHANNEL,
                            "已选择 Telegram，但 Telegram Bot Token 或 Chat ID 未配置完整");
                }
                yield new ChannelTarget("TELEGRAM", "Telegram",
                        telegramBotToken, telegramChatId, null, null, null);
            }
            case "WHATSAPP" -> {
                if (!whatsappReady) {
                    throw new BizException(ErrorCode.AUTH_OTP_NO_CHANNEL,
                            "已选择 WhatsApp，但 WhatsApp API Key 或接收者未配置完整");
                }
                yield new ChannelTarget("WHATSAPP", "WhatsApp",
                        null, null, whatsappApiKey, whatsappBaseUrl, whatsappTo);
            }
            case "BOTH" -> {
                if (!telegramReady || !whatsappReady) {
                    throw new BizException(ErrorCode.AUTH_OTP_NO_CHANNEL,
                            "已选择「两者都发」，需要同时配置 Telegram 和 WhatsApp");
                }
                yield new ChannelTarget("BOTH", "Telegram + WhatsApp",
                        telegramBotToken, telegramChatId, whatsappApiKey, whatsappBaseUrl, whatsappTo);
            }
            default -> throw new BizException(ErrorCode.AUTH_OTP_NO_CHANNEL);
        };
    }

    private boolean send(ChannelTarget target, String code, String clientIp) {
        String minutes = String.valueOf(Math.max(1, ingressProperties.getOtpTtl() / 60));
        String message = """
                🔐 BudgetPilot 登录验证码

                %s

                %s 分钟内有效。
                登录位置：%s
                如非本人操作，请忽略本条消息并修改密码。
                """.formatted(code, minutes, clientIp == null ? "未知" : clientIp);

        boolean sent = false;
        if (!isBlank(target.telegramChatId())) {
            sent = telegramNotifyService.sendToChat(
                    target.telegramBotToken(), target.telegramChatId(), message) || sent;
        }
        if (!isBlank(target.whatsappTo())) {
            sent = whatsAppNotifyService.sendToRecipient(
                    target.whatsappApiKey(), target.whatsappBaseUrl(), target.whatsappTo(), message) || sent;
        }
        return sent;
    }

    /**
     * 原子占用发送名额：同一用户 60 秒只能发一次；同一 IP 10 分钟内最多 10 次。
     */
    private void acquireSendSlot(Long userId, String clientIp) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                SEND_USER_PREFIX + userId, "1",
                ingressProperties.getOtpResendSeconds(), TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BizException(ErrorCode.AUTH_OTP_RESEND_LIMIT);
        }

        if (!isBlank(clientIp)) {
            String ipKey = SEND_IP_PREFIX + clientIp;
            Long count = redisTemplate.opsForValue().increment(ipKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(ipKey, 10, TimeUnit.MINUTES);
            }
            if (count != null && count > ingressProperties.getOtpIpLimit()) {
                releaseSendSlot(userId);
                throw new BizException(ErrorCode.AUTH_OTP_RESEND_LIMIT);
            }
        }
    }

    private void releaseSendSlot(Long userId) {
        try {
            redisTemplate.delete(SEND_USER_PREFIX + userId);
        } catch (Exception ignored) {
        }
    }

    private OtpChallengeVO buildChallenge(String challengeId, ChannelTarget target) {
        OtpChallengeVO vo = new OtpChallengeVO();
        vo.setChallengeId(challengeId);
        vo.setChannel(target.channelLabel());
        vo.setRecipient(target.maskedRecipient());
        vo.setExpireIn((int) ingressProperties.getOtpTtl());
        return vo;
    }

    private String userConfig(Long userId, String key) {
        UserConfig config = userConfigMapper.selectOne(new LambdaQueryWrapper<UserConfig>()
                .eq(UserConfig::getUserId, userId)
                .eq(UserConfig::getConfigKey, key));
        return config == null ? null : config.getConfigValue();
    }

    private static String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private static Long parseUserId(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String mask(String value) {
        if (isBlank(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "***" + trimmed.substring(trimmed.length() - 4);
    }

    private static String challengeKey(String challengeId) {
        return CHALLENGE_PREFIX + challengeId;
    }

    private record ChannelTarget(String channel, String channelLabel,
                                 String telegramBotToken, String telegramChatId,
                                 String whatsappApiKey, String whatsappBaseUrl, String whatsappTo) {
        String maskedRecipient() {
            boolean hasTelegram = telegramChatId != null;
            boolean hasWhatsApp = whatsappTo != null;
            if (hasTelegram && hasWhatsApp) {
                return "Telegram " + mask(telegramChatId) + " / WhatsApp " + mask(whatsappTo);
            }
            return mask(hasTelegram ? telegramChatId : whatsappTo);
        }
    }
}