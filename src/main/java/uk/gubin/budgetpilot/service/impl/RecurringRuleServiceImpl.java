package uk.gubin.budgetpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.dto.RecurringRuleCreateDTO;
import uk.gubin.budgetpilot.dto.RecurringRuleUpdateDTO;
import uk.gubin.budgetpilot.dto.TransactionCreateDTO;
import uk.gubin.budgetpilot.entity.Account;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.RecurringRule;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.RecurringRuleMapper;
import uk.gubin.budgetpilot.service.RecurringRuleService;
import uk.gubin.budgetpilot.service.TransactionService;
import uk.gubin.budgetpilot.vo.TransactionVO;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringRuleServiceImpl extends ServiceImpl<RecurringRuleMapper, RecurringRule> implements RecurringRuleService {

    private static final Set<String> FREQUENCIES = Set.of("DAILY", "WEEKLY", "MONTHLY", "YEARLY");
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private final TransactionService transactionService;
    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public List<RecurringRule> getRulesDueToday() {
        LocalDate today = LocalDate.now();
        return baseMapper.selectList(new LambdaQueryWrapper<RecurringRule>()
                .eq(RecurringRule::getIsActive, true)
                .le(RecurringRule::getNextExecute, today)
                // 有截止日期时，只处理截止日期前（含当天）尚未执行的周期
                .apply("(end_date IS NULL OR next_execute <= end_date)")
                .orderByAsc(RecurringRule::getId));
    }

    /**
     * 以本次实际执行的 nextExecute 为基准推进计划，避免月末/漏跑时日期漂移。
     */
    @Override
    public void updateNextExecute(RecurringRule rule) {
        LocalDate executed = rule.getNextExecute();
        if (executed == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "规则的 nextExecute 为空");
        }
        rule.setLastExecuted(executed);
        LocalDate next = calculateNextExecute(rule, executed);
        rule.setNextExecute(next);
        if (rule.getEndDate() != null && next.isAfter(rule.getEndDate())) {
            rule.setIsActive(false);
        }
        baseMapper.updateById(rule);
    }

    @Override
    @Transactional
    public void generateTransaction(RecurringRule rule) {
        if (rule == null || rule.getNextExecute() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "周期规则或下次执行日期不存在");
        }
        createTransaction(rule, rule.getNextExecute(), rule.getNextExecute(), false);
    }

    /**
     * 立即补一笔今天的交易。不改变 nextExecute，重复点击同一天只生成一笔。
     */
    @Override
    @Transactional
    public void executeNow(Long id) {
        RecurringRule rule = baseMapper.selectById(id);
        if (rule == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "周期规则不存在");
        }
        if (!Boolean.TRUE.equals(rule.getIsActive())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "规则已停用，请先启用");
        }
        if (rule.getNextExecute() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "规则的 nextExecute 为空");
        }
        if (rule.getEndDate() != null && rule.getNextExecute().isAfter(rule.getEndDate())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "规则已过截止日期");
        }
        createTransaction(rule, LocalDate.now(), rule.getNextExecute(), true);
        log.info("Executed recurring rule manually: {}", rule.getName());
    }

    @Override
    @Transactional
    public RecurringRule create(RecurringRuleCreateDTO dto) {
        RecurringRule rule = new RecurringRule();
        rule.setName(dto.getName());
        rule.setType(dto.getType());
        rule.setAmount(dto.getAmount());
        rule.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "CNY");
        rule.setAccountId(dto.getAccountId());
        rule.setCategoryId(dto.getCategoryId());
        rule.setMerchantId(dto.getMerchantId());
        rule.setFrequency(dto.getFrequency());
        rule.setExecuteDay(dto.getExecuteDay());
        rule.setStartDate(dto.getStartDate());
        rule.setEndDate(dto.getEndDate());
        rule.setAutoConfirm(Boolean.TRUE.equals(dto.getAutoConfirm()));
        rule.setNote(dto.getNote());
        rule.setExtFields(dto.getExtFields());
        rule.setIsActive(true);

        validateRule(rule);
        rule.setExecuteDay(normalizeExecuteDay(rule));
        rule.setNextExecute(dto.getNextExecute() != null
                ? dto.getNextExecute()
                : firstExecuteDate(rule, rule.getStartDate()));
        if (rule.getNextExecute().isBefore(rule.getStartDate())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "首次执行日期不能早于开始日期");
        }
        if (rule.getEndDate() != null && rule.getNextExecute().isAfter(rule.getEndDate())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "首次执行日期不能晚于截止日期");
        }

        baseMapper.insert(rule);
        log.info("Created recurring rule: id={} name={} nextExecute={}",
                rule.getId(), rule.getName(), rule.getNextExecute());
        return rule;
    }

    @Override
    @Transactional
    public RecurringRule update(Long id, RecurringRuleUpdateDTO dto) {
        RecurringRule rule = baseMapper.selectById(id);
        if (rule == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "周期规则不存在");
        }
        // 先判断计划字段是否真的变化；页面编辑会提交全部字段，不能仅凭非空就重算
        boolean scheduleChanged =
                (dto.getFrequency() != null && !dto.getFrequency().equals(rule.getFrequency()))
                || (dto.getExecuteDay() != null && !dto.getExecuteDay().equals(rule.getExecuteDay()))
                || (dto.getStartDate() != null && !dto.getStartDate().equals(rule.getStartDate()));

        if (dto.getName() != null) rule.setName(dto.getName());
        if (dto.getType() != null) rule.setType(dto.getType());
        if (dto.getAmount() != null) rule.setAmount(dto.getAmount());
        if (dto.getCurrency() != null) rule.setCurrency(dto.getCurrency());
        if (dto.getAccountId() != null) rule.setAccountId(dto.getAccountId());
        if (dto.getCategoryId() != null) rule.setCategoryId(dto.getCategoryId());
        if (dto.getFrequency() != null) rule.setFrequency(dto.getFrequency());
        // 商户和截止日期允许通过传 null 清除
        rule.setMerchantId(dto.getMerchantId());
        rule.setEndDate(dto.getEndDate());
        if (dto.getStartDate() != null) rule.setStartDate(dto.getStartDate());
        if (dto.getNextExecute() != null) rule.setNextExecute(dto.getNextExecute());
        if (dto.getAutoConfirm() != null) rule.setAutoConfirm(dto.getAutoConfirm());
        if (dto.getIsActive() != null) rule.setIsActive(dto.getIsActive());
        if (dto.getNote() != null) rule.setNote(dto.getNote());
        if (dto.getExtFields() != null) rule.setExtFields(dto.getExtFields());

        validateRule(rule);
        if (dto.getExecuteDay() != null || dto.getFrequency() != null || dto.getStartDate() != null) {
            rule.setExecuteDay(normalizeExecuteDay(rule));
        }
        if (dto.getNextExecute() != null && rule.getNextExecute().isBefore(rule.getStartDate())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "下次执行日期不能早于开始日期");
        }
        if (dto.getNextExecute() == null && scheduleChanged) {
            LocalDate base = LocalDate.now();
            if (rule.getStartDate().isAfter(base)) {
                base = rule.getStartDate();
            }
            rule.setNextExecute(firstExecuteDate(rule, base));
        }
        if (rule.getNextExecute() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "下次执行日期不能为空");
        }
        if (rule.getEndDate() != null && rule.getNextExecute().isAfter(rule.getEndDate())) {
            rule.setIsActive(false);
        }

        baseMapper.updateById(rule);
        log.info("Updated recurring rule: id={} name={} active={} nextExecute={}",
                id, rule.getName(), rule.getIsActive(), rule.getNextExecute());
        return rule;
    }

    @Override
    @Transactional
    public RecurringRule toggleActive(Long id) {
        RecurringRule rule = baseMapper.selectById(id);
        if (rule == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "周期规则不存在");
        }
        rule.setIsActive(!Boolean.TRUE.equals(rule.getIsActive()));
        baseMapper.updateById(rule);
        log.info("Toggled recurring rule: id={} name={} active={}",
                id, rule.getName(), rule.getIsActive());
        return rule;
    }

    private void createTransaction(RecurringRule rule, LocalDate transactionDate,
                                   LocalDate occurrenceDate, boolean manual) {
        Category category = categoryMapper.selectById(rule.getCategoryId());
        Account account = accountMapper.selectById(rule.getAccountId());
        if (category == null || account == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "周期规则引用的账户或分类不存在");
        }
        if (rule.getType() == null || (rule.getType() != 1 && rule.getType() != 2)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "周期规则只支持支出或收入");
        }

        // 幂等键区分「计划内的一次执行」和「手动立即执行」：
        // 计划内按规则 + 执行日唯一，手动按规则 + 当天唯一。
        String externalId = "recurring:" + rule.getId() + ":"
                + (manual ? "manual:" + transactionDate : occurrenceDate);

        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setType(rule.getType());
        dto.setAmount(rule.getAmount());
        dto.setCurrency(rule.getCurrency());
        dto.setAccountId(rule.getAccountId());
        dto.setCategoryId(rule.getCategoryId());
        dto.setMerchantId(rule.getMerchantId());
        dto.setTransactionDate(transactionDate);
        dto.setNote(rule.getNote());
        dto.setIsConfirmed(rule.getAutoConfirm());
        dto.setIsRecurring(true);
        dto.setRecurringId(rule.getId());
        dto.setExternalId(externalId);
        dto.setExtFields(parseJsonMap(rule.getExtFields()));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "recurring");
        metadata.put("rule_id", rule.getId());
        metadata.put("occurrence_date", occurrenceDate.toString());
        metadata.put("auto_confirmed", Boolean.TRUE.equals(rule.getAutoConfirm()));
        if (manual) {
            metadata.put("manual", true);
        }
        dto.setMetadata(metadata);

        TransactionVO vo = transactionService.create(dto);
        log.info("Generated transaction {} from recurring rule {} (due {}, manual={})",
                vo.getId(), rule.getName(), occurrenceDate, manual);
    }

    private void validateRule(RecurringRule rule) {
        if (rule.getType() == null || (rule.getType() != 1 && rule.getType() != 2)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "类型只能是支出或收入");
        }
        if (rule.getFrequency() == null || !FREQUENCIES.contains(rule.getFrequency())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "频率只能是 DAILY/WEEKLY/MONTHLY/YEARLY");
        }
        if (rule.getAmount() == null || rule.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "金额必须大于0");
        }
        if (rule.getStartDate() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "开始日期不能为空");
        }
        if (rule.getEndDate() != null && rule.getEndDate().isBefore(rule.getStartDate())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "截止日期不能早于开始日期");
        }
    }

    /**
     * 周规则：1=周一 … 7=周日；月规则：1-31，小月自动取月底。
     * 未填执行日时用开始日期推导。
     */
    private Integer normalizeExecuteDay(RecurringRule rule) {
        String frequency = rule.getFrequency();
        if ("DAILY".equals(frequency) || "YEARLY".equals(frequency)) {
            return null;
        }
        Integer day = rule.getExecuteDay();
        if ("WEEKLY".equals(frequency)) {
            int value = day != null ? day : rule.getStartDate().getDayOfWeek().getValue();
            return Math.max(1, Math.min(7, value));
        }
        int value = day != null ? day : rule.getStartDate().getDayOfMonth();
        return Math.max(1, Math.min(31, value));
    }

    /**
     * 从 from 开始，按频率/执行日找到第一个应该执行的日期。
     * 新建规则时 from = startDate；修改计划时 from = max(today, startDate)。
     */
    private LocalDate firstExecuteDate(RecurringRule rule, LocalDate from) {
        return switch (rule.getFrequency()) {
            case "WEEKLY" -> {
                LocalDate date = from;
                int target = rule.getExecuteDay();
                while (date.getDayOfWeek().getValue() != target) {
                    date = date.plusDays(1);
                }
                yield date;
            }
            case "MONTHLY" -> {
                int day = rule.getExecuteDay();
                YearMonth ym = YearMonth.from(from);
                LocalDate candidate = ym.atDay(Math.min(day, ym.lengthOfMonth()));
                if (candidate.isBefore(from)) {
                    YearMonth next = ym.plusMonths(1);
                    candidate = next.atDay(Math.min(day, next.lengthOfMonth()));
                }
                yield candidate;
            }
            case "YEARLY" -> {
                LocalDate anniversary = rule.getStartDate().withYear(from.getYear());
                if (anniversary.isBefore(from)) {
                    anniversary = anniversary.plusYears(1);
                }
                yield anniversary;
            }
            default -> from;
        };
    }

    private LocalDate calculateNextExecute(RecurringRule rule, LocalDate current) {
        return switch (rule.getFrequency()) {
            case "DAILY" -> current.plusDays(1);
            case "WEEKLY" -> current.plusWeeks(1);
            case "MONTHLY" -> {
                int day = rule.getExecuteDay() != null ? rule.getExecuteDay() : current.getDayOfMonth();
                YearMonth next = YearMonth.from(current).plusMonths(1);
                yield next.atDay(Math.min(day, next.lengthOfMonth()));
            }
            // 用 startDate 的月/日做锚点，闰日 2/29 在平年落 2/28，闰年仍回到 2/29
            case "YEARLY" -> rule.getStartDate().withYear(current.getYear() + 1);
            default -> throw new BizException(ErrorCode.PARAM_ERROR, "未知的周期频率");
        };
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return JSON.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse ext_fields JSON for recurring rule", e);
            return null;
        }
    }
}