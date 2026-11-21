package uk.gubin.budgetpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.dto.AnnualBudgetCreateDTO;
import uk.gubin.budgetpilot.dto.AnnualBudgetUpdateDTO;
import uk.gubin.budgetpilot.entity.AnnualBudget;
import uk.gubin.budgetpilot.entity.AnnualBudgetItem;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.AnnualBudgetItemMapper;
import uk.gubin.budgetpilot.mapper.AnnualBudgetMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.service.AnnualBudgetService;
import uk.gubin.budgetpilot.vo.AnnualBudgetProgressVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
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
public class AnnualBudgetServiceImpl extends ServiceImpl<AnnualBudgetMapper, AnnualBudget>
        implements AnnualBudgetService {

    private final AnnualBudgetItemMapper annualBudgetItemMapper;
    private final CategoryMapper categoryMapper;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional
    public AnnualBudget create(AnnualBudgetCreateDTO dto) {
        validateYear(dto.getYear());
        if (exists(dto.getYear())) {
            throw new BizException(ErrorCode.ANNUAL_BUDGET_DUPLICATE);
        }

        List<Long> categoryIds = dto.getItems() == null ? List.of()
                : dto.getItems().stream().map(AnnualBudgetCreateDTO.BudgetItemDTO::getCategoryId).toList();
        validateBudgetCategories(categoryIds);
        validateTotal(dto.getTotalAmount(), dto.getItems() == null ? List.of()
                : dto.getItems().stream().map(AnnualBudgetCreateDTO.BudgetItemDTO::getAmount).toList());

        AnnualBudget budget = new AnnualBudget();
        budget.setBudgetYear(dto.getYear());
        budget.setTotalAmount(dto.getTotalAmount());
        budget.setNote(dto.getNote());
        budget.setIsLocked(false);
        baseMapper.insert(budget);

        if (dto.getItems() != null) {
            for (AnnualBudgetCreateDTO.BudgetItemDTO itemDTO : dto.getItems()) {
                AnnualBudgetItem item = new AnnualBudgetItem();
                item.setAnnualBudgetId(budget.getId());
                item.setCategoryId(itemDTO.getCategoryId());
                item.setAmount(itemDTO.getAmount());
                annualBudgetItemMapper.insert(item);
            }
        }

        log.info("Created annual budget for {}", dto.getYear());
        return budget;
    }

    @Override
    public AnnualBudgetProgressVO getProgress(String year) {
        validateYear(year);
        AnnualBudget budget = getBudgetByYear(year);
        if (budget == null) {
            throw new BizException(ErrorCode.ANNUAL_BUDGET_NOT_FOUND);
        }

        LocalDate start = Year.parse(year).atDay(1);
        LocalDate end = Year.parse(year).atMonth(12).atEndOfMonth();

        LambdaQueryWrapper<AnnualBudgetItem> itemQuery = new LambdaQueryWrapper<>();
        itemQuery.eq(AnnualBudgetItem::getAnnualBudgetId, budget.getId());
        List<AnnualBudgetItem> items = annualBudgetItemMapper.selectList(itemQuery);

        Map<Long, Category> allCats = loadAllCategories();
        Map<Long, BigDecimal> catSpentMap = new HashMap<>();
        BigDecimal totalSpent = BigDecimal.ZERO;
        for (AnnualBudgetItem item : items) {
            Set<Long> ids = collectBudgetCategoryIds(item, "SPECIAL", allCats);
            BigDecimal net = sumNetExpense(ids, start, end);
            catSpentMap.put(item.getCategoryId(), net);
            totalSpent = totalSpent.add(net);
        }

        BigDecimal remaining = budget.getTotalAmount().subtract(totalSpent);
        BigDecimal progressPct = budget.getTotalAmount().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalSpent.divide(budget.getTotalAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);

        Map<Long, Category> catMap = allCats;

        AnnualBudgetProgressVO vo = new AnnualBudgetProgressVO();
        vo.setYear(year);
        vo.setAnnualBudgetId(budget.getId());
        vo.setNote(budget.getNote());
        vo.setTotalBudget(budget.getTotalAmount());
        vo.setTotalSpent(totalSpent);
        vo.setRemaining(remaining);
        vo.setProgressPct(progressPct);
        vo.setIsLocked(budget.getIsLocked());

        List<AnnualBudgetProgressVO.ItemProgress> itemProgressList = new ArrayList<>();
        for (AnnualBudgetItem item : items) {
            BigDecimal spent = catSpentMap.getOrDefault(item.getCategoryId(), BigDecimal.ZERO);

            AnnualBudgetProgressVO.ItemProgress ip = new AnnualBudgetProgressVO.ItemProgress();
            ip.setCategoryId(item.getCategoryId());
            ip.setBudget(item.getAmount());
            ip.setSpent(spent);
            ip.setRemaining(item.getAmount().subtract(spent));
            ip.setProgressPct(spent.divide(item.getAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));
            ip.setStatus(calculateStatus(ip.getProgressPct()));

            Category category = catMap.get(item.getCategoryId());
            if (category != null) {
                ip.setCategoryName(category.getName());
                ip.setCategoryIcon(category.getIcon());
                ip.setCategoryColor(category.getColor());
            }
            itemProgressList.add(ip);
        }
        vo.setItems(itemProgressList);
        return vo;
    }

    @Override
    @Transactional
    public AnnualBudget update(String year, AnnualBudgetUpdateDTO dto) {
        validateYear(year);
        AnnualBudget budget = getBudgetByYear(year);
        if (budget == null) {
            throw new BizException(ErrorCode.ANNUAL_BUDGET_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(budget.getIsLocked())) {
            throw new BizException(ErrorCode.BUDGET_LOCKED);
        }

        if (dto.getItems() != null && dto.getItems().isEmpty()) {
            Long existingItems = annualBudgetItemMapper.selectCount(
                    new LambdaQueryWrapper<AnnualBudgetItem>()
                            .eq(AnnualBudgetItem::getAnnualBudgetId, budget.getId()));
            if (existingItems != null && existingItems > 0) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "年度预算分类明细不能为空；请至少保留一项");
            }
        }

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            List<Long> categoryIds = dto.getItems().stream()
                    .map(AnnualBudgetUpdateDTO.BudgetItemDTO::getCategoryId).toList();
            validateBudgetCategories(categoryIds);
            validateTotal(dto.getTotalAmount() != null ? dto.getTotalAmount() : budget.getTotalAmount(),
                    dto.getItems().stream().map(AnnualBudgetUpdateDTO.BudgetItemDTO::getAmount).toList());
        }

        if (dto.getTotalAmount() != null) {
            budget.setTotalAmount(dto.getTotalAmount());
        }
        if (dto.getNote() != null) {
            budget.setNote(dto.getNote());
        }
        baseMapper.updateById(budget);

        if (dto.getItems() != null) {
            annualBudgetItemMapper.delete(new LambdaQueryWrapper<AnnualBudgetItem>()
                    .eq(AnnualBudgetItem::getAnnualBudgetId, budget.getId()));
            for (AnnualBudgetUpdateDTO.BudgetItemDTO itemDTO : dto.getItems()) {
                AnnualBudgetItem item = new AnnualBudgetItem();
                item.setAnnualBudgetId(budget.getId());
                item.setCategoryId(itemDTO.getCategoryId());
                item.setAmount(itemDTO.getAmount());
                annualBudgetItemMapper.insert(item);
            }
        }

        return budget;
    }

    @Override
    public boolean exists(String year) {
        return count(new LambdaQueryWrapper<AnnualBudget>()
                .eq(AnnualBudget::getBudgetYear, year)) > 0;
    }

    private AnnualBudget getBudgetByYear(String year) {
        return baseMapper.selectOne(new LambdaQueryWrapper<AnnualBudget>()
                .eq(AnnualBudget::getBudgetYear, year));
    }

    private Map<Long, Category> loadAllCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
    }

    private Set<Long> collectBudgetCategoryIds(AnnualBudgetItem item, String budgetType,
                                               Map<Long, Category> categories) {
        Set<Long> result = new LinkedHashSet<>();
        Category root = categories.get(item.getCategoryId());
        if (root == null || !budgetType.equals(root.getBudgetType())) return result;
        result.add(root.getId());
        collectDescendants(root.getId(), budgetType, categories, result);
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

    private BigDecimal sumNetExpense(Set<Long> categoryIds, LocalDate start, LocalDate end) {
        if (categoryIds.isEmpty()) return BigDecimal.ZERO;
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.in(Transaction::getType, 1, 4)
                .eq(Transaction::getIsConfirmed, true)
                .eq(Transaction::getReviewStatus, "REVIEWED")
                .in(Transaction::getCategoryId, categoryIds)
                .ge(Transaction::getTransactionDate, start)
                .le(Transaction::getTransactionDate, end);
        return transactionMapper.selectList(query).stream()
                .map(tx -> tx.getType() == 4 ? tx.getAmountBase().negate() : tx.getAmountBase())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateYear(String year) {
        if (year == null || !year.matches("\\d{4}")) {
            throw new BizException(ErrorCode.PARAM_ERROR, "年份格式必须为 yyyy");
        }
    }

    private void validateTotal(BigDecimal totalAmount, List<BigDecimal> itemAmounts) {
        if (itemAmounts == null || itemAmounts.isEmpty()) return;
        BigDecimal sum = itemAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(totalAmount) != 0) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "年度总预算(" + totalAmount + ")与分类明细之和(" + sum + ")不匹配");
        }
    }

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
            if (!"SPECIAL".equals(category.getBudgetType())) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "年度专项预算只能设置在专项分类: " + category.getName());
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

    private String calculateStatus(BigDecimal progressPct) {
        if (progressPct.compareTo(BigDecimal.valueOf(100)) > 0) return "EXCEEDED";
        if (progressPct.compareTo(BigDecimal.valueOf(80)) > 0) return "CAUTION";
        return "NORMAL";
    }
}