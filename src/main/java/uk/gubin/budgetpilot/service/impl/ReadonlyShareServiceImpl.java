package uk.gubin.budgetpilot.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.common.TenantContext;
import uk.gubin.budgetpilot.dto.ReadonlyShareCreateDTO;
import uk.gubin.budgetpilot.entity.ReadonlyShare;
import uk.gubin.budgetpilot.entity.User;
import uk.gubin.budgetpilot.mapper.ReadonlyShareMapper;
import uk.gubin.budgetpilot.mapper.UserMapper;
import uk.gubin.budgetpilot.service.ReadonlySessionManager;
import uk.gubin.budgetpilot.service.ReadonlyShareService;
import uk.gubin.budgetpilot.vo.ReadonlySessionVO;
import uk.gubin.budgetpilot.vo.ReadonlyShareCreatedVO;
import uk.gubin.budgetpilot.vo.ReadonlyShareVO;
import uk.gubin.budgetpilot.vo.ShareCheckVO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 只读共享实现。
 *
 * <p>分享码只存 SHA-256；PIN 用 BCrypt。只读会话由 ReadonlySessionManager
 * 创建带 {@code ro_} 前缀的令牌，过滤器和前端都不依赖“藏按钮”。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReadonlyShareServiceImpl implements ReadonlyShareService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long SESSION_TTL_SECONDS = 12 * 60 * 60;
    private static final int PIN_MAX_ATTEMPTS = 5;
    private static final long PIN_FAIL_TTL_SECONDS = 15 * 60;
    private static final int VERIFY_IP_MAX = 30;
    private static final long VERIFY_IP_TTL_SECONDS = 10 * 60;

    private static final String PIN_FAIL_KEY = "readonly:pin:fail:%d:%s";
    private static final String VERIFY_IP_KEY = "readonly:verify:ip:%s";

    private final ReadonlyShareMapper shareMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ReadonlySessionManager sessionManager;
    private final StringRedisTemplate redisTemplate;

    @Override
    public List<ReadonlyShareVO> list() {
        Long ownerId = currentUserId();
        LocalDateTime now = LocalDateTime.now();
        return shareMapper.selectByUserId(ownerId).stream()
                .map(share -> toVO(share, now))
                .toList();
    }

    @Override
    @Transactional
    public ReadonlyShareCreatedVO create(ReadonlyShareCreateDTO dto) {
        Long ownerId = currentUserId();
        String label = dto.getLabel() == null || dto.getLabel().isBlank()
                ? "只读分享" : dto.getLabel().trim();
        String pin = dto.getPin() == null ? "" : dto.getPin().trim();
        int days = dto.getExpiresInDays() != null ? dto.getExpiresInDays() : 7;

        String code = newCode();
        ReadonlyShare share = new ReadonlyShare();
        share.setUserId(ownerId);
        share.setLabel(label);
        share.setCodeHash(sha256(code));
        share.setCodeHint(code.substring(code.length() - 4));
        share.setPinHash(pin.isEmpty() ? null : passwordEncoder.encode(pin));
        share.setExpiresAt(LocalDateTime.now().plusDays(days));
        share.setViewCount(0);
        shareMapper.insert(share);

        ReadonlyShareCreatedVO vo = new ReadonlyShareCreatedVO();
        vo.setId(share.getId());
        vo.setLabel(label);
        vo.setCode(code);
        vo.setPath("/share/" + code);
        vo.setPin(pin.isEmpty() ? null : pin);
        vo.setExpiresAt(share.getExpiresAt());
        return vo;
    }

    @Override
    @Transactional
    public void revoke(Long id) {
        Long ownerId = currentUserId();
        ReadonlyShare share = shareMapper.selectByIdAndUserId(id, ownerId);
        if (share == null) {
            throw new BizException(ErrorCode.SHARE_NOT_FOUND);
        }
        if (share.getRevokedAt() != null) {
            return;
        }
        shareMapper.revoke(id, ownerId);

        long keepSeconds = Math.max(60,
                Duration.between(LocalDateTime.now(), share.getExpiresAt()).getSeconds());
        sessionManager.revokeTokens(id, keepSeconds);
    }

    @Override
    public ShareCheckVO check(String code) {
        ShareCheckVO vo = new ShareCheckVO();
        ReadonlyShare share = findUsable(code);
        if (share == null) {
            vo.setValid(false);
            return vo;
        }
        vo.setValid(true);
        vo.setNeedsPin(share.getPinHash() != null);
        vo.setLabel(share.getLabel());
        vo.setExpiresAt(share.getExpiresAt());
        return vo;
    }

    @Override
    public ReadonlySessionVO verify(String code, String pin, String clientIp) {
        if (tooManyVerifies(clientIp)) {
            throw new BizException(ErrorCode.SHARE_TOO_MANY_ATTEMPTS);
        }

        ReadonlyShare share = findUsable(code);
        if (share == null) {
            throw new BizException(ErrorCode.SHARE_LINK_INVALID);
        }

        if (share.getPinHash() != null) {
            String input = pin == null ? "" : pin.trim();
            if (input.isEmpty()) {
                throw new BizException(ErrorCode.SHARE_PIN_REQUIRED);
            }
            if (pinFailCount(share.getId(), clientIp) >= PIN_MAX_ATTEMPTS) {
                throw new BizException(ErrorCode.SHARE_TOO_MANY_ATTEMPTS);
            }
            if (!passwordEncoder.matches(input, share.getPinHash())) {
                recordPinFail(share.getId(), clientIp);
                throw new BizException(ErrorCode.SHARE_PIN_WRONG);
            }
            clearPinFail(share.getId(), clientIp);
        }

        User owner = userMapper.selectById(share.getUserId());
        if (owner == null || !Boolean.TRUE.equals(owner.getIsActive())) {
            throw new BizException(ErrorCode.SHARE_LINK_INVALID);
        }

        LocalDateTime now = LocalDateTime.now();
        long secondsLeft = Duration.between(now, share.getExpiresAt()).getSeconds();
        if (secondsLeft < 60) {
            throw new BizException(ErrorCode.SHARE_LINK_INVALID);
        }
        long ttl = Math.min(SESSION_TTL_SECONDS, secondsLeft);

        String token = sessionManager.createSession(owner.getId(), share.getId(), ttl);
        shareMapper.touch(share.getId(), now, truncateIp(clientIp));

        ReadonlySessionVO vo = new ReadonlySessionVO();
        vo.setToken(token);
        vo.setOwnerName(displayName(owner));
        vo.setShareId(share.getId());
        vo.setExpiresAt(now.plusSeconds(ttl));
        vo.setReadOnly(true);
        return vo;
    }

    private ReadonlyShare findUsable(String code) {
        String raw = code == null ? "" : code.trim();
        if (raw.isEmpty()) {
            return null;
        }
        ReadonlyShare share = shareMapper.selectByCodeHash(sha256(raw));
        if (share == null || share.getRevokedAt() != null
                || !share.getExpiresAt().isAfter(LocalDateTime.now())) {
            return null;
        }
        return share;
    }

    private ReadonlyShareVO toVO(ReadonlyShare share, LocalDateTime now) {
        ReadonlyShareVO vo = new ReadonlyShareVO();
        vo.setId(share.getId());
        vo.setLabel(share.getLabel());
        vo.setCodeHint(share.getCodeHint());
        vo.setPinRequired(share.getPinHash() != null);
        vo.setExpiresAt(share.getExpiresAt());
        vo.setRevokedAt(share.getRevokedAt());
        vo.setLastUsedAt(share.getLastUsedAt());
        vo.setLastUsedIp(share.getLastUsedIp());
        vo.setViewCount(share.getViewCount() == null ? 0 : share.getViewCount());
        vo.setCreatedAt(share.getCreatedAt());

        if (share.getRevokedAt() != null) {
            vo.setStatus("REVOKED");
            vo.setStatusName("已撤销");
        } else if (!share.getExpiresAt().isAfter(now)) {
            vo.setStatus("EXPIRED");
            vo.setStatusName("已过期");
        } else {
            vo.setStatus("ACTIVE");
            vo.setStatusName("有效");
        }
        return vo;
    }

    private boolean tooManyVerifies(String clientIp) {
        String key = String.format(VERIFY_IP_KEY, safeIp(clientIp));
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, VERIFY_IP_TTL_SECONDS, TimeUnit.SECONDS);
            }
            return count != null && count > VERIFY_IP_MAX;
        } catch (Exception e) {
            log.warn("Failed to check share verify rate limit for ip {}", clientIp, e);
            return false;
        }
    }

    private long pinFailCount(Long shareId, String clientIp) {
        try {
            String value = redisTemplate.opsForValue()
                    .get(String.format(PIN_FAIL_KEY, shareId, safeIp(clientIp)));
            return value == null ? 0 : Long.parseLong(value);
        } catch (Exception e) {
            log.warn("Failed to read share pin fail count for share {}", shareId, e);
            return 0;
        }
    }

    private void recordPinFail(Long shareId, String clientIp) {
        String key = String.format(PIN_FAIL_KEY, shareId, safeIp(clientIp));
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, PIN_FAIL_TTL_SECONDS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("Failed to record share pin fail for share {}", shareId, e);
        }
    }

    private void clearPinFail(Long shareId, String clientIp) {
        try {
            redisTemplate.delete(String.format(PIN_FAIL_KEY, shareId, safeIp(clientIp)));
        } catch (Exception e) {
            log.debug("Failed to clear share pin fail for share {}", shareId, e);
        }
    }

    private Long currentUserId() {
        Long tenant = TenantContext.get();
        if (tenant != null) {
            return tenant;
        }
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            throw new BizException(ErrorCode.AUTH_NOT_LOGGED_IN);
        }
    }

    private String newCode() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String displayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    private String safeIp(String clientIp) {
        return clientIp == null || clientIp.isBlank() ? "unknown" : clientIp;
    }

    private String truncateIp(String clientIp) {
        String ip = safeIp(clientIp);
        return ip.length() > 64 ? ip.substring(0, 64) : ip;
    }
}