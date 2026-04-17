package uk.gubin.budgetpilot.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.entity.CurrencyRate;
import uk.gubin.budgetpilot.mapper.CurrencyRateMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrencyRateServiceTest {

    @Mock private CurrencyRateMapper currencyRateMapper;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private CurrencyRateServiceImpl currencyRateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(currencyRateService, "baseCurrency", "CNY");
        ReflectionTestUtils.setField(currencyRateService, "supportedCurrencies", "CNY,USD,EUR,JPY");
        ReflectionTestUtils.setField(currencyRateService, "apiKey", "test-api-key");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ============ GET RATE ============

    @Nested
    @DisplayName("获取汇率")
    class GetRateTests {

        @Test
        @DisplayName("本位币直接返回 1")
        void getRate_BaseCurrency_ReturnsOne() {
            BigDecimal rate = currencyRateService.getRate("CNY", LocalDate.now());
            assertThat(rate).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("Redis 缓存命中")
        void getRate_CacheHit() {
            when(valueOps.get("rate:USD:2026-04-15")).thenReturn("7.2");

            BigDecimal rate = currencyRateService.getRate("USD", LocalDate.of(2026, 4, 15));

            assertThat(rate).isEqualByComparingTo(new BigDecimal("7.2"));
            // 不应查询 DB
            verify(currencyRateMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("MySQL 命中")
        void getRate_DbHit() {
            when(valueOps.get(anyString())).thenReturn(null);

            CurrencyRate rateEntity = new CurrencyRate();
            rateEntity.setRate(new BigDecimal("7.15"));
            when(currencyRateMapper.selectOne(any())).thenReturn(rateEntity);

            BigDecimal rate = currencyRateService.getRate("USD", LocalDate.of(2026, 4, 15));

            assertThat(rate).isEqualByComparingTo(new BigDecimal("7.15"));
            // 写回 Redis
            verify(valueOps).set(eq("rate:USD:2026-04-15"), eq("7.15"), eq(25L), any());
        }

        @Test
        @DisplayName("降级：使用 3 天内最近汇率")
        void getRate_FallbackToNearestRate() {
            when(valueOps.get(anyString())).thenReturn(null);
            when(currencyRateMapper.selectOne(any()))
                    .thenReturn(null) // today
                    .thenReturn(null) // -1 day
                    .thenAnswer(inv -> {
                        CurrencyRate r = new CurrencyRate();
                        r.setRate(new BigDecimal("7.10"));
                        return r;
                    }); // -2 days

            BigDecimal rate = currencyRateService.getRate("USD", LocalDate.of(2026, 4, 15));

            assertThat(rate).isEqualByComparingTo(new BigDecimal("7.10"));
        }

        @Test
        @DisplayName("3 天内无数据抛异常")
        void getRate_NoData_ThrowsException() {
            when(valueOps.get(anyString())).thenReturn(null);
            when(currencyRateMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() ->
                    currencyRateService.getRate("USD", LocalDate.of(2026, 4, 15)))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("汇率获取失败");
        }

        @Test
        @DisplayName("不支持的币种抛异常")
        void getRate_UnsupportedCurrency() {
            assertThatThrownBy(() ->
                    currencyRateService.getRate("BTC", LocalDate.now()))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("币种不支持");
        }
    }

    // ============ CONVERT ============

    @Nested
    @DisplayName("币种转换")
    class ConvertTests {

        @Test
        @DisplayName("本位币转换不变")
        void convertToBase_BaseCurrency() {
            BigDecimal result = currencyRateService.convertToBase(
                    new BigDecimal("100"), "CNY", LocalDate.now());
            assertThat(result).isEqualByComparingTo(new BigDecimal("100"));
        }

        @Test
        @DisplayName("外币正确转换")
        void convertToBase_ForeignCurrency() {
            when(valueOps.get(anyString())).thenReturn(null);
            when(currencyRateMapper.selectOne(any())).thenAnswer(inv -> {
                CurrencyRate r = new CurrencyRate();
                r.setRate(new BigDecimal("7.2"));
                return r;
            });

            BigDecimal result = currencyRateService.convertToBase(
                    new BigDecimal("100"), "USD", LocalDate.of(2026, 4, 15));

            assertThat(result).isEqualByComparingTo(new BigDecimal("720.00"));
        }
    }

    // ============ CLEANUP ============

    @Nested
    @DisplayName("汇率数据清理")
    class CleanupTests {

        @Test
        @DisplayName("清理 6 个月前的数据")
        void cleanupOldRates() {
            when(currencyRateMapper.delete(any())).thenReturn(10);

            currencyRateService.cleanupOldRates(6);

            verify(currencyRateMapper).delete(any());
        }
    }
}
