package uk.gubin.budgetpilot.service;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 只读共享会话：用 Sa-Token 以账本主人身份登录，但令牌带 {@code ro_} 前缀，
 * 服务端过滤器据此无条件拒绝写请求。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReadonlySessionManager {

    public static final String TOKEN_PREFIX = "ro_";
    public static final String SHARE_TOKENS_KEY = "readonly:share:%d:tokens";
    public static final String SHARE_REVOKED_KEY = "readonly:share:%d:revoked";
    /** 令牌过期后多保留 5 分钟，避免时钟误差造成标记先消失 */
    private static final long MARKER_BUFFER_SECONDS = 300;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    /**
     * @return 只读令牌
     */
    public String createSession(Long ownerId, Long shareId, long ttlSeconds) {
        String random = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(randomBytes(32));
        String token = TOKEN_PREFIX + shareId + "_" + random;

        SaLoginModel model = new SaLoginModel()
                .setToken(token)
                .setTimeout(ttlSeconds)
                .setActiveTimeout(ttlSeconds);
        StpUtil.login(ownerId, model);

        try {
            String setKey = String.format(SHARE_TOKENS_KEY, shareId);
            redisTemplate.opsForSet().add(setKey, token);
            redisTemplate.expire(setKey, ttlSeconds + MARKER_BUFFER_SECONDS, TimeUnit.SECONDS);
            StpUtil.getTokenSession().set("readonlyShareId", shareId);
        } catch (Exception e) {
            log.error("Failed to register readonly share token for share {}", shareId, e);
            try {
                StpUtil.logout();
            } catch (Exception ignored) {
                // 清理失败不影响拒绝访问
            }
            throw new BizException(ErrorCode.SYSTEM_ERROR, "创建只读会话失败");
        }
        return token;
    }

    /**
     * 撤销一个分享下的所有只读会话。
     *
     * <p>先标记 revoked，再尝试踢人；即使踢人失败，过滤器看到 revoked
     * 也会拒绝，不会退化成可写会话。</p>
     */
    public void revokeTokens(Long shareId, long keepSeconds) {
        String revokedKey = String.format(SHARE_REVOKED_KEY, shareId);
        try {
            redisTemplate.opsForValue().set(revokedKey, "1",
                    Math.max(60, keepSeconds), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to mark readonly share {} revoked in redis", shareId, e);
        }

        String setKey = String.format(SHARE_TOKENS_KEY, shareId);
        Set<String> tokens;
        try {
            tokens = redisTemplate.opsForSet().members(setKey);
        } catch (Exception e) {
            log.warn("Failed to read readonly share tokens for share {}", shareId, e);
            return;
        }
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        for (String token : tokens) {
            try {
                StpUtil.kickoutByTokenValue(token);
            } catch (Exception e) {
                log.debug("Failed to kick readonly token for share {}", shareId, e);
            }
        }
        try {
            redisTemplate.delete(setKey);
        } catch (Exception e) {
            log.debug("Failed to delete readonly token set for share {}", shareId, e);
        }
    }

    private byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}