package uk.gubin.budgetpilot.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.entity.AlertLog;
import uk.gubin.budgetpilot.entity.CurrencyRate;
import uk.gubin.budgetpilot.mapper.*;
import uk.gubin.budgetpilot.service.ConfigService;
import uk.gubin.budgetpilot.service.CurrencyRateService;
import uk.gubin.budgetpilot.service.AlertLogService;
import uk.gubin.budgetpilot.service.TelegramNotifyService;
import uk.gubin.budgetpilot.service.WhatsAppNotifyService;
import uk.gubin.budgetpilot.service.ScheduledTasks;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final ConfigService configService;
    private final CurrencyRateService currencyRateService;
    private final AlertLogService alertLogService;
    private final TelegramNotifyService telegramNotifyService;
    private final WhatsAppNotifyService whatsAppNotifyService;
    private final CurrencyRateMapper currencyRateMapper;
    private final TransactionMapper transactionMapper;
    private final MerchantAliasMapper merchantAliasMapper;
    private final MerchantMapper merchantMapper;
    private final AccountMapper accountMapper;
    private final RecurringRuleMapper recurringRuleMapper;
    private final AlertRuleMapper alertRuleMapper;
    private final AlertLogMapper alertLogMapper;
    private final BudgetMapper budgetMapper;
    private final BudgetItemMapper budgetItemMapper;
    private final AnnualBudgetMapper annualBudgetMapper;
    private final AnnualBudgetItemMapper annualBudgetItemMapper;
    private final StringRedisTemplate redisTemplate;
    private final ScheduledTasks scheduledTasks;

    @SaCheckRole("ADMIN")
    @GetMapping("/config")
    public Result<Map<String, String>> getAllConfig() {
        log.info("Read all system config: userId={}", StpUtil.getLoginIdDefaultNull());
        return Result.ok(configService.getAll());
    }

    @SaCheckRole("ADMIN")
    @GetMapping("/config/{key}")
    public Result<String> getConfig(@PathVariable String key) {
        log.info("Read system config: key={} userId={}", key, StpUtil.getLoginIdDefaultNull());
        return Result.ok(configService.get(key));
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/config/{key}")
    public Result<Void> setConfig(@PathVariable String key, @RequestBody Map<String, String> body) {
        configService.set(key, body.get("value"));
        // 配置值可能包含 Bot Token / API Key，只记录键名和操作人。
        log.info("Updated system config: key={} userId={}", key, StpUtil.getLoginIdDefaultNull());
        return Result.ok();
    }

    @GetMapping("/currencies")
    public Result<List<String>> getSupportedCurrencies() {
        String csv = configService.get("supported_currencies");
        if (csv.isBlank()) csv = "CNY,USD,EUR,GBP,JPY,HKD,SGD,THB,KRW";
        return Result.ok(List.of(csv.split(",")));
    }

    @GetMapping("/rates")
    public Result<List<CurrencyRate>> getRates(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        LambdaQueryWrapper<CurrencyRate> q = new LambdaQueryWrapper<>();
        q.eq(CurrencyRate::getRateDate, queryDate).orderByAsc(CurrencyRate::getTargetCurrency);
        return Result.ok(currencyRateMapper.selectList(q));
    }

    @GetMapping("/rate")
    public Result<java.math.BigDecimal> getRate(
            @RequestParam String currency,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return Result.ok(currencyRateService.getRate(currency, queryDate));
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/rates/refresh")
    public Result<String> refreshRates() {
        currencyRateService.refreshRates();
        return Result.ok("汇率刷新完成");
    }

    @GetMapping("/alerts")
    public Result<List<AlertLog>> getAlerts() {
        return Result.ok(alertLogService.getUnreadAlerts());
    }

    @PutMapping("/alerts/{id}/read")
    public Result<Void> markAlertRead(@PathVariable Long id) {
        alertLogService.markRead(id);
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/telegram/test")
    public Result<String> testTelegram() {
        boolean success = telegramNotifyService.testSend();
        log.info("Telegram test notification: userId={} success={}",
                StpUtil.getLoginIdDefaultNull(), success);
        return success ? Result.ok("Telegram 推送成功") : Result.fail(10003, "Telegram 推送失败");
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/whatsapp/test")
    public Result<String> testWhatsApp() {
        boolean success = whatsAppNotifyService.testSend();
        log.info("WhatsApp test notification: userId={} success={}",
                StpUtil.getLoginIdDefaultNull(), success);
        return success ? Result.ok("WhatsApp 推送成功") : Result.fail(10003, "WhatsApp 推送失败");
    }

    @SaCheckRole("ADMIN")
    @DeleteMapping("/test-data")
    public Result<Map<String, Integer>> clearTestData() {
        // 清空所有测试数据（保留系统分类）
        int transactions = transactionMapper.delete(new LambdaQueryWrapper<>());
        int merchantAliases = merchantAliasMapper.delete(new LambdaQueryWrapper<>());
        int merchants = merchantMapper.delete(new LambdaQueryWrapper<>());
        int accounts = accountMapper.delete(new LambdaQueryWrapper<>());
        int recurringRules = recurringRuleMapper.delete(new LambdaQueryWrapper<>());
        int alertRules = alertRuleMapper.delete(new LambdaQueryWrapper<>());
        int alertLogs = alertLogMapper.delete(new LambdaQueryWrapper<>());
        int budgets = budgetMapper.delete(new LambdaQueryWrapper<>());
        int budgetItems = budgetItemMapper.delete(new LambdaQueryWrapper<>());
        int annualBudgets = annualBudgetMapper.delete(new LambdaQueryWrapper<>());
        int annualBudgetItems = annualBudgetItemMapper.delete(new LambdaQueryWrapper<>());

        // 清除 Redis 缓存（仅删除应用相关 key，不 flushDb）
        deleteRedisKeys("report:*");
        deleteRedisKeys("idempotency:*");

        Map<String, Integer> result = Map.ofEntries(
            Map.entry("transactions", transactions),
            Map.entry("merchantAliases", merchantAliases),
            Map.entry("merchants", merchants),
            Map.entry("accounts", accounts),
            Map.entry("recurringRules", recurringRules),
            Map.entry("alertRules", alertRules),
            Map.entry("alertLogs", alertLogs),
            Map.entry("budgets", budgets),
            Map.entry("budgetItems", budgetItems),
            Map.entry("annualBudgets", annualBudgets),
            Map.entry("annualBudgetItems", annualBudgetItems)
        );
        log.warn("Test data cleared: userId={} deleted={}",
                StpUtil.getLoginIdDefaultNull(), result);
        return Result.ok(result);
    }

    private void deleteRedisKeys(String pattern) {
        var cursor = redisTemplate.scan(ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build());
        while (cursor.hasNext()) {
            redisTemplate.delete(cursor.next());
        }
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/check-unconfirmed")
    public Result<String> checkUnconfirmedTransactions() {
        log.info("Manual unconfirmed transaction check: userId={}", StpUtil.getLoginIdDefaultNull());
        scheduledTasks.checkUnconfirmedTransactions();
        return Result.ok("待确认交易检查已执行");
    }
}
