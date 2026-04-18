package uk.gubin.budgetpilot.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
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
import uk.gubin.budgetpilot.service.ScheduledTasks;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final ConfigService configService;
    private final CurrencyRateService currencyRateService;
    private final AlertLogService alertLogService;
    private final TelegramNotifyService telegramNotifyService;
    private final CurrencyRateMapper currencyRateMapper;
    private final TransactionMapper transactionMapper;
    private final AccountMapper accountMapper;
    private final RecurringRuleMapper recurringRuleMapper;
    private final AlertRuleMapper alertRuleMapper;
    private final AlertLogMapper alertLogMapper;
    private final BudgetMapper budgetMapper;
    private final BudgetItemMapper budgetItemMapper;
    private final StringRedisTemplate redisTemplate;
    private final ScheduledTasks scheduledTasks;

    @GetMapping("/config")
    public Result<Map<String, String>> getAllConfig() {
        return Result.ok(configService.getAll());
    }

    @PutMapping("/config/{key}")
    public Result<Void> setConfig(@PathVariable String key, @RequestBody Map<String, String> body) {
        configService.set(key, body.get("value"));
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

    @PostMapping("/telegram/test")
    public Result<String> testTelegram() {
        boolean success = telegramNotifyService.testSend();
        return success ? Result.ok("Telegram 推送成功") : Result.fail(10003, "Telegram 推送失败");
    }

    @DeleteMapping("/test-data")
    public Result<Map<String, Integer>> clearTestData() {
        // 清空所有测试数据（保留系统分类）
        int transactions = transactionMapper.delete(new LambdaQueryWrapper<>());
        int accounts = accountMapper.delete(new LambdaQueryWrapper<>());
        int recurringRules = recurringRuleMapper.delete(new LambdaQueryWrapper<>());
        int alertRules = alertRuleMapper.delete(new LambdaQueryWrapper<>());
        int alertLogs = alertLogMapper.delete(new LambdaQueryWrapper<>());
        int budgets = budgetMapper.delete(new LambdaQueryWrapper<>());
        int budgetItems = budgetItemMapper.delete(new LambdaQueryWrapper<>());

        // 清除Redis缓存
        redisTemplate.getConnectionFactory().getConnection().flushDb();

        return Result.ok(Map.of(
            "transactions", transactions,
            "accounts", accounts,
            "recurringRules", recurringRules,
            "alertRules", alertRules,
            "alertLogs", alertLogs,
            "budgets", budgets,
            "budgetItems", budgetItems
        ));
    }

    @PostMapping("/check-unconfirmed")
    public Result<String> checkUnconfirmedTransactions() {
        scheduledTasks.checkUnconfirmedTransactions();
        return Result.ok("待确认交易检查已执行");
    }
}
