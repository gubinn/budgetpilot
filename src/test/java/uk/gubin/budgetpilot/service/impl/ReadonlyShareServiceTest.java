package uk.gubin.budgetpilot.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.common.TenantContext;
import uk.gubin.budgetpilot.dto.ReadonlyShareCreateDTO;
import uk.gubin.budgetpilot.entity.ReadonlyShare;
import uk.gubin.budgetpilot.entity.User;
import uk.gubin.budgetpilot.mapper.ReadonlyShareMapper;
import uk.gubin.budgetpilot.mapper.UserMapper;
import uk.gubin.budgetpilot.service.ReadonlySessionManager;
import uk.gubin.budgetpilot.vo.ReadonlySessionVO;
import uk.gubin.budgetpilot.vo.ReadonlyShareCreatedVO;
import uk.gubin.budgetpilot.vo.ShareCheckVO;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReadonlyShareServiceTest {

    @Mock private ReadonlyShareMapper shareMapper;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ReadonlySessionManager sessionManager;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private ReadonlyShareServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContext.set(7L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        service = new ReadonlyShareServiceImpl(
                shareMapper, userMapper, passwordEncoder, sessionManager, redisTemplate);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("创建分享：只保存分享码哈希和 PIN 哈希，明文只返回一次")
    void createStoresHashesOnly() {
        when(passwordEncoder.encode("1234")).thenReturn("pin-hash");
        when(shareMapper.insert(any(ReadonlyShare.class))).thenAnswer(invocation -> {
            ReadonlyShare share = invocation.getArgument(0);
            share.setId(11L);
            return 1;
        });

        ReadonlyShareCreateDTO dto = new ReadonlyShareCreateDTO();
        dto.setLabel("给媳妇看");
        dto.setExpiresInDays(7);
        dto.setPin("1234");

        ReadonlyShareCreatedVO created = service.create(dto);

        assertThat(created.getCode()).isNotBlank();
        assertThat(created.getPath()).isEqualTo("/share/" + created.getCode());
        assertThat(created.getPin()).isEqualTo("1234");

        ArgumentCaptor<ReadonlyShare> captor = ArgumentCaptor.forClass(ReadonlyShare.class);
        verify(shareMapper).insert(captor.capture());
        ReadonlyShare saved = captor.getValue();
        assertThat(saved.getCodeHash()).hasSize(64);
        assertThat(saved.getCodeHash()).isNotEqualTo(created.getCode());
        assertThat(saved.getCodeHint()).isEqualTo(created.getCode().substring(created.getCode().length() - 4));
        assertThat(saved.getPinHash()).isEqualTo("pin-hash");
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(6));
    }

    @Test
    @DisplayName("检查有效的免 PIN 分享")
    void checkActiveShareWithoutPin() {
        ReadonlyShare share = share(1L, 2L, null, LocalDateTime.now().plusDays(1));
        when(shareMapper.selectByCodeHash(anyString())).thenReturn(share);

        ShareCheckVO check = service.check("some-code");

        assertThat(check.isValid()).isTrue();
        assertThat(check.isNeedsPin()).isFalse();
        assertThat(check.getLabel()).isEqualTo("给媳妇看");
    }

    @Test
    @DisplayName("过期或撤销的分享检查为无效")
    void checkRejectsExpiredAndRevoked() {
        ReadonlyShare expired = share(1L, 2L, null, LocalDateTime.now().minusMinutes(1));
        when(shareMapper.selectByCodeHash(anyString())).thenReturn(expired);
        assertThat(service.check("code").isValid()).isFalse();

        ReadonlyShare revoked = share(1L, 2L, null, LocalDateTime.now().plusDays(1));
        revoked.setRevokedAt(LocalDateTime.now());
        when(shareMapper.selectByCodeHash(anyString())).thenReturn(revoked);
        assertThat(service.check("code").isValid()).isFalse();
    }

    @Test
    @DisplayName("验证通过后创建只读会话并记录访问")
    void verifyCreatesReadonlySession() {
        ReadonlyShare share = share(5L, 2L, null, LocalDateTime.now().plusDays(1));
        when(shareMapper.selectByCodeHash(anyString())).thenReturn(share);
        User owner = new User();
        owner.setId(2L);
        owner.setUsername("owner");
        owner.setNickname("顾斌");
        owner.setIsActive(true);
        when(userMapper.selectById(2L)).thenReturn(owner);
        when(sessionManager.createSession(eq(2L), eq(5L), anyLong())).thenReturn("ro_5_abc");

        ReadonlySessionVO session = service.verify("code", null, "1.2.3.4");

        assertThat(session.getToken()).isEqualTo("ro_5_abc");
        assertThat(session.getOwnerName()).isEqualTo("顾斌");
        assertThat(session.isReadOnly()).isTrue();
        verify(shareMapper).touch(eq(5L), any(LocalDateTime.class), eq("1.2.3.4"));
    }

    @Test
    @DisplayName("PIN 错误时拒绝，并累计失败次数")
    void verifyRejectsWrongPin() {
        ReadonlyShare share = share(5L, 2L, "pin-hash", LocalDateTime.now().plusDays(1));
        when(shareMapper.selectByCodeHash(anyString())).thenReturn(share);
        when(passwordEncoder.matches("0000", "pin-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.verify("code", "0000", "1.2.3.4"))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SHARE_PIN_WRONG));
        verify(valueOperations, atLeastOnce()).increment(anyString());
    }

    @Test
    @DisplayName("PIN 尝试次数过多时直接拒绝")
    void verifyRejectsTooManyPinAttempts() {
        ReadonlyShare share = share(5L, 2L, "pin-hash", LocalDateTime.now().plusDays(1));
        when(shareMapper.selectByCodeHash(anyString())).thenReturn(share);
        when(valueOperations.get(anyString())).thenReturn("5");

        assertThatThrownBy(() -> service.verify("code", "1234", "1.2.3.4"))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SHARE_TOO_MANY_ATTEMPTS));
    }

    @Test
    @DisplayName("撤销分享时踢掉该分享的只读会话")
    void revokeKicksTokens() {
        ReadonlyShare share = share(5L, 2L, null, LocalDateTime.now().plusDays(1));
        when(shareMapper.selectByIdAndUserId(5L, 7L)).thenReturn(share);

        service.revoke(5L);

        verify(shareMapper).revoke(5L, 7L);
        verify(sessionManager).revokeTokens(eq(5L), anyLong());
    }

    @Test
    @DisplayName("删除已撤销或已过期的分享记录")
    void deleteRemovesInactiveShare() {
        ReadonlyShare share = share(5L, 2L, null, LocalDateTime.now().minusMinutes(1));
        when(shareMapper.selectByIdAndUserId(5L, 7L)).thenReturn(share);

        service.delete(5L);

        verify(shareMapper).deleteByIdAndUserId(5L, 7L);
    }

    @Test
    @DisplayName("有效链接必须先撤销才能删除记录")
    void deleteRejectsActiveShare() {
        ReadonlyShare share = share(5L, 2L, null, LocalDateTime.now().plusDays(1));
        when(shareMapper.selectByIdAndUserId(5L, 7L)).thenReturn(share);

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PARAM_ERROR));
        verify(shareMapper, org.mockito.Mockito.never()).deleteByIdAndUserId(anyLong(), anyLong());
    }

    private ReadonlyShare share(Long id, Long ownerId, String pinHash, LocalDateTime expiresAt) {
        ReadonlyShare share = new ReadonlyShare();
        share.setId(id);
        share.setUserId(ownerId);
        share.setLabel("给媳妇看");
        share.setPinHash(pinHash);
        share.setExpiresAt(expiresAt);
        share.setViewCount(0);
        return share;
    }
}