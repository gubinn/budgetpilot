package uk.gubin.budgetpilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotifyService {

    private final StringRedisTemplate redisTemplate;
    private final ConfigService configService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${budgetpilot.telegram.bot-token:}")
    private String defaultBotToken;

    @Value("${budgetpilot.telegram.chat-id:}")
    private String defaultChatId;

    private static final String API_URL = "https://api.telegram.org/bot%s/sendMessage";
    private static final String DEDUP_PREFIX = "telegram:dedup:";

    /**
     * 发送 Telegram 消息
     */
    public boolean send(String title, String content) {
        String token = resolveConfig("telegram_bot_token", defaultBotToken);
        String chatId = resolveConfig("telegram_chat_id", defaultChatId);

        if (token == null || token.isBlank() || chatId == null || chatId.isBlank()) {
            log.warn("Telegram 未配置，跳过推送");
            return false;
        }

        try {
            String url = String.format(API_URL, token);
            String escapedContent = escapeMarkdownV2(content);
            Map<String, Object> body = Map.of(
                    "chat_id", chatId,
                    "text", escapedContent,
                    "parse_mode", "MarkdownV2"
            );
            restTemplate.postForEntity(url, body, String.class);
            return true;
        } catch (Exception e) {
            log.error("Telegram 推送失败", e);
            return false;
        }
    }

    /**
     * 转义 MarkdownV2 特殊字符
     * MarkdownV2 需要转义: _ * [ ] ( ) ~ > # + - = | { } . !
     * 注意: 换行符不需要转义，但需要在段落间保留
     */
    private String escapeMarkdownV2(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // MarkdownV2 需要转义的特殊字符
            if ("_*[]()~>#+-=|{}.!".indexOf(c) >= 0) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 格式化消息内容为美观的 Telegram 消息
     */
    public String formatMessage(String emoji, String title, List<String> lines) {
        StringBuilder sb = new StringBuilder();
        sb.append(emoji).append(" ").append(title).append("\n\n");
        for (String line : lines) {
            sb.append("• ").append(line).append("\n");
        }
        sb.append("\n━━━━━━━━━━━━━\n");
        sb.append("_").append(java.time.LocalDate.now()).append("_");
        return sb.toString();
    }

    /**
     * 发送并记录去重
     */
    public boolean sendWithDedup(String ruleId, String title, String content) {
        String dedupKey = DEDUP_PREFIX + ruleId + ":" + java.time.LocalDate.now();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(dedupKey))) {
            log.debug("Telegram dedup hit for rule {} today", ruleId);
            return false;
        }

        boolean sent = send(title, content);
        if (sent) {
            redisTemplate.opsForValue().set(dedupKey, "1", 24, TimeUnit.HOURS);
        }
        return sent;
    }

    /**
     * 测试推送
     */
    public boolean testSend() {
        String content = String.format("""
                🔧 BudgetPilot 测试

                Telegram 推送配置成功！

                • 当前时间: %s
                • 服务状态: 正常

                ━━━━━━━━━━━━━━
                _%s_
                """,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                java.time.LocalDate.now());
        return send("BudgetPilot Test", content);
    }

    private String resolveConfig(String key, String defaultValue) {
        String fromConfig = configService.get(key);
        if (fromConfig != null && !fromConfig.isBlank()) {
            return fromConfig;
        }
        return defaultValue;
    }
}
