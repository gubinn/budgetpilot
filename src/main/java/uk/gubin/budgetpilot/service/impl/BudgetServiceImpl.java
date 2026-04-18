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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

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

        List<Long> budgetCatIds = new ArrayList<>();
        for (BudgetItem item : budgetItems) {
            budgetCatIds.add(item.getCategoryId());
            budgetCatIds.addAll(getChildCategoryIds(item.getCategoryId()));
        }

        BigDecimal totalSpent;
        if (budgetCatIds.isEmpty()) {
            totalSpent = BigDecimal.ZERO;
        } else {
            LambdaQueryWrapper<Transaction> totalQuery = new LambdaQueryWrapper<>();
            totalQuery.eq(Transaction::getType, 1)
                    .eq(Transaction::getIsConfirmed, true)
                    .in(Transaction::getCategoryId, budgetCatIds)
                    .ge(Transaction::getTransactionDate, start)
                    .le(Transaction::getTransactionDate, end);
            List<Transaction> txs = transactionMapper.selectList(totalQuery);
            totalSpent = txs.stream()
                    .map(Transaction::getAmountBase)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
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
    @Transactional
    public Budget update(String yearMonth, BudgetUpdateDTO dto) {
        Budget budget = getBudgetByMonth(yearMonth);
        if (budget == null) {
            throw new BizException(ErrorCode.BUDGET_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(budget.getIsLocked())) {
            throw new BizException(ErrorCode.BUDGET_LOCKED);
        }

        if (dto.getTotalAmount() != null) {
            budget.setTotalAmount(dto.getTotalAmount());
        }
        if (dto.getNote() != null) {
            budget.setNote(dto.getNote());
        }
        baseMapper.updateById(budget);

        // 更新明细
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (BudgetUpdateDTO.BudgetItemDTO itemDTO : dto.getItems()) {
                LambdaQueryWrapper<BudgetItem> query = new LambdaQueryWrapper<>();
                query.eq(BudgetItem::getBudgetId, budget.getId())
                        .eq(BudgetItem::getCategoryId, itemDTO.getCategoryId());
                BudgetItem item = budgetItemMapper.selectOne(query);
                if (item == null) {
                    item = new BudgetItem();
                    item.setBudgetId(budget.getId());
                    item.setCategoryId(itemDTO.getCategoryId());
                    item.setAmount(itemDTO.getAmount());
                    item.setSpent(BigDecimal.ZERO);
                    budgetItemMapper.insert(item);
                } else {
                    item.setAmount(itemDTO.getAmount());
                    budgetItemMapper.updateById(item);
                }
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

        LambdaQueryWrapper<BudgetItem> query = new LambdaQueryWrapper<>();
        query.eq(BudgetItem::getBudgetId, budget.getId())
                .eq(BudgetItem::getCategoryId, categoryId);
        return budgetItemMapper.selectOne(query);
    }

    @Override
    @Transactional
    public void updateItemSpent(Long categoryId, String yearMonth, BigDecimal spentDelta) {
        Budget budget = getBudgetByMonth(yearMonth);
        if (budget == null) return;

        LambdaQueryWrapper<BudgetItem> query = new LambdaQueryWrapper<>();
        query.eq(BudgetItem::getBudgetId, budget.getId())
                .eq(BudgetItem::getCategoryId, categoryId);
        BudgetItem item = budgetItemMapper.selectOne(query);
        if (item != null) {
            item.setSpent(item.getSpent().add(spentDelta));
            budgetItemMapper.updateById(item);
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

    private Budget getBudgetByMonth(String yearMonth) {
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

        List<BudgetProgressVO.ItemProgress> result = new ArrayList<>();
        for (BudgetItem item : items) {
            BudgetProgressVO.ItemProgress ip = new BudgetProgressVO.ItemProgress();
            ip.setCategoryId(item.getCategoryId());
            ip.setBudget(item.getAmount());

            // 获取一级分类及其子分类
            List<Long> catIds = new ArrayList<>();
            catIds.add(item.getCategoryId());
            catIds.addAll(getChildCategoryIds(item.getCategoryId()));

            // 聚合已消费
            LambdaQueryWrapper<Transaction> spentQuery = new LambdaQueryWrapper<>();
            spentQuery.eq(Transaction::getType, 1)
                    .eq(Transaction::getIsConfirmed, true)
                    .in(Transaction::getCategoryId, catIds)
                    .ge(Transaction::getTransactionDate, start)
                    .le(Transaction::getTransactionDate, end);
            List<Transaction> txs = transactionMapper.selectList(spentQuery);
            BigDecimal spent = txs.stream()
                    .map(Transaction::getAmountBase)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            ip.setSpent(spent);
            ip.setRemaining(item.getAmount().subtract(spent));
            ip.setProgressPct(spent.divide(item.getAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));

            // 计算状态
            Category cat = categoryMapper.selectById(item.getCategoryId());
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

    private List<Long> getChildCategoryIds(Long parentId) {
        LambdaQueryWrapper<Category> query = new LambdaQueryWrapper<>();
        query.eq(Category::getParentId, parentId).select(Category::getId);
        return categoryMapper.selectList(query).stream().map(Category::getId).toList();
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
