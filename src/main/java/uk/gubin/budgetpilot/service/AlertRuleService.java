package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.dto.AlertRuleCreateDTO;
import uk.gubin.budgetpilot.dto.AlertRuleUpdateDTO;
import uk.gubin.budgetpilot.entity.AlertRule;
import uk.gubin.budgetpilot.mapper.AlertRuleMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleService extends ServiceImpl<AlertRuleMapper, AlertRule> {

    public List<AlertRule> getActiveRules() {
        LambdaQueryWrapper<AlertRule> query = new LambdaQueryWrapper<>();
        query.eq(AlertRule::getIsActive, true);
        return baseMapper.selectList(query);
    }

    public List<AlertRule> getActiveRulesByUserId(Long userId) {
        return baseMapper.selectActiveByUserId(userId);
    }

    public List<AlertRule> getActiveRulesByType(Integer type) {
        LambdaQueryWrapper<AlertRule> query = new LambdaQueryWrapper<>();
        query.eq(AlertRule::getIsActive, true).eq(AlertRule::getType, type);
        return baseMapper.selectList(query);
    }

    public AlertRule create(AlertRuleCreateDTO dto) {
        AlertRule rule = new AlertRule();
        rule.setName(dto.getName());
        rule.setType(dto.getType());
        rule.setConfig(dto.getConfig());
        rule.setNotifyChannel(dto.getNotifyChannel() != null ? dto.getNotifyChannel() : "TELEGRAM");
        rule.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        baseMapper.insert(rule);
        log.info("Created alert rule: {}", rule.getName());
        return rule;
    }

    public AlertRule update(Long id, AlertRuleUpdateDTO dto) {
        AlertRule rule = baseMapper.selectById(id);
        if (rule == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "预警规则不存在");
        }
        if (dto.getName() != null) rule.setName(dto.getName());
        if (dto.getType() != null) rule.setType(dto.getType());
        if (dto.getConfig() != null) rule.setConfig(dto.getConfig());
        if (dto.getNotifyChannel() != null) rule.setNotifyChannel(dto.getNotifyChannel());
        if (dto.getIsActive() != null) rule.setIsActive(dto.getIsActive());
        baseMapper.updateById(rule);
        log.info("Updated alert rule: {}", rule.getName());
        return rule;
    }

    public AlertRule toggleActive(Long id) {
        AlertRule rule = baseMapper.selectById(id);
        if (rule == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "预警规则不存在");
        }
        rule.setIsActive(!rule.getIsActive());
        baseMapper.updateById(rule);
        log.info("Toggled alert rule {}: active={}", rule.getName(), rule.getIsActive());
        return rule;
    }
}
