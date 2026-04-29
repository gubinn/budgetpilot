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
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringRuleServiceImpl extends ServiceImpl<RecurringRuleMapper, RecurringRule> implements RecurringRuleService {

    private final TransactionService transactionService;
    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public List<RecurringRule> getRulesDueToday() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<RecurringRule> query = new LambdaQueryWrapper<>();
        query.eq(RecurringRule::getIsActive, true)
                .le(RecurringRule::getNextExecute, today)
                .and(w -> w.isNull(RecurringRule::getEndDate)
                        .or().ge(RecurringRule::getEndDate, today));
        return baseMapper.selectList(query);
    }

    @Override
    public void updateNextExecute(RecurringRule rule) {
        rule.setLastExecuted(LocalDate.now());
        rule.setNextExecute(calculateNextExecute(rule));
        baseMapper.updateById(rule);
    }

    @Override
    @Transactional
    public void generateTransaction(RecurringRule rule) {
        Category category = categoryMapper.selectById(rule.getCategoryId());
        Account account = accountMapper.selectById(rule.getAccountId());
        if (category == null || account == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "周期规则引用的账户或分类不存在");
        }

        // 通过 TransactionService.create() 创建，确保余额、预算、预警全部正确处理
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setType(rule.getType());
        dto.setAmount(rule.getAmount());
        dto.setCurrency(rule.getCurrency());
        dto.setAccountId(rule.getAccountId());
        dto.setCategoryId(rule.getCategoryId());
        dto.setMerchantId(rule.getMerchantId());
        dto.setTransactionDate(LocalDate.now());
        dto.setNote(rule.getNote());
        dto.setIsConfirmed(rule.getAutoConfirm());
        dto.setIsRecurring(true);
        dto.setRecurringId(rule.getId());
        dto.setExtFields(parseJsonMap(rule.getExtFields()));
        dto.setMetadata(Map.of(
                "source", "recurring",
                "rule_id", rule.getId(),
                "auto_confirmed", rule.getAutoConfirm()
        ));

        TransactionVO vo = transactionService.create(dto);
        log.info("Generated transaction {} from recurring rule: {}", vo.getId(), rule.getName());
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
        rule.setNextExecute(dto.getNextExecute() != null ? dto.getNextExecute() : dto.getStartDate());
        rule.setAutoConfirm(dto.getAutoConfirm() != null ? dto.getAutoConfirm() : false);
        rule.setNote(dto.getNote());
        rule.setExtFields(dto.getExtFields());
        rule.setIsActive(true);
        baseMapper.insert(rule);
        return rule;
    }

    @Override
    @Transactional
    public RecurringRule update(Long id, RecurringRuleUpdateDTO dto) {
        RecurringRule rule = baseMapper.selectById(id);
        if (rule == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "周期规则不存在");
        }
        if (dto.getName() != null) rule.setName(dto.getName());
        if (dto.getType() != null) rule.setType(dto.getType());
        if (dto.getAmount() != null) rule.setAmount(dto.getAmount());
        if (dto.getCurrency() != null) rule.setCurrency(dto.getCurrency());
        if (dto.getAccountId() != null) rule.setAccountId(dto.getAccountId());
        if (dto.getCategoryId() != null) rule.setCategoryId(dto.getCategoryId());
        if (dto.getMerchantId() != null) rule.setMerchantId(dto.getMerchantId());
        if (dto.getFrequency() != null) rule.setFrequency(dto.getFrequency());
        if (dto.getExecuteDay() != null) rule.setExecuteDay(dto.getExecuteDay());
        if (dto.getStartDate() != null) rule.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) rule.setEndDate(dto.getEndDate());
        if (dto.getNextExecute() != null) rule.setNextExecute(dto.getNextExecute());
        if (dto.getAutoConfirm() != null) rule.setAutoConfirm(dto.getAutoConfirm());
        if (dto.getIsActive() != null) rule.setIsActive(dto.getIsActive());
        if (dto.getNote() != null) rule.setNote(dto.getNote());
        if (dto.getExtFields() != null) rule.setExtFields(dto.getExtFields());
        baseMapper.updateById(rule);
        return rule;
    }

    @Override
    @Transactional
    public RecurringRule toggleActive(Long id) {
        RecurringRule rule = baseMapper.selectById(id);
        if (rule == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "周期规则不存在");
        }
        rule.setIsActive(!rule.getIsActive());
        baseMapper.updateById(rule);
        return rule;
    }

    private LocalDate calculateNextExecute(RecurringRule rule) {
        LocalDate current = rule.getNextExecute();
        return switch (rule.getFrequency()) {
            case "DAILY" -> current.plusDays(1);
            case "WEEKLY" -> current.plusWeeks(1);
            case "MONTHLY" -> current.plusMonths(1);
            case "YEARLY" -> current.plusYears(1);
            default -> current.plusMonths(1);
        };
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse ext_fields JSON for recurring rule", e);
            return null;
        }
    }
}
