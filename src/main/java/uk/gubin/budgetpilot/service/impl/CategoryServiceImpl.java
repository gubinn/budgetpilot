package uk.gubin.budgetpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.dto.CategoryCreateDTO;
import uk.gubin.budgetpilot.dto.CategoryUpdateDTO;
import uk.gubin.budgetpilot.entity.AnnualBudgetItem;
import uk.gubin.budgetpilot.entity.BudgetItem;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.AnnualBudgetItemMapper;
import uk.gubin.budgetpilot.mapper.BudgetItemMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.MerchantMapper;
import uk.gubin.budgetpilot.mapper.RecurringRuleMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.service.CategoryService;
import uk.gubin.budgetpilot.vo.CategoryVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final TransactionMapper transactionMapper;
    private final CategoryMapper categoryMapper;
    private final MerchantMapper merchantMapper;
    private final RecurringRuleMapper recurringRuleMapper;
    private final BudgetItemMapper budgetItemMapper;
    private final AnnualBudgetItemMapper annualBudgetItemMapper;
    private final StringRedisTemplate redisTemplate;

    /**
     * 清除当前用户的报表缓存（分类变更只影响当前用户）
     */
    private void clearAllReportCache() {
        Long userId = getUserId();
        try {
            if (userId != null) {
                scanAndDelete("report:monthly-summary:" + userId + ":*");
                scanAndDelete("report:category-detail:" + userId + ":*");
            }
            log.info("Cleared report cache for user {}", userId);
        } catch (Exception e) {
            log.warn("Failed to clear report cache", e);
        }
    }

    private Long getUserId() {
        try {
            return cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 使用 SCAN 命令安全删除匹配的 keys
     */
    private void scanAndDelete(String pattern) {
        try {
            var cursor = redisTemplate.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100)
                    .build());
            while (cursor.hasNext()) {
                redisTemplate.delete(cursor.next());
            }
            cursor.close();
        } catch (Exception e) {
            log.warn("Failed to scan and delete keys with pattern {}", pattern, e);
        }
    }

    @Override
    @Transactional
    public CategoryVO create(CategoryCreateDTO dto) {
        Category entity = new Category();
        entity.setParentId(dto.getParentId());
        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setIcon(dto.getIcon());
        entity.setColor(dto.getColor());
        entity.setSortOrder(dto.getSortOrder());
        entity.setIsSystem(false);
        entity.setIsActive(true);
        entity.setBudgetType(normalizeBudgetType(dto.getBudgetType(),
                dto.getType() != null && dto.getType() == 1));
        entity.setExcludeFromReports(Boolean.TRUE.equals(dto.getExcludeFromReports()));
        save(entity);
        log.info("Created category: {}", entity.getName());
        return toVO(entity);
    }

    @Override
    public List<CategoryVO> getTree(Integer type) {
        LambdaQueryWrapper<Category> query = new LambdaQueryWrapper<>();
        query.eq(Category::getIsActive, true);
        if (type != null) {
            query.eq(Category::getType, type);
        }
        query.orderByAsc(Category::getSortOrder);

        List<Category> all = list(query);

        // 循环引用检测
        detectCircularReference(all);

        List<CategoryVO> tree = buildTree(all, 0L);
        Map<Long, Long> directCounts = new java.util.HashMap<>();
        for (var stat : transactionMapper.countGroupedByCategory()) {
            directCounts.put(stat.getCategoryId(), stat.getTxCount());
        }
        for (CategoryVO root : tree) {
            populateTransactionCount(root, directCounts);
        }
        return tree;
    }

    private long populateTransactionCount(CategoryVO vo, Map<Long, Long> directCounts) {
        long total = directCounts.getOrDefault(vo.getId(), 0L);
        if (vo.getChildren() != null) {
            for (CategoryVO child : vo.getChildren()) {
                total += populateTransactionCount(child, directCounts);
            }
        }
        vo.setTransactionCount(total);
        return total;
    }

    /**
     * 递归组装分类树，支持任意层级（此前只组装两层，三级及以下会丢失）
     */
    private List<CategoryVO> buildTree(List<Category> all, long parentId) {
        return all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() == parentId)
                .map(c -> {
                    CategoryVO vo = toVO(c);
                    vo.setChildren(buildTree(all, c.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 检测分类树中是否存在循环引用
     */
    private void detectCircularReference(List<Category> categories) {
        for (Category cat : categories) {
            if (hasCircularPath(categories, cat.getId(), new java.util.HashSet<>())) {
                throw new BizException(ErrorCode.CATEGORY_CIRCULAR_REFERENCE,
                        String.format("分类 \"%s\" 存在循环引用", cat.getName()));
            }
        }
    }

    private boolean hasCircularPath(List<Category> categories, Long categoryId, java.util.Set<Long> visited) {
        if (visited.contains(categoryId)) {
            return true;
        }
        visited.add(categoryId);

        Category cat = categories.stream()
                .filter(c -> c.getId().equals(categoryId))
                .findFirst()
                .orElse(null);
        if (cat == null || cat.getParentId() == 0) {
            return false;
        }
        return hasCircularPath(categories, cat.getParentId(), visited);
    }

    @Override
    public CategoryVO getById(Long id) {
        Category entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return toVO(entity);
    }

    @Override
    @Transactional
    public CategoryVO update(Long id, CategoryUpdateDTO dto) {
        Category entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 系统分类不可修改类型
        if (dto.getType() != null && !dto.getType().equals(entity.getType())) {
            throw new BizException(ErrorCode.CATEGORY_SYSTEM_IMMUTABLE);
        }

        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getIcon() != null) entity.setIcon(dto.getIcon());
        if (dto.getColor() != null) entity.setColor(dto.getColor());
        if (dto.getSortOrder() != null) entity.setSortOrder(dto.getSortOrder());
        if (dto.getExcludeFromReports() != null) {
            entity.setExcludeFromReports(dto.getExcludeFromReports());
        }
        if (dto.getBudgetType() != null) {
            String newBudgetType = normalizeBudgetType(dto.getBudgetType(),
                    entity.getType() != null && entity.getType() == 1);
            if (!newBudgetType.equals(entity.getBudgetType())) {
                boolean hasBudget = budgetItemMapper.selectCount(
                        new LambdaQueryWrapper<BudgetItem>().eq(BudgetItem::getCategoryId, id)) > 0
                        || annualBudgetItemMapper.selectCount(
                        new LambdaQueryWrapper<AnnualBudgetItem>().eq(AnnualBudgetItem::getCategoryId, id)) > 0;
                if (hasBudget) {
                    throw new BizException(ErrorCode.PARAM_ERROR,
                            "该分类已有预算，不能直接修改预算属性；请先合并或删除预算项");
                }
            }
            entity.setBudgetType(newBudgetType);
        }

        // 处理 parentId 修改
        if (dto.getParentId() != null && !dto.getParentId().equals(entity.getParentId())) {
            // 不能将自己设为自己的父节点
            if (dto.getParentId().equals(id)) {
                throw new BizException(ErrorCode.CATEGORY_CIRCULAR_REFERENCE, "分类不能设为自己的父节点");
            }
            // 循环引用检测：检查新父节点是否会成为当前分类的子节点
            if (dto.getParentId() != 0 && isDescendant(entity.getId(), dto.getParentId())) {
                throw new BizException(ErrorCode.CATEGORY_CIRCULAR_REFERENCE, "会导致循环引用");
            }
            entity.setParentId(dto.getParentId());
        }

        baseMapper.updateById(entity);

        // 清除报表缓存（分类名称、颜色变更会影响报表显示）
        clearAllReportCache();

        return toVO(entity);
    }

    /**
     * 检查 targetId 是否是 categoryId 的后代（间接子节点）
     */
    private boolean isDescendant(Long categoryId, Long targetId) {
        List<Category> all = list(new LambdaQueryWrapper<Category>().eq(Category::getIsActive, true));
        return checkDescendant(categoryId, targetId, all, new java.util.HashSet<>());
    }

    private boolean checkDescendant(Long ancestorId, Long targetId, List<Category> all, java.util.Set<Long> visited) {
        if (ancestorId.equals(targetId)) {
            return true;
        }
        if (visited.contains(ancestorId)) {
            return false;
        }
        visited.add(ancestorId);

        List<Category> children = all.stream()
                .filter(c -> ancestorId.equals(c.getParentId()))
                .collect(Collectors.toList());
        for (Category child : children) {
            if (checkDescendant(child.getId(), targetId, all, visited)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        if (entity.getIsSystem()) {
            throw new BizException(ErrorCode.CATEGORY_SYSTEM_IMMUTABLE);
        }
        if (hasChildren(id)) {
            throw new BizException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }
        if (hasTransactions(id)) {
            throw new BizException(ErrorCode.CATEGORY_HAS_TRANSACTIONS);
        }
        entity.setIsActive(false);
        baseMapper.updateById(entity);

        // 清除报表缓存
        clearAllReportCache();

        log.info("Deactivated category: {}", entity.getName());
    }

    @Override
    public List<Category> getChildren(Long parentId) {
        LambdaQueryWrapper<Category> query = new LambdaQueryWrapper<>();
        query.eq(Category::getParentId, parentId);
        query.eq(Category::getIsActive, true);
        return list(query);
    }

    @Override
    @Transactional
    public CategoryVO merge(Long sourceId, Long targetId) {
        if (sourceId == null || targetId == null || sourceId.equals(targetId)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "源分类和目标分类不能相同");
        }
        Category source = baseMapper.selectById(sourceId);
        Category target = baseMapper.selectById(targetId);
        if (source == null || target == null) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        if (!source.getType().equals(target.getType())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "只能合并同类型分类");
        }
        if (isDescendant(sourceId, targetId)) {
            throw new BizException(ErrorCode.CATEGORY_CIRCULAR_REFERENCE, "不能合并到自己的子分类");
        }
        if (hasChildren(sourceId)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请先合并子分类，再合并当前分类");
        }
        if (hasChildren(targetId)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "目标分类必须是没有子分类的叶子");
        }

        transactionMapper.reassignCategory(sourceId, targetId);
        merchantMapper.reassignCategory(sourceId, targetId);
        recurringRuleMapper.reassignCategory(sourceId, targetId);

        List<BudgetItem> sourceBudgetItems = budgetItemMapper.selectList(
                new LambdaQueryWrapper<BudgetItem>().eq(BudgetItem::getCategoryId, sourceId));
        for (BudgetItem item : sourceBudgetItems) {
            BudgetItem existing = budgetItemMapper.selectOne(new LambdaQueryWrapper<BudgetItem>()
                    .eq(BudgetItem::getBudgetId, item.getBudgetId())
                    .eq(BudgetItem::getCategoryId, targetId));
            if (existing == null) {
                item.setCategoryId(targetId);
                budgetItemMapper.updateById(item);
            } else {
                existing.setAmount(existing.getAmount().add(item.getAmount()));
                budgetItemMapper.updateById(existing);
                budgetItemMapper.deleteById(item.getId());
            }
        }

        List<AnnualBudgetItem> sourceAnnualItems = annualBudgetItemMapper.selectList(
                new LambdaQueryWrapper<AnnualBudgetItem>().eq(AnnualBudgetItem::getCategoryId, sourceId));
        for (AnnualBudgetItem item : sourceAnnualItems) {
            AnnualBudgetItem existing = annualBudgetItemMapper.selectOne(new LambdaQueryWrapper<AnnualBudgetItem>()
                    .eq(AnnualBudgetItem::getAnnualBudgetId, item.getAnnualBudgetId())
                    .eq(AnnualBudgetItem::getCategoryId, targetId));
            if (existing == null) {
                item.setCategoryId(targetId);
                annualBudgetItemMapper.updateById(item);
            } else {
                existing.setAmount(existing.getAmount().add(item.getAmount()));
                annualBudgetItemMapper.updateById(existing);
                annualBudgetItemMapper.deleteById(item.getId());
            }
        }

        source.setIsActive(false);
        baseMapper.updateById(source);
        clearAllReportCache();
        log.info("Merged category {} into {}", sourceId, targetId);
        return toVO(target);
    }

    @Override
    public boolean hasChildren(Long id) {
        LambdaQueryWrapper<Category> query = new LambdaQueryWrapper<>();
        query.eq(Category::getParentId, id);
        return count(query) > 0;
    }

    @Override
    public boolean hasTransactions(Long id) {
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.eq(Transaction::getCategoryId, id);
        return transactionMapper.selectCount(query) > 0;
    }

    private String normalizeBudgetType(String value, boolean expense) {
        if (!expense) return "NONE";
        if (value == null || value.isBlank()) return "DAILY";
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "DAILY", "FIXED", "SPECIAL", "NONE" -> normalized;
            default -> "DAILY";
        };
    }

    private CategoryVO toVO(Category entity) {
        CategoryVO vo = new CategoryVO();
        vo.setId(entity.getId());
        vo.setParentId(entity.getParentId());
        vo.setName(entity.getName());
        vo.setType(entity.getType());
        vo.setIcon(entity.getIcon());
        vo.setColor(entity.getColor());
        vo.setSortOrder(entity.getSortOrder());
        vo.setIsSystem(entity.getIsSystem());
        vo.setIsActive(entity.getIsActive());
        vo.setBudgetType(entity.getBudgetType());
        vo.setExcludeFromReports(entity.getExcludeFromReports());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setChildren(new ArrayList<>());
        return vo;
    }
}
