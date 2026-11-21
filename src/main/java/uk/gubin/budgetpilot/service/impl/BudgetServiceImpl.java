package uk.gubin.budgetpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.dto.BudgetCreateDTO;
import uk.gubin.budgetpilot.dto.BudgetUpdateDTO;
import uk.gubin.budgetpilot.entity.Budget;
import uk.gubin.budgetpilot.entity.BudgetItem;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.BudgetItemMapper;
import uk.gubin.budgetpilot.mapper.BudgetMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.service.BudgetService;
import uk.gubin.budgetpilot.vo.BudgetProgressVO;
import uk.gubin.budgetpilot.vo.FixedExpenseVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl extends ServiceImpl<BudgetMapper, Budget> implements BudgetService {

    private final BudgetItemMapper budgetItemMapper;
    private final CategoryMapper categoryMapper;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional
    public Budget create(BudgetCreateDTO dto) {
        if (exists(dto.getYearMonth())) {
            throw new BizException(ErrorCode.BUDGET_MONTH_DUPLICATE);
        }

        // 校验：分类必须有效，且父分类与子分类不能同时设置（否则父子会重复计算）
        validateBudgetCategories(dto.getItems() == null ? List.of()
                : dto.getItems().stream().map(BudgetCreateDTO.BudgetItemDTO::getCategoryId).toList());

        // 校验：总预算应等于明细之和
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            BigDecimal itemsSum = dto.getItems().stream()
                    .map(BudgetCreateDTO.BudgetItemDTO::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (itemsSum.compareTo(dto.getTotalAmount()) != 0) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "预算总额(" + dto.getTotalAmount() + ")与分类明细之和(" + itemsSum + ")不匹配");
            }
        }

        Budget budget = new Budget();
        budget.setYearMonth(dto.getYearMonth());
        budget.setTotalAmount(dto.getTotalAmount());
        budget.setNote(dto.getNote());
        budget.setIsLocked(false);
        baseMapper.insert(budget);

        // 创建预算明细
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (BudgetCreateDTO.BudgetItemDTO itemDTO : dto.getItems()) {
                BudgetItem item = new BudgetItem();
                item.setBudgetId(budget.getId());
                item.setCategoryId(itemDTO.getCategoryId());
                item.setAmount(itemDTO.getAmount());
                item.setSpent(BigDecimal.ZERO);
                budgetItemMapper.insert(item);
            }
        }

        log.info("Created budget for {}", dto.getYearMonth());
        return budget;
    }

    @Override
    public BudgetProgressVO getProgress(String yearMonth) {
        Budget budget = getBudgetByMonth(yearMonth);
        if (budget == null) {
            throw new BizException(ErrorCode.BUDGET_NOT_FOUND);
        }

        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate today = LocalDate.now();
        int daysTotal = ym.lengthOfMonth();
        int daysPassed = (int) ChronoUnit.DAYS.between(ym.atDay(1), today) + 1;
        daysPassed = Math.min(daysPassed, daysTotal);
        daysPassed = Math.max(daysPassed, 1);

        // 计算总已消费（仅统计预算内分类的交易）
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // 获取预算包含的所有分类ID（含子分类）
        LambdaQueryWrapper<BudgetItem> itemQuery = new LambdaQueryWrapper<>();
        itemQuery.eq(BudgetItem::getBudgetId, budget.getId());
        List<BudgetItem> budgetItems = budgetItemMapper.selectList(itemQuery);

        Map<Long, Category> allCats = loadAllCategories();
        Set<Long> budgetCatIds = collectBudgetCategoryIds(budgetItems, "DAILY", allCats);

        BigDecimal totalSpent;
        if (budgetCatIds.isEmpty()) {
            totalSpent = BigDecimal.ZERO;
        } else {
            totalSpent = sumNetExpense(budgetCatIds, start, end);
        }

        BigDecimal remaining = budget.getTotalAmount().subtract(totalSpent);
        BigDecimal progressPct = totalSpent.divide(budget.getTotalAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);

        BigDecimal dailyAvgSpent = totalSpent.divide(BigDecimal.valueOf(daysPassed), 2, RoundingMode.HALF_UP);
        BigDecimal dailyAvgRemaining = remaining.max(BigDecimal.ZERO)
                .divide(BigDecimal.valueOf(daysTotal - daysPassed + 1), 2, RoundingMode.HALF_UP);

        BudgetProgressVO vo = new BudgetProgressVO();
        vo.setYearMonth(yearMonth);
        vo.setBudgetId(budget.getId());
        vo.setNote(budget.getNote());
        vo.setTotalBudget(budget.getTotalAmount());
        vo.setTotalSpent(totalSpent);
        vo.setRemaining(remaining);
        vo.setProgressPct(progressPct);
        vo.setDaysPassed(daysPassed);
        vo.setDaysTotal(daysTotal);
        vo.setDailyAvgSpent(dailyAvgSpent);
        vo.setDailyAvgRemaining(dailyAvgRemaining);
        vo.setIsLocked(budget.getIsLocked());

        // 计算分类明细进度
        List<BudgetProgressVO.ItemProgress> itemProgressList = calculateItemProgress(budget, yearMonth);
        vo.setItems(itemProgressList);

        return vo;
    }

    @Override
    public List<FixedExpenseVO> fixedSummary(String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        Map<Long, Category> categories = loadAllCategories();
        List<Category> fixed = categories.values().stream()
                .filter(c -> c.getType() != null && c.getType() == 1 && "FIXED".equals(c.getBudgetType()))
                .sorted(java.util.Comparator.comparing(Category::getSortOrder,
                        java.util.Comparator.nullsLast(Integer::compareTo)))
                .toList();
        Set<Long> fixedIds = fixed.stream().map(Category::getId).collect(Collectors.toSet());
        if (fixedIds.isEmpty()) return List.of();

        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.in(Transaction::getType, 1, 4)
                .eq(Transaction::getIsConfirmed, true)
                .eq(Transaction::getReviewStatus, "CONFIRMED")
                .in(Transaction::getCategoryId, fixedIds)
                .ge(Transaction::getTransactionDate, start)
                .le(Transaction::getTransactionDate, end);
        Map<Long, BigDecimal> spent = transactionMapper.selectList(query).stream()
                .collect(Collectors.groupingBy(Transaction::getCategoryId,
                        Collectors.reducing(BigDecimal.ZERO,
                                tx -> tx.getType() == 4 ? tx.getAmountBase().negate() : tx.getAmountBase(),
                                BigDecimal::add)));

        return fixed.stream().map(category -> {
            FixedExpenseVO vo = new FixedExpenseVO();
            vo.setCategoryId(category.getId());
            vo.setCategoryName(category.getName());
            vo.setCategoryColor(category.getColor());
            vo.setCategoryIcon(category.getIcon());
            vo.setSpent(spent.getOrDefault(category.getId(), BigDecimal.ZERO));
            return vo;
        }).toList();
    }

    @Override
    @Transactional
    public Budget update(String yearMonth, BudgetUpdateDTO dto) {
        Budget budget = getBudgetByMonth(yearMonth);
        if (budget == null) {
            throw new BizException(ErrorCode.BUDGET_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(budget.getIsLocked())) {
            throw new BizException(ErrorCode.BUDGET_LOCKED);
        }

        // 防止客户端误传空明细导致整月预算项被清空
        if (dto.getItems() != null && dto.getItems().isEmpty()) {
            Long existingItems = budgetItemMapper.selectCount(
                    new LambdaQueryWrapper<BudgetItem>().eq(BudgetItem::getBudgetId, budget.getId()));
            if (existingItems != null && existingItems > 0) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "预算分类明细不能为空；请至少保留一项");
            }
        }

        // 校验：分类必须有效，且父分类与子分类不能同时设置（否则父子会重复计算）
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            validateBudgetCategories(dto.getItems().stream()
                    .map(BudgetUpdateDTO.BudgetItemDTO::getCategoryId).toList());
        }

        // 校验：如果传入了明细，总预算应等于明细之和
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            BigDecimal itemsSum = dto.getItems().stream()
                    .map(BudgetUpdateDTO.BudgetItemDTO::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal compareTotal = dto.getTotalAmount() != null ? dto.getTotalAmount() : budget.getTotalAmount();
            if (itemsSum.compareTo(compareTotal) != 0) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "预算总额与分类明细之和不匹配");
            }
        }

        if (dto.getTotalAmount() != null) {
            budget.setTotalAmount(dto.getTotalAmount());
        }
        if (dto.getNote() != null) {
            budget.setNote(dto.getNote());
        }
        baseMapper.updateById(budget);

        // 更新明细：先删除旧的，再插入新的
        if (dto.getItems() != null) {
            budgetItemMapper.delete(new LambdaQueryWrapper<BudgetItem>().eq(BudgetItem::getBudgetId, budget.getId()));
            for (BudgetUpdateDTO.BudgetItemDTO itemDTO : dto.getItems()) {
                BudgetItem item = new BudgetItem();
                item.setBudgetId(budget.getId());
                item.setCategoryId(itemDTO.getCategoryId());
                item.setAmount(itemDTO.getAmount());
                item.setSpent(BigDecimal.ZERO);
                budgetItemMapper.insert(item);
            }
        }

        return budget;
    }

    @Override
    @Transactional
    public Budget copyBudget(String yearMonth, String sourceMonth) {
        if (exists(yearMonth)) {
            throw new BizException(ErrorCode.BUDGET_MONTH_DUPLICATE);
        }

        Budget source = getBudgetByMonth(sourceMonth);
        if (source == null) {
            throw new BizException(ErrorCode.BUDGET_NOT_FOUND);
        }

        Budget budget = new Budget();
        budget.setYearMonth(yearMonth);
        budget.setTotalAmount(source.getTotalAmount());
        budget.setNote(source.getNote());
        budget.setIsLocked(false);
        baseMapper.insert(budget);

        // 复制明细
        LambdaQueryWrapper<BudgetItem> query = new LambdaQueryWrapper<>();
        query.eq(BudgetItem::getBudgetId, source.getId());
        List<BudgetItem> sourceItems = budgetItemMapper.selectList(query);
        for (BudgetItem sourceItem : sourceItems) {
            BudgetItem newItem = new BudgetItem();
            newItem.setBudgetId(budget.getId());
            newItem.setCategoryId(sourceItem.getCategoryId());
            newItem.setAmount(sourceItem.getAmount());
            newItem.setSpent(BigDecimal.ZERO);
            budgetItemMapper.insert(newItem);
        }

        log.info("Copied budget from {} to {}", sourceMonth, yearMonth);
        return budget;
    }

    @Override
    public BudgetItem getItem(Long categoryId, String yearMonth) {
        Budget budget = getBudgetByMonth(yearMonth);
        if (budget == null) return null;

        // 预算可以设在任意一级分类：优先精确匹配，找不到再向上找父分类。
        for (Long id : categoryPathIds(categoryId)) {
            LambdaQueryWrapper<BudgetItem> query = new LambdaQueryWrapper<>();
            query.eq(BudgetItem::getBudgetId, budget.getId())
                    .eq(BudgetItem::getCategoryId, id);
            BudgetItem item = budgetItemMapper.selectOne(query);
            if (item != null) return item;
        }
        return null;
    }

    @Override
    @Transactional
    public void updateItemSpent(Long categoryId, String yearMonth, BigDecimal spentDelta) {
        Budget budget = getBudgetByMonth(yearMonth);
        if (budget == null) return;

        Category source = categoryMapper.selectById(categoryId);
        // 固定/专项/非预算流水不进入月度预算；专项年度预算按查询实时计算，不维护 denormalized spent。
        if (source == null || !"DAILY".equals(source.getBudgetType())) return;

        // 一笔交易可能同时影响“子分类预算”和“父分类预算”，但预算页会禁止父子同时设置；
        // 这里仍按分类链逐级更新，兼容历史数据。只更新 DAILY 预算项。
        for (Long id : categoryPathIds(categoryId)) {
            Category itemCategory = categoryMapper.selectById(id);
            if (itemCategory == null || !"DAILY".equals(itemCategory.getBudgetType())) continue;
            LambdaQueryWrapper<BudgetItem> query = new LambdaQueryWrapper<>();
            query.eq(BudgetItem::getBudgetId, budget.getId())
                    .eq(BudgetItem::getCategoryId, id);
            BudgetItem item = budgetItemMapper.selectOne(query);
            if (item != null) {
                item.setSpent(item.getSpent().add(spentDelta));
                budgetItemMapper.updateById(item);
            }
        }
    }

    @Override
    public boolean exists(String yearMonth) {
        LambdaQueryWrapper<Budget> query = new LambdaQueryWrapper<>();
        query.eq(Budget::getYearMonth, yearMonth);
        return count(query) > 0;
    }

    @Override
    @Transactional
    public void lockPreviousMonth() {
        String prevMonth = YearMonth.now().minusMonths(1).toString();
        LambdaQueryWrapper<Budget> query = new LambdaQueryWrapper<>();
        query.eq(Budget::getYearMonth, prevMonth);
        Budget budget = baseMapper.selectOne(query);
        if (budget != null && !Boolean.TRUE.equals(budget.getIsLocked())) {
            budget.setIsLocked(true);
            baseMapper.updateById(budget);
            log.info("Locked budget for {}", prevMonth);
        }
    }

    Budget getBudgetByMonth(String yearMonth) {
        LambdaQueryWrapper<Budget> query = new LambdaQueryWrapper<>();
        query.eq(Budget::getYearMonth, yearMonth);
        return baseMapper.selectOne(query);
    }

    private List<BudgetProgressVO.ItemProgress> calculateItemProgress(Budget budget, String yearMonth) {
        LambdaQueryWrapper<BudgetItem> query = new LambdaQueryWrapper<>();
        query.eq(BudgetItem::getBudgetId, budget.getId());
        List<BudgetItem> items = budgetItemMapper.selectList(query);

        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // 批量加载全部分类，按预算属性过滤
        Map<Long, Category> catMap = loadAllCategories();
        Map<Long, BigDecimal> catSpentMap = new HashMap<>();
        for (BudgetItem item : items) {
            Set<Long> ids = collectBudgetCategoryIds(List.of(item), "DAILY", catMap);
            BigDecimal net = sumNetExpense(ids, start, end);
            catSpentMap.put(item.getCategoryId(), net);
        }

        List<BudgetProgressVO.ItemProgress> result = new ArrayList<>();
        for (BudgetItem item : items) {
            BudgetProgressVO.ItemProgress ip = new BudgetProgressVO.ItemProgress();
            ip.setCategoryId(item.getCategoryId());
            ip.setBudget(item.getAmount());

            BigDecimal spent = catSpentMap.getOrDefault(item.getCategoryId(), BigDecimal.ZERO);
            ip.setSpent(spent);
            ip.setRemaining(item.getAmount().subtract(spent));
            ip.setProgressPct(spent.divide(item.getAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));

            // 从 Map 中获取分类信息
            Category cat = catMap.get(item.getCategoryId());
            if (cat != null) {
                ip.setCategoryName(cat.getName());
                ip.setCategoryIcon(cat.getIcon());
                ip.setCategoryColor(cat.getColor());
            }

            ip.setStatus(calculateStatus(ip.getProgressPct(), yearMonth));
            result.add(ip);
        }

        return result;
    }

    private Map<Long, Category> loadAllCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
    }

    /**
     * 收集预算项命中的分类：分类自身 + 后代中预算属性匹配的分类。
     * 例如“居住”月度预算只包含 DAILY 的水电/物业，不包含 FIXED 的房租/房贷。
     */
    private Set<Long> collectBudgetCategoryIds(List<BudgetItem> items, String budgetType,
                                               Map<Long, Category> categories) {
        Set<Long> result = new LinkedHashSet<>();
        for (BudgetItem item : items) {
            Category root = categories.get(item.getCategoryId());
            if (root == null || !budgetType.equals(root.getBudgetType())) continue;
            result.add(root.getId());
            collectDescendants(root.getId(), budgetType, categories, result);
        }
        return result;
    }

    private void collectDescendants(Long parentId, String budgetType,
                                    Map<Long, Category> categories, Set<Long> result) {
        for (Category category : categories.values()) {
            if (!parentId.equals(category.getParentId())) continue;
            if (budgetType.equals(category.getBudgetType())) {
                result.add(category.getId());
            }
            collectDescendants(category.getId(), budgetType, categories, result);
        }
    }

    /**
     * 净支出 = 支出(type=1) - 退款(type=4)；只统计已复核且已确认的交易。
     */
    private BigDecimal sumNetExpense(Set<Long> categoryIds, LocalDate start, LocalDate end) {
        if (categoryIds.isEmpty()) return BigDecimal.ZERO;
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.in(Transaction::getType, 1, 4)
                .eq(Transaction::getIsConfirmed, true)
                .eq(Transaction::getReviewStatus, "CONFIRMED")
                .in(Transaction::getCategoryId, categoryIds)
                .ge(Transaction::getTransactionDate, start)
                .le(Transaction::getTransactionDate, end);
        return transactionMapper.selectList(query).stream()
                .map(tx -> tx.getType() == 4 ? tx.getAmountBase().negate() : tx.getAmountBase())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 校验预算分类：必须是当前用户的支出分类，且不能同时设置父分类和子分类。
     * 父分类预算本身已经包含子分类消费，父子同时设置会导致总额重复计算。
     */
    private void validateBudgetCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return;

        Set<Long> uniqueIds = new LinkedHashSet<>(categoryIds);
        if (uniqueIds.size() != categoryIds.size()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "预算分类不能重复");
        }

        Map<Long, Category> categories = categoryMapper.selectBatchIds(uniqueIds).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
        for (Long id : uniqueIds) {
            Category category = categories.get(id);
            if (category == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "预算分类不存在: " + id);
            }
            if (category.getType() == null || category.getType() != 1) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "预算只能设置在支出分类: " + category.getName());
            }
            if (!"DAILY".equals(category.getBudgetType())) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "月度预算只能设置在日常分类: " + category.getName()
                                + "（固定/专项请使用对应预算页）");
            }
        }

        for (Long id : uniqueIds) {
            Category category = categories.get(id);
            Long parentId = category.getParentId();
            Set<Long> seen = new HashSet<>();
            while (parentId != null && parentId > 0 && seen.add(parentId)) {
                if (uniqueIds.contains(parentId)) {
                    Category parent = categories.get(parentId);
                    String parentName = parent != null ? parent.getName() : String.valueOf(parentId);
                    throw new BizException(ErrorCode.PARAM_ERROR,
                            "父分类「" + parentName + "」与子分类「" + category.getName()
                                    + "」不能同时设置预算，请只保留一层");
                }
                Category parent = categoryMapper.selectById(parentId);
                parentId = parent == null ? null : parent.getParentId();
            }
        }
    }

    /**
     * 返回分类自身到根分类的 ID 链，用于预算父/子分类的命中与汇总。
     */
    private List<Long> categoryPathIds(Long categoryId) {
        List<Long> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        Long current = categoryId;
        while (current != null && current > 0 && seen.add(current)) {
            result.add(current);
            Category category = categoryMapper.selectById(current);
            current = category == null ? null : category.getParentId();
        }
        return result;
    }

    private String calculateStatus(BigDecimal progressPct, String yearMonth) {
        if (progressPct.compareTo(BigDecimal.valueOf(100)) > 0) return "EXCEEDED";
        if (progressPct.compareTo(BigDecimal.valueOf(80)) > 0) return "CAUTION";

        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate today = LocalDate.now();
        int daysTotal = ym.lengthOfMonth();
        int daysPassed = (int) ChronoUnit.DAYS.between(ym.atDay(1), today) + 1;
        daysPassed = Math.min(daysPassed, daysTotal);

        BigDecimal timeProgress = BigDecimal.valueOf(daysPassed)
                .divide(BigDecimal.valueOf(daysTotal), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // 消费进度 > 时间进度 * 1.2 → WARNING
        if (progressPct.compareTo(timeProgress.multiply(BigDecimal.valueOf(1.2))) > 0) return "WARNING";

        return "NORMAL";
    }
}
