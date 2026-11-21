package uk.gubin.budgetpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.common.MerchantNameNormalizer;
import uk.gubin.budgetpilot.common.PageResult;
import uk.gubin.budgetpilot.dto.MerchantCreateDTO;
import uk.gubin.budgetpilot.dto.MerchantQueryDTO;
import uk.gubin.budgetpilot.dto.MerchantUpdateDTO;
import uk.gubin.budgetpilot.dto.MerchantUsageStatDTO;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Merchant;
import uk.gubin.budgetpilot.entity.MerchantAlias;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.MerchantAliasMapper;
import uk.gubin.budgetpilot.mapper.MerchantMapper;
import uk.gubin.budgetpilot.mapper.RecurringRuleMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.service.MerchantService;
import uk.gubin.budgetpilot.vo.MerchantVO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final MerchantAliasMapper merchantAliasMapper;
    private final RecurringRuleMapper recurringRuleMapper;
    private final MerchantNameNormalizer nameNormalizer;

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

        merchantAliasMapper.deleteByMerchantId(id);
        baseMapper.deleteById(id);
        log.info("Deleted merchant: {}", id);
    }

    @Override
    public List<MerchantVO> query(MerchantQueryDTO dto) {
        LambdaQueryWrapper<Merchant> query = buildQuery(dto);
        applySort(query, dto);
        List<Merchant> merchants = baseMapper.selectList(query);
        return toVOList(merchants);
    }

    @Override
    public PageResult<MerchantVO> queryPage(MerchantQueryDTO dto) {
        LambdaQueryWrapper<Merchant> query = buildQuery(dto);
        applySort(query, dto);

        int pageNo = dto.getPage() == null || dto.getPage() < 1 ? 1 : dto.getPage();
        int pageSize = dto.getSize() == null || dto.getSize() < 1 ? 20 : Math.min(dto.getSize(), 200);
        Page<Merchant> page = new Page<>(pageNo, pageSize);
        Page<Merchant> result = baseMapper.selectPage(page, query);

        return PageResult.of(toVOList(result.getRecords()), result.getTotal(), pageNo, pageSize);
    }

    @Override
    public List<MerchantVO> searchByName(String keyword, Integer limit) {
        LambdaQueryWrapper<Merchant> query = new LambdaQueryWrapper<>();
        query.eq(Merchant::getIsActive, true);

        applyKeywordFilter(query, keyword);

        // 按使用频次 + 最近使用时间排序
        query.orderByDesc(Merchant::getUsageCount)
                .orderByDesc(Merchant::getLastUsedAt)
                .orderByDesc(Merchant::getId);

        // 默认限制 10 条，避免加载全表
        int safeLimit = (limit == null || limit <= 0) ? 10 : limit;
        query.last("LIMIT " + safeLimit);

        List<Merchant> merchants = baseMapper.selectList(query);
        return toVOList(merchants);
    }

    @Override
    @Transactional
    public MerchantVO findOrCreate(String name, Long categoryId, boolean autoCreate) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String raw = trimTo(name, 100);
        String canonical = trimTo(nameNormalizer.canonicalName(raw), 100);
        if (canonical.isBlank()) {
            canonical = raw;
        }

        // 1. 已存在的聚合商户（含被停用的，直接复用，避免撞唯一键）
        Merchant canonicalMatch = findByName(canonical, false);
        if (canonicalMatch != null) {
            if (!Boolean.TRUE.equals(canonicalMatch.getIsActive())) {
                canonicalMatch.setIsActive(true);
                baseMapper.updateById(canonicalMatch);
            }
            recordAlias(canonicalMatch.getId(), raw, canonical);
            return toVO(canonicalMatch);
        }

        // 2. 已记录的原始名称别名
        Merchant aliasMatch = findByAlias(raw);
        if (aliasMatch != null) {
            return toVO(aliasMatch);
        }

        // 3. 历史遗留的原始名称商户：能改名就直接改名，避免再造一条
        Merchant legacy = findByName(raw, true);
        if (legacy != null) {
            if (!raw.equals(canonical) && !Boolean.TRUE.equals(legacy.getIsSystem())) {
                try {
                    legacy.setName(canonical);
                    baseMapper.updateById(legacy);
                } catch (DuplicateKeyException e) {
                    Merchant concurrent = findByName(canonical, true);
                    if (concurrent != null) {
                        recordAlias(concurrent.getId(), raw, canonical);
                        return toVO(concurrent);
                    }
                }
            }
            recordAlias(legacy.getId(), raw, canonical);
            return toVO(legacy);
        }

        // 4. 自动创建聚合商户
        if (autoCreate) {
            try {
                MerchantCreateDTO dto = new MerchantCreateDTO();
                dto.setName(canonical);
                dto.setCategoryId(categoryId);
                MerchantVO created = create(dto);
                recordAlias(created.getId(), raw, canonical);
                return getById(created.getId());
            } catch (DuplicateKeyException e) {
                // 并发创建，其他线程已创建成功，重新查询返回
                Merchant retry = findByName(canonical, true);
                if (retry != null) {
                    recordAlias(retry.getId(), raw, canonical);
                    return toVO(retry);
                }
                throw e;
            }
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

    @Override
    @Transactional
    public Map<String, Object> consolidate() {
        List<Merchant> all = baseMapper.selectList(new LambdaQueryWrapper<>());
        if (all.isEmpty()) {
            return Map.of("groups", 0, "mergedGroups", 0, "deleted", 0,
                    "transactionsRepointed", 0, "aliasesCreated", 0);
        }

        Map<Long, MerchantUsageStatDTO> stats = loadStats(
                all.stream().map(Merchant::getId).toList());

        Map<String, List<Merchant>> groups = new LinkedHashMap<>();
        for (Merchant merchant : all) {
            String canonical = trimTo(nameNormalizer.canonicalName(merchant.getName()), 100);
            if (canonical.isBlank()) canonical = merchant.getName();
            groups.computeIfAbsent(canonical, k -> new ArrayList<>()).add(merchant);
        }

        int mergedGroups = 0;
        int deleted = 0;
        int txRepointed = 0;
        int aliasesCreated = 0;

        for (Map.Entry<String, List<Merchant>> entry : groups.entrySet()) {
            String canonical = entry.getKey();
            List<Merchant> members = entry.getValue();
            members.sort(Comparator
                    .comparingLong((Merchant m) -> txCount(stats, m.getId())).reversed()
                    .thenComparing(Merchant::getId));

            Merchant exact = members.stream()
                    .filter(m -> canonical.equals(m.getName()))
                    .findFirst().orElse(null);
            Merchant system = members.stream()
                    .filter(m -> Boolean.TRUE.equals(m.getIsSystem()))
                    .findFirst().orElse(null);
            Merchant target = exact != null ? exact : (system != null ? system : members.get(0));

            if (members.size() > 1) {
                mergedGroups++;
            }

            for (Merchant member : members) {
                if (member.getId().equals(target.getId())) {
                    continue;
                }
                txRepointed += transactionMapper.reassignMerchant(member.getId(), target.getId());
                recurringRuleMapper.reassignMerchant(member.getId(), target.getId());
                merchantAliasMapper.reassignMerchant(member.getId(), target.getId());
                aliasesCreated += recordAliasForMerge(target.getId(), member.getName(), canonical);
                aliasesCreated += recordManualAliases(target.getId(), member.getAlias());

                if (!Boolean.TRUE.equals(member.getIsSystem())) {
                    baseMapper.deleteById(member.getId());
                    deleted++;
                }
            }

            // 先把其它成员的原始名称记为别名，再改主商户名称，避免唯一键冲突
            aliasesCreated += recordAliasForMerge(target.getId(), target.getName(), canonical);

            if (!canonical.equals(target.getName())) {
                try {
                    target.setName(canonical);
                } catch (Exception e) {
                    log.warn("Cannot rename merchant {} to {}: {}", target.getId(), canonical, e.getMessage());
                }
            }

            // 分类：以该商户历史交易中出现最多的分类为准，修正早期导入留下的旧分类
            Long categoryId = transactionMapper.findDominantCategoryId(target.getId());
            if (categoryId == null) {
                categoryId = members.stream()
                        .filter(m -> m.getCategoryId() != null)
                        .max(Comparator.comparingLong(m -> txCount(stats, m.getId())))
                        .map(Merchant::getCategoryId)
                        .orElse(target.getCategoryId());
            }

            long totalTx = members.stream().mapToLong(m -> txCount(stats, m.getId())).sum();
            LocalDate lastUsed = members.stream()
                    .map(m -> stats.get(m.getId()))
                    .filter(Objects::nonNull)
                    .map(MerchantUsageStatDTO::getLastDate)
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(target.getLastUsedAt());

            target.setCategoryId(categoryId);
            target.setUsageCount((int) Math.min(totalTx, Integer.MAX_VALUE));
            target.setLastUsedAt(lastUsed);
            if (target.getColor() == null) target.setColor(generateDefaultColor());
            baseMapper.updateById(target);
        }

        log.info("Consolidated merchants: groups={}, mergedGroups={}, deleted={}, txRepointed={}, aliasesCreated={}",
                groups.size(), mergedGroups, deleted, txRepointed, aliasesCreated);

        return Map.of(
                "groups", groups.size(),
                "mergedGroups", mergedGroups,
                "deleted", deleted,
                "transactionsRepointed", txRepointed,
                "aliasesCreated", aliasesCreated
        );
    }

    // ==================== Internal Methods ====================

    private LambdaQueryWrapper<Merchant> buildQuery(MerchantQueryDTO dto) {
        LambdaQueryWrapper<Merchant> query = new LambdaQueryWrapper<>();

        applyKeywordFilter(query, dto.getKeyword());

        if (dto.getCategoryId() != null) {
            query.eq(Merchant::getCategoryId, dto.getCategoryId());
        }

        // 默认仅查询活跃商户
        if (dto.getIsActive() != null) {
            query.eq(Merchant::getIsActive, dto.getIsActive());
        } else {
            query.eq(Merchant::getIsActive, true);
        }
        return query;
    }

    private void applyKeywordFilter(LambdaQueryWrapper<Merchant> query, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        String kw = keyword.trim();
        Set<Long> aliasMerchantIds = merchantAliasMapper.selectList(
                        new LambdaQueryWrapper<MerchantAlias>()
                                .like(MerchantAlias::getRawName, kw))
                .stream().map(MerchantAlias::getMerchantId).collect(Collectors.toCollection(LinkedHashSet::new));

        query.and(q -> {
            q.like(Merchant::getName, kw)
                    .or()
                    .like(Merchant::getAlias, kw);
            if (!aliasMerchantIds.isEmpty()) {
                q.or().in(Merchant::getId, aliasMerchantIds);
            }
        });
    }

    private void applySort(LambdaQueryWrapper<Merchant> query, MerchantQueryDTO dto) {
        String sort = dto.getSort() == null ? "usage_count,desc" : dto.getSort();
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        boolean isAsc = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1]);

        if (isAsc) {
            query.orderByAsc(mapSortField(sortField));
        } else {
            query.orderByDesc(mapSortField(sortField));
        }
        // 保证分页顺序稳定
        query.orderByDesc(Merchant::getId);
    }

    private List<MerchantVO> toVOList(List<Merchant> merchants) {
        if (merchants == null || merchants.isEmpty()) {
            return List.of();
        }

        Set<Long> categoryIds = merchants.stream()
                .map(Merchant::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Category> categoryMap = categoryIds.isEmpty() ? Map.of()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        List<Long> merchantIds = merchants.stream().map(Merchant::getId).toList();
        Map<Long, List<String>> aliasMap = new HashMap<>();
        for (MerchantAlias alias : merchantAliasMapper.selectList(
                new LambdaQueryWrapper<MerchantAlias>()
                        .in(MerchantAlias::getMerchantId, merchantIds)
                        .orderByAsc(MerchantAlias::getRawName))) {
            aliasMap.computeIfAbsent(alias.getMerchantId(), k -> new ArrayList<>()).add(alias.getRawName());
        }

        Map<Long, MerchantUsageStatDTO> stats = loadStats(merchantIds);

        return merchants.stream()
                .map(m -> toVO(m, categoryMap.get(m.getCategoryId()),
                        aliasMap.getOrDefault(m.getId(), List.of()), stats.get(m.getId())))
                .collect(Collectors.toList());
    }

    private MerchantVO toVO(Merchant entity) {
        return toVOList(List.of(entity)).get(0);
    }

    private MerchantVO toVO(Merchant entity, Category category, List<String> aliases,
                            MerchantUsageStatDTO stat) {
        MerchantVO vo = new MerchantVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setAlias(entity.getAlias());
        vo.setAliases(aliases);
        vo.setAliasCount(aliases.size());
        vo.setTransactionCount(stat == null ? 0L : stat.getTxCount());
        vo.setCategoryId(entity.getCategoryId());
        vo.setIcon(entity.getIcon());
        vo.setColor(entity.getColor());
        vo.setDescription(entity.getDescription());
        vo.setUsageCount(entity.getUsageCount());
        vo.setLastUsedAt(stat != null && stat.getLastDate() != null ? stat.getLastDate() : entity.getLastUsedAt());
        vo.setIsActive(entity.getIsActive());
        vo.setIsSystem(entity.getIsSystem());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());

        if (category != null) {
            vo.setCategoryName(category.getName());
            vo.setCategoryColor(category.getColor());
        }

        if (entity.getTags() != null) {
            try {
                vo.setTags(objectMapper.readValue(entity.getTags(), new TypeReference<>() {}));
            } catch (Exception ignored) {}
        }

        return vo;
    }

    private Map<Long, MerchantUsageStatDTO> loadStats(List<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, MerchantUsageStatDTO> stats = new HashMap<>();
        for (MerchantUsageStatDTO stat : transactionMapper.countByMerchantIds(merchantIds)) {
            stats.put(stat.getMerchantId(), stat);
        }
        return stats;
    }

    private long txCount(Map<Long, MerchantUsageStatDTO> stats, Long merchantId) {
        MerchantUsageStatDTO stat = stats.get(merchantId);
        return stat == null || stat.getTxCount() == null ? 0 : stat.getTxCount();
    }

    private Merchant findByName(String name, boolean activeOnly) {
        LambdaQueryWrapper<Merchant> query = new LambdaQueryWrapper<>();
        query.eq(Merchant::getName, name);
        if (activeOnly) query.eq(Merchant::getIsActive, true);
        return baseMapper.selectOne(query);
    }

    private Merchant findByAlias(String rawName) {
        MerchantAlias alias = merchantAliasMapper.selectOne(
                new LambdaQueryWrapper<MerchantAlias>().eq(MerchantAlias::getRawName, rawName));
        if (alias == null) {
            return null;
        }
        Merchant merchant = baseMapper.selectById(alias.getMerchantId());
        return merchant != null && Boolean.TRUE.equals(merchant.getIsActive()) ? merchant : null;
    }

    private void recordAlias(Long merchantId, String rawName, String canonical) {
        recordAliasForMerge(merchantId, rawName, canonical);
    }

    private int recordAliasForMerge(Long merchantId, String rawName, String canonical) {
        if (rawName == null || rawName.isBlank()) {
            return 0;
        }
        String raw = trimTo(rawName, 100);
        if (raw.equals(canonical)) {
            return 0;
        }

        MerchantAlias existing = merchantAliasMapper.selectOne(
                new LambdaQueryWrapper<MerchantAlias>().eq(MerchantAlias::getRawName, raw));
        if (existing != null) {
            if (!Objects.equals(existing.getMerchantId(), merchantId)) {
                existing.setMerchantId(merchantId);
                merchantAliasMapper.updateById(existing);
            }
            return 0;
        }

        MerchantAlias alias = new MerchantAlias();
        alias.setMerchantId(merchantId);
        alias.setRawName(raw);
        try {
            merchantAliasMapper.insert(alias);
            return 1;
        } catch (DuplicateKeyException e) {
            // 并发写入，忽略
            return 0;
        }
    }

    private int recordManualAliases(Long merchantId, String manualAlias) {
        if (manualAlias == null || manualAlias.isBlank()) {
            return 0;
        }
        int created = 0;
        for (String part : manualAlias.split("[,，;；/]")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                created += recordAliasForMerge(merchantId, trimmed, null);
            }
        }
        return created;
    }

    private String trimTo(String value, int max) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private String generateDefaultColor() {
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