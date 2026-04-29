package uk.gubin.budgetpilot.service;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.dto.ChangePasswordDTO;
import uk.gubin.budgetpilot.dto.LoginDTO;
import uk.gubin.budgetpilot.dto.UserCreateDTO;
import uk.gubin.budgetpilot.dto.UserUpdateDTO;
import uk.gubin.budgetpilot.entity.AlertRule;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.User;
import uk.gubin.budgetpilot.entity.UserConfig;
import uk.gubin.budgetpilot.mapper.AlertRuleMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.UserConfigMapper;
import uk.gubin.budgetpilot.mapper.UserMapper;
import uk.gubin.budgetpilot.vo.LoginVO;
import uk.gubin.budgetpilot.vo.UserVO;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private final UserConfigMapper userConfigMapper;
    private final CategoryMapper categoryMapper;
    private final AlertRuleMapper alertRuleMapper;
    private final uk.gubin.budgetpilot.mapper.BudgetMapper budgetMapper;
    private final uk.gubin.budgetpilot.mapper.BudgetItemMapper budgetItemMapper;
    private final uk.gubin.budgetpilot.mapper.TransactionMapper transactionMapper;
    private final uk.gubin.budgetpilot.mapper.AccountMapper accountMapper;
    private final uk.gubin.budgetpilot.mapper.MerchantMapper merchantMapper;
    private final uk.gubin.budgetpilot.mapper.RecurringRuleMapper recurringRuleMapper;
    private final uk.gubin.budgetpilot.mapper.AlertLogMapper alertLogMapper;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户登录
     */
    public LoginVO login(LoginDTO dto) {
        String username = dto.getUsername();

        // 登录限流：同一用户名连续失败 5 次后锁定 15 分钟
        // Redis 不可用时跳过限流（优雅降级）
        try {
            String failKey = "login_fail:" + username;
            String lockKey = "login_lock:" + username;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
                throw new BizException(ErrorCode.AUTH_WRONG_CREDENTIALS, "登录失败次数过多，请 15 分钟后再试");
            }

            LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
            query.eq(User::getUsername, username);
            User user = baseMapper.selectOne(query);

            if (user == null) {
                recordLoginFail(failKey);
                throw new BizException(ErrorCode.AUTH_WRONG_CREDENTIALS);
            }

            if (!user.getIsActive()) {
                throw new BizException(ErrorCode.AUTH_USER_DISABLED);
            }

            if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
                recordLoginFail(failKey);
                throw new BizException(ErrorCode.AUTH_WRONG_CREDENTIALS);
            }

            // 登录成功，清除失败计数和锁定
            try {
                redisTemplate.delete(failKey);
                redisTemplate.delete(lockKey);
            } catch (Exception ignored) {}

            // Sa-Token 登录
            StpUtil.login(user.getId());
            // 将角色存入 Token Session
            StpUtil.getTokenSession().set("role", user.getRole());

            // 更新最后登录时间
            user.setLastLogin(LocalDateTime.now());
            baseMapper.updateById(user);

            LoginVO vo = new LoginVO();
            vo.setToken(StpUtil.getTokenValue());
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setRole(user.getRole());
            return vo;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            // Redis 不可用（连接超时、超时异常等），跳过登录限流，降级为不限流登录
            log.warn("Redis 不可用，跳过登录限流: {}", e.getClass().getSimpleName());
            LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
            query.eq(User::getUsername, username);
            User user = baseMapper.selectOne(query);
            if (user == null || !user.getIsActive()) {
                throw new BizException(ErrorCode.AUTH_WRONG_CREDENTIALS);
            }
            if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
                throw new BizException(ErrorCode.AUTH_WRONG_CREDENTIALS);
            }
            StpUtil.login(user.getId());
            StpUtil.getTokenSession().set("role", user.getRole());
            user.setLastLogin(LocalDateTime.now());
            baseMapper.updateById(user);
            LoginVO vo = new LoginVO();
            vo.setToken(StpUtil.getTokenValue());
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setRole(user.getRole());
            return vo;
        }
    }

    private void recordLoginFail(String failKey) {
        Long count = redisTemplate.opsForValue().increment(failKey);
        if (count != null && count >= 5) {
            // 达到阈值，设置锁定 15 分钟
            redisTemplate.opsForValue().set(failKey.replace("login_fail:", "login_lock:"), "1", 15, java.util.concurrent.TimeUnit.MINUTES);
            redisTemplate.delete(failKey);
        } else {
            // 设置过期时间 15 分钟（首次创建时设置，后续 increment 不改变 TTL）
            redisTemplate.expire(failKey, 15, java.util.concurrent.TimeUnit.MINUTES);
        }
    }

    /**
     * 退出登录
     */
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 获取当前用户信息
     */
    public UserVO getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return toVO(user);
    }

    /**
     * 修改密码
     */
    public void changePassword(ChangePasswordDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = baseMapper.selectById(userId);

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.AUTH_WRONG_OLD_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        baseMapper.updateById(user);

        // 使其他会话失效（防止并发 session 在改密后仍可用）
        StpUtil.kickout(userId);
        // 重新登录保持当前会话
        StpUtil.login(userId);
    }

    /**
     * 创建用户
     */
    public UserVO create(UserCreateDTO dto) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, dto.getUsername());
        if (baseMapper.selectCount(query) > 0) {
            throw new BizException(ErrorCode.AUTH_USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setRole(dto.getRole());
        user.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        baseMapper.insert(user);

        // 为新用户复制系统预置分类和预警规则
        copyDefaultCategories(user.getId());
        copyDefaultAlertRules(user.getId());

        return toVO(user);
    }

    /**
     * 复制系统预置分类到新用户（忽略租户拦截器）
     */
    @InterceptorIgnore(tenantLine = "true")
    private void copyDefaultCategories(Long userId) {
        List<Category> defaultCategories = categoryMapper.selectSystemDefaults();

        // 先处理父分类（parent_id=0），建立旧ID→新ID映射
        Map<Long, Long> idMapping = new HashMap<>();
        for (Category cat : defaultCategories) {
            if (cat.getParentId() == null || cat.getParentId() == 0L) {
                Category newCat = new Category();
                newCat.setUserId(userId);
                newCat.setParentId(0L);
                newCat.setName(cat.getName());
                newCat.setType(cat.getType());
                newCat.setIcon(cat.getIcon());
                newCat.setColor(cat.getColor());
                newCat.setSortOrder(cat.getSortOrder());
                newCat.setIsSystem(false);
                newCat.setIsActive(true);
                categoryMapper.insert(newCat);
                idMapping.put(cat.getId(), newCat.getId());
            }
        }

        // 再处理子分类，用映射替换 parent_id
        for (Category cat : defaultCategories) {
            if (cat.getParentId() != null && cat.getParentId() > 0L && idMapping.containsKey(cat.getParentId())) {
                Category newCat = new Category();
                newCat.setUserId(userId);
                newCat.setParentId(idMapping.get(cat.getParentId()));
                newCat.setName(cat.getName());
                newCat.setType(cat.getType());
                newCat.setIcon(cat.getIcon());
                newCat.setColor(cat.getColor());
                newCat.setSortOrder(cat.getSortOrder());
                newCat.setIsSystem(false);
                newCat.setIsActive(true);
                categoryMapper.insert(newCat);
            }
        }

        log.info("Copied {} default categories for user {}", defaultCategories.size(), userId);
    }

    /**
     * 复制系统预置预警规则到新用户（忽略租户拦截器，硬编码默认规则）
     */
    @InterceptorIgnore(tenantLine = "true")
    private void copyDefaultAlertRules(Long userId) {
        Object[][] defaultRules = {
                {"预算阈值预警", 1, "{\"threshold_pct\": 80}"},
                {"单笔大额预警", 2, "{\"max_amount\": 1000}"},
                {"日消费上限", 3, "{\"daily_limit\": 500}"},
                {"周消费异常检测", 4, "{\"deviation_pct\": 50}"},
                {"信用卡还款提醒", 5, "{\"advance_days\": 3}"},
                {"周期账单提醒", 6, "{\"advance_days\": 1}"},
                {"月度预算未设定", 7, "{\"check_day\": 25}"}
        };

        for (Object[] rule : defaultRules) {
            AlertRule newRule = new AlertRule();
            newRule.setUserId(userId);
            newRule.setName((String) rule[0]);
            newRule.setType((Integer) rule[1]);
            newRule.setConfig((String) rule[2]);
            newRule.setNotifyChannel("TELEGRAM");
            newRule.setIsActive(true);
            alertRuleMapper.insert(newRule);
        }

        log.info("Copied {} default alert rules for user {}", defaultRules.length, userId);
    }

    /**
     * 删除用户（级联清理关联数据）
     */
    @Transactional
    public void delete(Long id) {
        User user = baseMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        log.info("Deleting user {} (id={}), cleaning up associated data", user.getUsername(), id);

        // 清理预警日志
        alertLogMapper.deleteByUserId(id);
        // 清理预警规则
        alertRuleMapper.deleteByUserId(id);
        // 清理周期规则
        recurringRuleMapper.deleteByUserId(id);
        // 清理预算项和预算
        budgetItemMapper.deleteByUserId(id);
        budgetMapper.deleteByUserId(id);
        // 清理交易
        transactionMapper.deleteByUserId(id);
        // 清理商户
        merchantMapper.deleteByUserId(id);
        // 清理账户
        accountMapper.deleteByUserId(id);
        // 清理分类
        categoryMapper.deleteByUserId(id);
        // 清理用户配置
        userConfigMapper.deleteByUserId(id);

        // 最后删除用户
        baseMapper.deleteById(id);

        log.info("User {} (id={}) deleted successfully", user.getUsername(), id);
    }

    /**
     * 更新用户
     */
    public UserVO update(Long id, UserUpdateDTO dto) {
        User user = baseMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getRole() != null) user.setRole(dto.getRole());
        if (dto.getIsActive() != null) user.setIsActive(dto.getIsActive());

        baseMapper.updateById(user);
        return toVO(user);
    }

    /**
     * 重置密码
     */
    public void resetPassword(Long id, String newPassword) {
        User user = baseMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        baseMapper.updateById(user);
    }

    /**
     * 用户列表
     */
    public List<UserVO> listUsers() {
        return list().stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 获取用户配置
     */
    public String getUserConfig(Long userId, String key) {
        LambdaQueryWrapper<UserConfig> query = new LambdaQueryWrapper<>();
        query.eq(UserConfig::getUserId, userId)
             .eq(UserConfig::getConfigKey, key);
        UserConfig config = userConfigMapper.selectOne(query);
        return config != null ? config.getConfigValue() : null;
    }

    /**
     * 设置用户配置
     */
    public void setUserConfig(Long userId, String key, String value) {
        LambdaQueryWrapper<UserConfig> query = new LambdaQueryWrapper<>();
        query.eq(UserConfig::getUserId, userId)
             .eq(UserConfig::getConfigKey, key);
        UserConfig config = userConfigMapper.selectOne(query);

        if (config == null) {
            config = new UserConfig();
            config.setUserId(userId);
            config.setConfigKey(key);
            config.setConfigValue(value);
            userConfigMapper.insert(config);
        } else {
            config.setConfigValue(value);
            userConfigMapper.updateById(config);
        }
    }

    /**
     * 获取用户所有配置
     */
    public java.util.Map<String, String> getUserConfigAll(Long userId) {
        LambdaQueryWrapper<UserConfig> query = new LambdaQueryWrapper<>();
        query.eq(UserConfig::getUserId, userId);
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        userConfigMapper.selectList(query).forEach(c -> map.put(c.getConfigKey(), c.getConfigValue()));
        return map;
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setIsActive(user.getIsActive());
        vo.setLastLogin(user.getLastLogin());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
