package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    public List<AlertRule> getActiveRulesByType(Integer type) {
        LambdaQueryWrapper<AlertRule> query = new LambdaQueryWrapper<>();
        query.eq(AlertRule::getIsActive, true).eq(AlertRule::getType, type);
        return baseMapper.selectList(query);
    }
}
