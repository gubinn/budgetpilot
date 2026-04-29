package uk.gubin.budgetpilot.component;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Config;
import uk.gubin.budgetpilot.entity.User;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.ConfigMapper;
import uk.gubin.budgetpilot.mapper.UserMapper;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final ConfigMapper configMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void initDefaultAdmin() {
        long count = userMapper.selectCount(null);
        if (count == 0) {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String randomPassword = generateRandomPassword();
            String encoded = encoder.encode(randomPassword);

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(encoded);
            admin.setNickname("管理员");
            admin.setRole("ADMIN");
            admin.setIsActive(true);
            userMapper.insert(admin);

            log.warn("\n" +
                    "==========================================\n" +
                    "  [初始化] 已创建默认管理员账号\n" +
                    "  用户名: admin\n" +
                    "  密  码: {}\n" +
                    "  请首次登录后立即修改密码！\n" +
                    "==========================================", randomPassword);
        }

        // 初始化系统预置分类（如果不存在）
        initSystemCategories();

        // 初始化系统配置（如果不存在）
        initSystemConfig();
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 新用户创建时会复制这些分类到自己的租户下
     */
    @InterceptorIgnore(tenantLine = "true")
    private void initSystemCategories() {
        // 通过检查已知系统分类名称是否存在来判断是否已初始化
        // 不依赖 is_system 字段计数（旧数据可能 is_system 为 false）
        long existing = categoryMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<uk.gubin.budgetpilot.entity.Category>()
                        .eq(uk.gubin.budgetpilot.entity.Category::getUserId, 0L)
                        .eq(uk.gubin.budgetpilot.entity.Category::getParentId, 0L)
                        .in(uk.gubin.budgetpilot.entity.Category::getIcon,
                                "food", "transport", "salary", "adjust"));
        if (existing >= 4) {
            log.info("System categories already exist (count={}), skipping init", existing);
            return;
        }

        // 支出大类
        String[][] expenseParents = {
                {"餐饮", "food", "#FF6B6B", "1"},
                {"交通", "transport", "#4ECDC4", "2"},
                {"住房", "home", "#45B7D1", "3"},
                {"购物", "shopping", "#96CEB4", "4"},
                {"医疗", "medical", "#FFEAA7", "5"},
                {"教育", "education", "#DDA0DD", "6"},
                {"娱乐", "entertainment", "#98D8C8", "7"},
                {"社交", "social", "#F7DC6F", "8"},
                {"金融", "finance", "#BB8FCE", "9"},
                {"其他支出", "other-expense", "#BDC3C7", "10"},
        };

        Map<String, Long> parentIdMap = new LinkedHashMap<>();
        for (String[] row : expenseParents) {
            Category cat = new Category();
            cat.setUserId(0L);
            cat.setParentId(0L);
            cat.setName(row[0]);
            cat.setType(1);
            cat.setIcon(row[1]);
            cat.setColor(row[2]);
            cat.setIsSystem(true);
            cat.setIsActive(true);
            cat.setSortOrder(Integer.parseInt(row[3]));
            categoryMapper.insert(cat);
            parentIdMap.put(row[0], cat.getId());
        }

        // 收入大类
        String[][] incomeParents = {
                {"工资", "salary", "#2ECC71", "1"},
                {"副业", "sidejob", "#27AE60", "2"},
                {"投资", "investment", "#1ABC9C", "3"},
                {"报销", "reimburse", "#16A085", "4"},
                {"其他收入", "other-income", "#BDC3C7", "5"},
        };
        for (String[] row : incomeParents) {
            Category cat = new Category();
            cat.setUserId(0L);
            cat.setParentId(0L);
            cat.setName(row[0]);
            cat.setType(2);
            cat.setIcon(row[1]);
            cat.setColor(row[2]);
            cat.setIsSystem(true);
            cat.setIsActive(true);
            cat.setSortOrder(Integer.parseInt(row[3]));
            categoryMapper.insert(cat);
            parentIdMap.put(row[0], cat.getId());
        }

        // 特殊分类
        Category transfer = new Category();
        transfer.setUserId(0L);
        transfer.setParentId(0L);
        transfer.setName("内部转账");
        transfer.setType(1);
        transfer.setIcon("transfer");
        transfer.setColor("#95A5A6");
        transfer.setIsSystem(true);
        transfer.setIsActive(true);
        transfer.setSortOrder(11);
        categoryMapper.insert(transfer);
        parentIdMap.put("内部转账", transfer.getId());

        // 余额调整（固定 ID 58/59）
        Category adjIncome = new Category();
        adjIncome.setId(58L);
        adjIncome.setUserId(0L);
        adjIncome.setParentId(0L);
        adjIncome.setName("余额调整");
        adjIncome.setType(2);
        adjIncome.setIcon("adjust");
        adjIncome.setColor("#FF9800");
        adjIncome.setIsSystem(true);
        adjIncome.setIsActive(true);
        adjIncome.setSortOrder(99);
        try { categoryMapper.insert(adjIncome); } catch (Exception e) { log.warn("Category 58 already exists"); }

        Category adjExpense = new Category();
        adjExpense.setId(59L);
        adjExpense.setUserId(0L);
        adjExpense.setParentId(0L);
        adjExpense.setName("余额调整");
        adjExpense.setType(1);
        adjExpense.setIcon("adjust");
        adjExpense.setColor("#FF9800");
        adjExpense.setIsSystem(true);
        adjExpense.setIsActive(true);
        adjExpense.setSortOrder(99);
        try { categoryMapper.insert(adjExpense); } catch (Exception e) { log.warn("Category 59 already exists"); }

        // 子分类
        String[][] subCategories = {
                // 支出子分类: [父分类, 名称, icon]
                {"餐饮", "外卖", "takeout"},
                {"餐饮", "聚餐", "dining"},
                {"餐饮", "食材", "grocery"},
                {"餐饮", "饮品", "drink"},
                {"交通", "公交地铁", "bus"},
                {"交通", "打车", "taxi"},
                {"交通", "加油", "fuel"},
                {"交通", "停车", "parking"},
                {"住房", "房租/房贷", "rent"},
                {"住房", "水电燃气", "utility"},
                {"住房", "物业", "property"},
                {"住房", "维修", "repair"},
                {"购物", "日用品", "daily"},
                {"购物", "服饰", "clothes"},
                {"购物", "数码", "digital"},
                {"购物", "家具家电", "furniture"},
                {"医疗", "门诊", "clinic"},
                {"医疗", "药品", "medicine"},
                {"医疗", "体检", "checkup"},
                {"医疗", "保险", "insurance"},
                {"教育", "课程", "course"},
                {"教育", "书籍", "book"},
                {"教育", "考试", "exam"},
                {"娱乐", "电影", "movie"},
                {"娱乐", "游戏", "game"},
                {"娱乐", "旅行", "travel"},
                {"娱乐", "运动", "sport"},
                {"社交", "礼金", "gift"},
                {"社交", "聚会", "party"},
                {"金融", "利息", "interest"},
                {"金融", "手续费", "fee"},
                {"金融", "投资亏损", "loss"},
                // 收入子分类
                {"工资", "基本工资", "base-salary"},
                {"工资", "奖金", "bonus"},
                {"工资", "补贴", "allowance"},
                {"副业", "兼职", "parttime"},
                {"副业", "稿费", "article"},
                {"副业", "咨询", "consult"},
                {"投资", "股息", "dividend"},
                {"投资", "利息", "interest-income"},
                {"投资", "理财收益", "wealth"},
        };

        for (String[] row : subCategories) {
            Long parentId = parentIdMap.get(row[0]);
            if (parentId == null) {
                log.warn("Parent category '{}' not found, skipping sub-category '{}'", row[0], row[1]);
                continue;
            }
            Category sub = new Category();
            sub.setUserId(0L);
            sub.setParentId(parentId);
            sub.setName(row[1]);
            sub.setType(row[0].equals("工资") || row[0].equals("副业") || row[0].equals("投资") ? 2 : 1);
            sub.setIcon(row[2]);
            sub.setIsSystem(true);
            sub.setIsActive(true);
            sub.setSortOrder(0);
            categoryMapper.insert(sub);
        }

        log.info("Initialized {} system categories", parentIdMap.size() + subCategories.length + 3);
    }

    /**
     * 初始化系统配置
     */
    private void initSystemConfig() {
        long existing = configMapper.selectCount(null);
        if (existing > 0) {
            log.info("System config already exists (count={}), skipping init", existing);
            return;
        }

        String[][] configs = {
                {"base_currency", "CNY", "本位币"},
                {"supported_currencies", "CNY,USD,EUR,GBP,JPY,HKD,SGD,THB,KRW", "支持的币种列表"},
                {"month_start_day", "1", "月度统计起始日"},
        };

        for (String[] row : configs) {
            Config cfg = new Config();
            cfg.setConfigKey(row[0]);
            cfg.setConfigValue(row[1]);
            cfg.setDescription(row[2]);
            configMapper.insert(cfg);
        }

        log.info("Initialized {} system configs", configs.length);
    }
}
