package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gubin.budgetpilot.entity.AlertRule;
import uk.gubin.budgetpilot.entity.Budget;
import uk.gubin.budgetpilot.mapper.AlertRuleMapper;
import uk.gubin.budgetpilot.mapper.BudgetMapper;
import uk.gubin.budgetpilot.mapper.CurrencyRateMapper;

/**
 * 系统配置服务 - 管理 t_config 表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService extends ServiceImpl<uk.gubin.budgetpilot.mapper.ConfigMapper, uk.gubin.budgetpilot.entity.Config> {

    /**
     * 获取配置值
     */
    public String get(String key) {
        LambdaQueryWrapper<uk.gubin.budgetpilot.entity.Config> query = new LambdaQueryWrapper<>();
        query.eq(uk.gubin.budgetpilot.entity.Config::getConfigKey, key);
        uk.gubin.budgetpilot.entity.Config config = baseMapper.selectOne(query);
        return config != null ? config.getConfigValue() : "";
    }

    /**
     * 更新配置值
     */
    public void set(String key, String value) {
        LambdaQueryWrapper<uk.gubin.budgetpilot.entity.Config> query = new LambdaQueryWrapper<>();
        query.eq(uk.gubin.budgetpilot.entity.Config::getConfigKey, key);
        uk.gubin.budgetpilot.entity.Config config = baseMapper.selectOne(query);
        if (config == null) {
            config = new uk.gubin.budgetpilot.entity.Config();
            config.setConfigKey(key);
            config.setConfigValue(value);
            baseMapper.insert(config);
        } else {
            config.setConfigValue(value);
            baseMapper.updateById(config);
        }
    }

    /**
     * 获取所有配置
     */
    public java.util.Map<String, String> getAll() {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        list().forEach(c -> map.put(c.getConfigKey(), c.getConfigValue()));
        return map;
    }
}
