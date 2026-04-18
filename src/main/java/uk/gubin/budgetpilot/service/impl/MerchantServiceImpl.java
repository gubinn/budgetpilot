package uk.gubin.budgetpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.dto.MerchantCreateDTO;
import uk.gubin.budgetpilot.dto.MerchantQueryDTO;
import uk.gubin.budgetpilot.dto.MerchantUpdateDTO;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Merchant;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.MerchantMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.service.MerchantService;
import uk.gubin.budgetpilot.vo.MerchantVO;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl extends ServiceImpl<MerchantMapper, Merchant> implements MerchantService {

    private final ObjectMapper objectMapper;
    private final CategoryMapper categoryMapper;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional
    public MerchantVO create(MerchantCreateDTO dto) {
        // 检查名称是否已存在
        LambdaQueryWrapper<Merchant> nameQuery = new LambdaQueryWrapper<>();
        nameQuery.eq(Merchant::getName, dto.getName());
        if (baseMapper.selectCount(nameQuery) > 0) {
            throw new BizException(ErrorCode.MERCHANT_NAME_EXISTS, dto.getName());
        }

        // 构建实体
        Merchant entity = new Merchant();
        entity.setName(dto.getName());
        entity.setAlias(dto.getAlias());
        entity.setCategoryId(dto.getCategoryId());
        entity.setIcon(dto.getIcon());
        entity.setColor(dto.getColor() != null ? dto.getColor() : generateDefaultColor());
        entity.setDescription(dto.getDescription());
        entity.setUsageCount(0);
        entity.setIsActive(true);
        entity.setIsSystem(false);

        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            try {
                entity.setTags(objectMapper.writeValueAsString(dto.getTags()));
            } catch (Exception ignored) {}
        }

        baseMapper.insert(entity);
        log.info("Created merchant: {}", entity.getName());

        return toVO(entity);
    }

    @Override
    public MerchantVO getById(Long id) {
        Merchant entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        return toVO(entity);
    }

    @Override
    @Transactional
    public MerchantVO update(Long id, MerchantUpdateDTO dto) {
        Merchant entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.MERCHANT_NOT_FOUND);
        }

        // 系统预设商户不可修改
        if (Boolean.TRUE.equals(entity.getIsSystem())) {
            throw new BizException(ErrorCode.MERCHANT_SYSTEM_IMMUTABLE);
        }

        // 名称修改时检查是否重复
        if (dto.getName() != null && !dto.getName().equals(entity.getName())) {
            LambdaQueryWrapper<Merchant> nameQuery = new LambdaQueryWrapper<>();
            nameQuery.eq(Merchant::getName, dto.getName());
            if (baseMapper.selectCount(nameQuery) > 0) {
                throw new BizException(ErrorCode.MERCHANT_NAME_EXISTS, dto.getName());
            }
            entity.setName(dto.getName());
        }

        if (dto.getAlias() != null) entity.setAlias(dto.getAlias());
        if (dto.getCategoryId() != null) entity.setCategoryId(dto.getCategoryId());
        if (dto.getIcon() != null) entity.setIcon(dto.getIcon());
        if (dto.getColor() != null) entity.setColor(dto.getColor());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getIsActive() != null) entity.setIsActive(dto.getIsActive());

        if (dto.getTags() != null) {
            try {
                entity.setTags(objectMapper.writeValueAsString(dto.getTags()));
            } catch (Exception ignored) {}
        }

        baseMapper.updateById(entity);
        log.info("Updated merchant: {}", entity.getName());

        return toVO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Merchant entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.MERCHANT_NOT_FOUND);
        }

        // 系统预设商户不可删除
        if (Boolean.TRUE.equals(entity.getIsSystem())) {
            throw new BizException(ErrorCode.MERCHANT_SYSTEM_IMMUTABLE);
        }

        // 检查是否有关联交易
        if (hasTransactions(id)) {
            throw new BizException(ErrorCode.MERCHANT_HAS_TRANSACTIONS);
        }

        baseMapper.deleteById(id);
        log.info("Deleted merchant: {}", id);
    }

    @Override
    public List<MerchantVO> query(MerchantQueryDTO dto) {
        LambdaQueryWrapper<Merchant> query = new LambdaQueryWrapper<>();

        if (dto.getKeyword() != null && !dto.getKeyword().trim().isEmpty()) {
            String kw = dto.getKeyword().trim();
            query.and(q -> q
                    .like(Merchant::getName, kw)
                    .or()
                    .like(Merchant::getAlias, kw)
            );
        }

        if (dto.getCategoryId() != null) {
            query.eq(Merchant::getCategoryId, dto.getCategoryId());
        }

        // 默认仅查询活跃商户
        if (dto.getIsActive() != null) {
            query.eq(Merchant::getIsActive, dto.getIsActive());
        } else {
            query.eq(Merchant::getIsActive, true);
        }

        // 解析排序
        String[] sortParts = dto.getSort().split(",");
        String sortField = sortParts[0];
        boolean isAsc = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1]);

        if (isAsc) {
            query.orderByAsc(mapSortField(sortField));
        } else {
            query.orderByDesc(mapSortField(sortField));
        }

        List<Merchant> merchants = baseMapper.selectList(query);
        return merchants.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<MerchantVO> searchByName(String keyword, Integer limit) {
        LambdaQueryWrapper<Merchant> query = new LambdaQueryWrapper<>();
        query.eq(Merchant::getIsActive, true);

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            query.and(q -> q
                    .like(Merchant::getName, kw)
                    .or()
                    .like(Merchant::getAlias, kw)
            );
        }

        // 按使用频次 + 最近使用时间排序
        query.orderByDesc(Merchant::getUsageCount)
                .orderByDesc(Merchant::getLastUsedAt);

        if (limit != null && limit > 0) {
            query.last("LIMIT " + limit);
        }

        List<Merchant> merchants = baseMapper.selectList(query);
        return merchants.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MerchantVO findOrCreate(String name, Long categoryId, boolean autoCreate) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String trimmedName = name.trim();

        // 1. 先精确匹配名称
        LambdaQueryWrapper<Merchant> exactQuery = new LambdaQueryWrapper<>();
        exactQuery.eq(Merchant::getName, trimmedName).eq(Merchant::getIsActive, true);
        Merchant existing = baseMapper.selectOne(exactQuery);
        if (existing != null) {
            return toVO(existing);
        }

        // 2. 模糊匹配别名
        LambdaQueryWrapper<Merchant> aliasQuery = new LambdaQueryWrapper<>();
        aliasQuery.eq(Merchant::getIsActive, true)
                .like(Merchant::getAlias, trimmedName);
        List<Merchant> aliasMatches = baseMapper.selectList(aliasQuery);
        if (!aliasMatches.isEmpty()) {
            return toVO(aliasMatches.get(0));
        }

        // 3. 自动创建新商户
        if (autoCreate) {
            MerchantCreateDTO dto = new MerchantCreateDTO();
            dto.setName(trimmedName);
            dto.setCategoryId(categoryId);
            return create(dto);
        }

        return null;
    }

    @Override
    @Transactional
    public void incrementUsage(Long merchantId) {
        if (merchantId == null) return;
        baseMapper.incrementUsage(merchantId, LocalDate.now());
    }

    @Override
    public boolean hasTransactions(Long merchantId) {
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.eq(Transaction::getMerchantId, merchantId);
        return transactionMapper.selectCount(query) > 0;
    }

    // ==================== Internal Methods ====================

    private MerchantVO toVO(Merchant entity) {
        MerchantVO vo = new MerchantVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setAlias(entity.getAlias());
        vo.setCategoryId(entity.getCategoryId());
        vo.setIcon(entity.getIcon());
        vo.setColor(entity.getColor());
        vo.setDescription(entity.getDescription());
        vo.setUsageCount(entity.getUsageCount());
        vo.setLastUsedAt(entity.getLastUsedAt());
        vo.setIsActive(entity.getIsActive());
        vo.setIsSystem(entity.getIsSystem());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());

        // 关联分类信息
        if (entity.getCategoryId() != null) {
            Category category = categoryMapper.selectById(entity.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
                vo.setCategoryColor(category.getColor());
            }
        }

        // 解析标签
        if (entity.getTags() != null) {
            try {
                vo.setTags(objectMapper.readValue(entity.getTags(), new TypeReference<>() {}));
            } catch (Exception ignored) {}
        }

        return vo;
    }

    private String generateDefaultColor() {
        // 生成随机颜色
        String[] colors = {"#e74c3c", "#3498db", "#27ae60", "#f39c12", "#9b59b6", "#1abc9c", "#e67e22", "#2ecc71"};
        return colors[(int) (Math.random() * colors.length)];
    }

    private com.baomidou.mybatisplus.core.toolkit.support.SFunction<Merchant, ?> mapSortField(String field) {
        return switch (field) {
            case "name" -> Merchant::getName;
            case "created_at" -> Merchant::getCreatedAt;
            case "last_used_at" -> Merchant::getLastUsedAt;
            default -> Merchant::getUsageCount;
        };
    }
}