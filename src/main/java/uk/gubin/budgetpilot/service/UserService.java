package uk.gubin.budgetpilot.service;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
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
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户登录
     */
    public LoginVO login(LoginDTO dto) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, dto.getUsername());
        User user = baseMapper.selectOne(query);

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!user.getIsActive()) {
            throw new RuntimeException("账户已被停用");
        }

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
            throw new RuntimeException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        baseMapper.updateById(user);

        // 重新登录保持会话
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
            throw new RuntimeException("用户名已存在");
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
