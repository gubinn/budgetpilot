package uk.gubin.budgetpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.entity.CurrencyRate;
import uk.gubin.budgetpilot.mapper.CurrencyRateMapper;
import uk.gubin.budgetpilot.service.CurrencyRateService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyRateServiceImpl extends ServiceImpl<CurrencyRateMapper, CurrencyRate> implements CurrencyRateService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${budgetpilot.base-currency:CNY}")
    private String baseCurrency;

    @Value("${budgetpilot.supported-currencies:CNY,USD,EUR,GBP,JPY,HKD,SGD,THB,KRW}")
    private String supportedCurrencies;

    @Value("${budgetpilot.exchange-rate-api.key:}")
    private String apiKey;

    @Value("${budgetpilot.exchange-rate-api.url:https://v6.exchangerate-api.com/v6}")
    private String apiUrl;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String REDIS_KEY_PREFIX = "rate:";
    private static final int FALLBACK_DAYS = 3;

    @Override
    public BigDecimal getRate(String currency, LocalDate date) {
        if (baseCurrency.equals(currency)) {
            return BigDecimal.ONE;
        }

        validateCurrency(currency);

        // 1. Redis cache
        String redisKey = REDIS_KEY_PREFIX + currency + ":" + date.format(DATE_FMT);
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            log.debug("Cache hit for rate: {} on {}", currency, date);
            return new BigDecimal(cached);
        }

        // 2. MySQL
        LambdaQueryWrapper<CurrencyRate> query = new LambdaQueryWrapper<>();
        query.eq(CurrencyRate::getBaseCurrency, baseCurrency)
                .eq(CurrencyRate::getTargetCurrency, currency)
                .eq(CurrencyRate::getRateDate, date);
        CurrencyRate rate = baseMapper.selectOne(query);
        if (rate != null) {
            redisTemplate.opsForValue().set(redisKey, rate.getRate().toString(), 25, TimeUnit.HOURS);
            log.debug("DB hit for rate: {} on {}", currency, date);
            return rate.getRate();
        }

        // 3. Fetch from API
        try {
            BigDecimal fetchedRate = fetchRateFromApi(currency, date);
            if (fetchedRate != null) {
                saveRate(currency, date, fetchedRate);
                redisTemplate.opsForValue().set(redisKey, fetchedRate.toString(), 25, TimeUnit.HOURS);
                return fetchedRate;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch rate from API for {} on {}", currency, date, e);
        }

        // 4. Fallback: get nearest rate within 3 days
        BigDecimal fallback = getNearestRate(currency, date);
        if (fallback != null) {
            log.error("[ALERT] Using fallback rate for {} on {} — API data missing for {} days, rate={}. "
                    + "This may cause >5%% error for volatile currencies.",
                    currency, date, FALLBACK_DAYS, fallback);
            return fallback;
        }

        // All sources exhausted — silent fallback to 1:1
        log.warn("Unable to get exchange rate for {} on {}, defaulting to 1:1", currency, date);
        return BigDecimal.ONE;
    }

    @Override
    public BigDecimal convertToBase(BigDecimal amount, String currency, LocalDate date) {
        if (baseCurrency.equals(currency)) {
            return amount;
        }
        BigDecimal rate = getRate(currency, date);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void refreshRates() {
        log.info("Starting rate refresh from ExchangeRate-API");
        String[] currencies = supportedCurrencies.split(",");

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Exchange Rate API key not configured, skipping refresh");
            return;
        }

        try {
            String url = String.format("%s/%s/latest/%s", apiUrl, apiKey, baseCurrency);
            String response = cn.hutool.http.HttpUtil.get(url);
            JsonNode root = objectMapper.readTree(response);

            if (!"success".equals(root.path("result").asText()) &&
                    !"success".equals(root.path("response").path("result").asText())) {
                log.error("API response not success: {}", response);
                return;
            }

            // API returns rates as base -> target (how many target per 1 base)
            // We need target -> base (how many base per 1 target)
            // The API returns: 1 CNY = X USD, etc.
            // We need: 1 USD = Y CNY
            JsonNode conversions = root.path("conversion_rates");
            LocalDate today = LocalDate.now();

            for (String target : currencies) {
                if (baseCurrency.equals(target)) continue;
                if (!conversions.has(target)) continue;

                // API gives us 1 CNY = rate USD, we need 1 USD = ? CNY
                // rate = 0.14 means 1 CNY = 0.14 USD, so 1 USD = 1/0.14 CNY = 7.14 CNY
                double apiRate = conversions.path(target).asDouble();
                if (apiRate > 0) {
                    BigDecimal ourRate = BigDecimal.ONE.divide(BigDecimal.valueOf(apiRate), 6, RoundingMode.HALF_UP);
                    saveRate(target, today, ourRate);
                    String redisKey = REDIS_KEY_PREFIX + target + ":" + today.format(DATE_FMT);
                    redisTemplate.opsForValue().set(redisKey, ourRate.toString(), 25, TimeUnit.HOURS);
                }
            }

            log.info("Rate refresh completed successfully");
        } catch (Exception e) {
            log.error("Rate refresh failed", e);
        }
    }

    @Override
    @Transactional
    public CurrencyRate getOrCreateRate(String currency, LocalDate date) {
        BigDecimal rate = getRate(currency, date);
        LambdaQueryWrapper<CurrencyRate> query = new LambdaQueryWrapper<>();
        query.eq(CurrencyRate::getBaseCurrency, baseCurrency)
                .eq(CurrencyRate::getTargetCurrency, currency)
                .eq(CurrencyRate::getRateDate, date);
        CurrencyRate existing = baseMapper.selectOne(query);
        return existing != null ? existing : saveRate(currency, date, rate);
    }

    @Override
    @Transactional
    public void cleanupOldRates(int monthsBefore) {
        LocalDate cutoff = LocalDate.now().minusMonths(monthsBefore);
        LambdaQueryWrapper<CurrencyRate> query = new LambdaQueryWrapper<>();
        query.lt(CurrencyRate::getRateDate, cutoff);
        int deleted = baseMapper.delete(query);
        log.info("Cleaned up {} old rate records before {}", deleted, cutoff);
    }

    @Transactional
    protected CurrencyRate saveRate(String currency, LocalDate date, BigDecimal rate) {
        LambdaQueryWrapper<CurrencyRate> query = new LambdaQueryWrapper<>();
        query.eq(CurrencyRate::getBaseCurrency, baseCurrency)
                .eq(CurrencyRate::getTargetCurrency, currency)
                .eq(CurrencyRate::getRateDate, date);
        CurrencyRate existing = baseMapper.selectOne(query);

        if (existing != null) {
            existing.setRate(rate);
            baseMapper.updateById(existing);
            return existing;
        }

        CurrencyRate newRate = new CurrencyRate();
        newRate.setBaseCurrency(baseCurrency);
        newRate.setTargetCurrency(currency);
        newRate.setRate(rate);
        newRate.setRateDate(date);
        newRate.setSource("exchangerate-api");
        baseMapper.insert(newRate);
        return newRate;
    }

    private BigDecimal getNearestRate(String currency, LocalDate date) {
        for (int i = 1; i <= FALLBACK_DAYS; i++) {
            LocalDate tryDate = date.minusDays(i);
            LambdaQueryWrapper<CurrencyRate> query = new LambdaQueryWrapper<>();
            query.eq(CurrencyRate::getBaseCurrency, baseCurrency)
                    .eq(CurrencyRate::getTargetCurrency, currency)
                    .eq(CurrencyRate::getRateDate, tryDate)
                    .orderByDesc(CurrencyRate::getRateDate)
                    .last("LIMIT 1");
            CurrencyRate rate = baseMapper.selectOne(query);
            if (rate != null) {
                return rate.getRate();
            }
        }
        return null;
    }

    private BigDecimal fetchRateFromApi(String currency, LocalDate date) throws com.fasterxml.jackson.core.JsonProcessingException {
        String url = String.format("%s/%s/latest/%s", apiUrl, apiKey, baseCurrency);
        String response = cn.hutool.http.HttpUtil.get(url);
        JsonNode root = objectMapper.readTree(response);
        JsonNode conversions = root.path("conversion_rates");
        if (conversions.has(currency)) {
            double apiRate = conversions.path(currency).asDouble();
            if (apiRate > 0) {
                return BigDecimal.ONE.divide(BigDecimal.valueOf(apiRate), 6, RoundingMode.HALF_UP);
            }
        }
        return null;
    }

    private void validateCurrency(String currency) {
        String[] currencies = supportedCurrencies.split(",");
        for (String c : currencies) {
            if (c.equals(currency)) return;
        }
        throw new BizException(ErrorCode.CURRENCY_UNSUPPORTED, currency);
    }
}
