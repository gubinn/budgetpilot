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
import java.util.List;
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
        // 通过 mapper 上带 @InterceptorIgnore 的查询判断是否已初始化。
        // 不能在本类内部直接调用 selectCount：@InterceptorIgnore 在同类调用时不生效，
        // 租户拦截器会把 user_id=0 的系统分类过滤掉，导致每次启动重复插入一整套分类。
        List<Category> existing = categoryMapper.selectSystemDefaults();
        if (!existing.isEmpty()) {
            log.info("System categories already exist (count={}), skipping init", existing.size());
            return;
        }

        // 支出大类（预算属性见子分类；固定/专项由分类属性控制，不再单列一级）
        String[][] expenseParents = {
                {"餐饮", "food", "#FF6B6B", "1"},
                {"交通", "transport", "#4ECDC4", "2"},
                {"居住", "home", "#45B7D1", "3"},
                {"购物", "shopping", "#96CEB4", "4"},
                {"医疗", "medical", "#FFEAA7", "5"},
                {"教育", "education", "#DDA0DD", "6"},
                {"休闲", "entertainment", "#98D8C8", "7"},
                {"通讯订阅", "phone", "#74B9FF", "8"},
                {"人情往来", "social", "#F7DC6F", "9"},
                {"保险", "insurance", "#E17055", "10"},
                {"其他支出", "other-expense", "#BDC3C7", "11"},
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
            cat.setBudgetType(row[0].equals("保险") ? "FIXED" : "DAILY");
            categoryMapper.insert(cat);
            parentIdMap.put(row[0], cat.getId());
        }

        // 收入大类
        String[][] incomeParents = {
                {"工资", "salary", "#2ECC71", "1"},
                {"公积金", "provident-fund", "#27AE60", "2"},
                {"投资", "investment", "#1ABC9C", "3"},
                {"其他收入", "other-income", "#BDC3C7", "4"},
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
            cat.setBudgetType("NONE");
            categoryMapper.insert(cat);
            parentIdMap.put(row[0], cat.getId());
        }

        // 余额调整（系统项，动态按名称查找，不再硬编码 ID）
        String[][] adjustments = {
                {"余额调整收入", "adjust", "#FF9800", "2"},
                {"余额调整支出", "adjust", "#FF9800", "1"},
        };
        for (String[] row : adjustments) {
            Category cat = new Category();
            cat.setUserId(0L);
            cat.setParentId(0L);
            cat.setName(row[0]);
            cat.setType(Integer.parseInt(row[3]));
            cat.setIcon(row[1]);
            cat.setColor(row[2]);
            cat.setIsSystem(true);
            cat.setIsActive(true);
            cat.setSortOrder(99);
            cat.setBudgetType("NONE");
            categoryMapper.insert(cat);
            parentIdMap.put(row[0], cat.getId());
        }

        // 往来款/借出：不计日常收支，用于待复核的对外转账
        Category loanParent = new Category();
        loanParent.setUserId(0L);
        loanParent.setParentId(0L);
        loanParent.setName("往来款");
        loanParent.setType(1);
        loanParent.setIcon("transfer");
        loanParent.setColor("#95A5A6");
        loanParent.setIsSystem(true);
        loanParent.setIsActive(true);
        loanParent.setSortOrder(12);
        loanParent.setBudgetType("NONE");
        categoryMapper.insert(loanParent);
        parentIdMap.put("往来款", loanParent.getId());

        Category loan = new Category();
        loan.setUserId(0L);
        loan.setParentId(loanParent.getId());
        loan.setName("借出");
        loan.setType(1);
        loan.setIcon("transfer");
        loan.setColor("#95A5A6");
        loan.setIsSystem(true);
        loan.setIsActive(true);
        loan.setSortOrder(1);
        loan.setBudgetType("NONE");
        categoryMapper.insert(loan);

        // 子分类: [父分类, 名称, icon, budgetType]
        String[][] subCategories = {
                {"餐饮", "外卖", "takeout", "DAILY"},
                {"餐饮", "堂食", "dining", "DAILY"},
                {"餐饮", "食材", "grocery", "DAILY"},
                {"餐饮", "饮品", "drink", "DAILY"},
                {"餐饮", "其他", "more", "DAILY"},
                {"交通", "公交地铁", "bus", "DAILY"},
                {"交通", "打车", "taxi", "DAILY"},
                {"交通", "加油", "fuel", "DAILY"},
                {"交通", "停车", "parking", "DAILY"},
                {"交通", "其他", "more", "DAILY"},
                {"居住", "房租", "rent", "FIXED"},
                {"居住", "房贷", "mortgage", "FIXED"},
                {"居住", "水电燃气", "utility", "DAILY"},
                {"居住", "物业维修", "property", "DAILY"},
                {"居住", "其他", "more", "DAILY"},
                {"购物", "日用品", "daily", "DAILY"},
                {"购物", "服饰", "clothes", "DAILY"},
                {"购物", "数码家电", "digital", "DAILY"},
                {"购物", "大额购物", "big-shopping", "SPECIAL"},
                {"购物", "其他", "more", "DAILY"},
                {"医疗", "门诊", "clinic", "DAILY"},
                {"医疗", "药品", "medicine", "DAILY"},
                {"医疗", "体检", "checkup", "DAILY"},
                {"医疗", "其他", "more", "DAILY"},
                {"教育", "课程", "course", "DAILY"},
                {"教育", "书籍", "book", "DAILY"},
                {"教育", "考试", "exam", "DAILY"},
                {"教育", "学费", "tuition", "SPECIAL"},
                {"教育", "其他", "more", "DAILY"},
                {"休闲", "电影游戏", "movie", "DAILY"},
                {"休闲", "运动", "sport", "DAILY"},
                {"休闲", "旅行", "travel", "SPECIAL"},
                {"休闲", "其他", "more", "DAILY"},
                {"通讯订阅", "话费宽带", "phone", "DAILY"},
                {"通讯订阅", "AI订阅", "ai", "DAILY"},
                {"通讯订阅", "存储订阅", "cloud", "DAILY"},
                {"通讯订阅", "其他", "more", "DAILY"},
                {"人情往来", "礼金", "gift", "DAILY"},
                {"人情往来", "聚会", "party", "DAILY"},
                {"人情往来", "亲属转账", "transfer", "DAILY"},
                {"人情往来", "其他", "more", "DAILY"},
                // 收入子分类
                {"工资", "基本工资", "base-salary", "NONE"},
                {"工资", "奖金补贴", "bonus", "NONE"},
                {"投资", "利息", "interest-income", "NONE"},
                {"投资", "股息", "dividend", "NONE"},
                {"投资", "理财收益", "wealth", "NONE"},
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
            sub.setType(row[0].equals("工资") || row[0].equals("投资") ? 2 : 1);
            sub.setIcon(row[2]);
            sub.setIsSystem(true);
            sub.setIsActive(true);
            sub.setSortOrder(0);
            sub.setBudgetType(row[3]);
            categoryMapper.insert(sub);
        }

        log.info("Initialized {} system categories", parentIdMap.size() + subCategories.length + 1);
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
