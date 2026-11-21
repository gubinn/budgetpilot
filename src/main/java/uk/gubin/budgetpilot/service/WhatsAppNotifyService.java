package uk.gubin.budgetpilot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WhatsApp 通知服务 - 通过 WhatsApp Agent Platform 官方 API 推送消息。
 *
 * <p>该 API 只允许向 Agent 创建者本人发送消息，适合个人记账提醒；
 * 出站频率限制为每个 Agent 每分钟 12 条。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppNotifyService {

    private static final String DEFAULT_BASE_URL = "https://api.whatsapp.com/agent/v1";
    private static final int MAX_BODY_LENGTH = 4000;

    private final ConfigService configService;
    private final RestTemplate restTemplate;

    @Value("${budgetpilot.whatsapp.api-key:}")
    private String defaultApiKey;

    @Value("${budgetpilot.whatsapp.to:}")
    private String defaultTo;

    @Value("${budgetpilot.whatsapp.base-url:https://api.whatsapp.com/agent/v1}")
    private String defaultBaseUrl;

    /**
     * 发送 WhatsApp 文本消息
     */
    public boolean send(String title, String content) {
        String apiKey = resolveConfig("whatsapp_api_key", defaultApiKey);
        String to = resolveConfig("whatsapp_to", defaultTo);
        String baseUrl = resolveConfig("whatsapp_base_url", defaultBaseUrl);

        if (apiKey == null || apiKey.isBlank() || to == null || to.isBlank()) {
            log.warn("WhatsApp 未配置，跳过推送");
            return false;
        }

        try {
            String url = baseUrl.replaceAll("/+$", "") + "/messages";

            Map<String, Object> text = new LinkedHashMap<>();
            text.put("body", truncate(content));
            text.put("preview_url", false);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("messaging_product", "whatsapp");
            body.put("to", normalizeRecipient(to));
            body.put("type", "text");
            body.put("text", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            return true;
        } catch (Exception e) {
            log.error("WhatsApp 推送失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 测试推送
     */
    public boolean testSend() {
        String content = String.format("""
                🔧 BudgetPilot 测试

                WhatsApp 推送配置成功！

                • 当前时间: %s
                • 服务状态: 正常

                ━━━━━━━━━━━━━━
                %s
                """,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                LocalDate.now());
        return send("BudgetPilot Test", content);
    }

    /**
     * Agent Platform 的接收者格式为 {@code user:<id>}，纯数字 ID 自动补全前缀。
     */
    private String normalizeRecipient(String to) {
        String trimmed = to.trim();
        return trimmed.contains(":") ? trimmed : "user:" + trimmed;
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() <= MAX_BODY_LENGTH ? text : text.substring(0, MAX_BODY_LENGTH - 1) + "…";
    }

    private String resolveConfig(String key, String defaultValue) {
        String fromConfig = configService.get(key);
        if (fromConfig != null && !fromConfig.isBlank()) {
            return fromConfig;
        }
        return defaultValue;
    }
}