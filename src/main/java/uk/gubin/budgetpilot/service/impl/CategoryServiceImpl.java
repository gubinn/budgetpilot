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
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.service.CategoryService;
import uk.gubin.budgetpilot.vo.CategoryVO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final TransactionMapper transactionMapper;
    private final StringRedisTemplate redisTemplate;

    /**
     * 清除所有报表缓存（分类变更可能影响所有月份）
     */
    private void clearAllReportCache() {
        try {
            // 清除所有月度汇总缓存（包含分类名称、颜色）
            scanAndDelete("report:monthly-summary:*");
            // 清除所有分类详情缓存
            scanAndDelete("report:category-detail:*");
            log.info("Cleared all report cache due to category change");
        } catch (Exception e) {
            log.warn("Failed to clear all report cache", e);
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

        List<Category> roots = all.stream()
                .filter(c -> c.getParentId() == 0)
                .collect(Collectors.toList());

        return roots.stream().map(root -> {
            CategoryVO vo = toVO(root);
            List<CategoryVO> children = all.stream()
                    .filter(c -> c.getParentId() != null && c.getParentId().equals(root.getId()))
                    .map(this::toVO)
                    .collect(Collectors.toList());
            vo.setChildren(children);
            return vo;
        }).collect(Collectors.toList());
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
    public CategoryVO update(Long id, CategoryCreateDTO dto) {
        Category entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getIcon() != null) entity.setIcon(dto.getIcon());
        if (dto.getColor() != null) entity.setColor(dto.getColor());
        if (dto.getSortOrder() != null) entity.setSortOrder(dto.getSortOrder());
        // 不允许修改 type
        baseMapper.updateById(entity);

        // 清除报表缓存（分类名称、颜色变更会影响报表显示）
        clearAllReportCache();

        return toVO(entity);
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
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setChildren(new ArrayList<>());
        return vo;
    }
}
