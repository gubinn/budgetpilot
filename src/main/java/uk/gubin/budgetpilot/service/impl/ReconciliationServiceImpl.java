package uk.gubin.budgetpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.common.PageResult;
import uk.gubin.budgetpilot.dto.ReconciliationCreateTxDTO;
import uk.gubin.budgetpilot.dto.TransactionCreateDTO;
import uk.gubin.budgetpilot.entity.Account;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Merchant;
import uk.gubin.budgetpilot.entity.ReconciliationBatch;
import uk.gubin.budgetpilot.entity.ReconciliationItem;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.MerchantMapper;
import uk.gubin.budgetpilot.mapper.ReconciliationBatchMapper;
import uk.gubin.budgetpilot.mapper.ReconciliationItemMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.service.ReconciliationService;
import uk.gubin.budgetpilot.service.TransactionService;
import uk.gubin.budgetpilot.service.reconciliation.BillChannel;
import uk.gubin.budgetpilot.service.reconciliation.BillParser;
import uk.gubin.budgetpilot.service.reconciliation.ParsedBill;
import uk.gubin.budgetpilot.service.reconciliation.ParsedBillItem;
import uk.gubin.budgetpilot.service.reconciliation.ReconcileConstants;
import uk.gubin.budgetpilot.service.reconciliation.ReconciliationMatcher;
import uk.gubin.budgetpilot.service.reconciliation.ReconciliationStatsUpdater;
import uk.gubin.budgetpilot.vo.MatchedTransactionVO;
import uk.gubin.budgetpilot.vo.ReconciliationBatchVO;
import uk.gubin.budgetpilot.vo.ReconciliationImportVO;
import uk.gubin.budgetpilot.vo.ReconciliationItemVO;
import uk.gubin.budgetpilot.vo.TransactionVO;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_CANDIDATES = 50;

    private final List<BillParser> parsers;
    private final ReconciliationMatcher matcher;
    private final ReconciliationStatsUpdater statsUpdater;
    private final ReconciliationBatchMapper batchMapper;
    private final ReconciliationItemMapper itemMapper;
    private final TransactionMapper transactionMapper;
    private final AccountMapper accountMapper;
    private final MerchantMapper merchantMapper;
    private final CategoryMapper categoryMapper;
    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;

    /** 本人账户互转的对手名，逗号分隔；命中后按转账处理，不计入收支 */
    @Value("${budgetpilot.reconciliation.self-names:}")
    private String selfNamesConfig;

    // ------------------------------------------------------------------ import

    @Override
    @Transactional
    public ReconciliationImportVO importBill(MultipartFile file, String channelCode, Long accountId,
                                                             boolean force) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择账单文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ErrorCode.PARAM_ERROR, "账单文件不能超过 10MB");
        }
        BillChannel channel = BillChannel.of(channelCode);
        if (channel == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不支持的账单渠道");
        }
        if (channel == BillChannel.CMB_CREDIT && accountId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "招商银行信用卡对账请先选择信用卡账户");
        }
        if (channel == BillChannel.CMB_DEBIT && accountId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "招商银行储蓄卡对账请先选择储蓄卡账户");
        }
        BillParser parser = parsers.stream().filter(p -> p.supports(channel)).findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.PARAM_ERROR,
                        channel.getLabel() + "账单解析暂未开放"));

        byte[] content;
        try {
            content = file.getBytes();
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "读取账单文件失败");
        }
        String fileHash = sha256(content);

        ReconciliationBatch existing = batchMapper.selectOne(new LambdaQueryWrapper<ReconciliationBatch>()
                .eq(ReconciliationBatch::getChannel, channel.name())
                .eq(ReconciliationBatch::getFileHash, fileHash));
        if (existing != null && !force) {
            ReconciliationImportVO vo = new ReconciliationImportVO();
            vo.setDuplicate(true);
            vo.setMessage("该账单已导入过，未重复处理");
            vo.setBatch(toBatchVO(existing));
            return vo;
        }

        ParsedBill bill;
        try {
            bill = parser.parse(content);
        } catch (Exception e) {
            log.warn("Failed to parse {} bill: {}", channel, e.getMessage());
            throw new BizException(ErrorCode.PARAM_ERROR, "账单解析失败：" + e.getMessage());
        }
        if (bill.getItems().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "账单中没有解析到交易明细");
        }
        if (bill.getPeriodStart() == null || bill.getPeriodEnd() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "无法识别账单日期范围");
        }

        Map<String, ReconciliationItem> preserved = new LinkedHashMap<>();
        Set<Long> oldLinkedTxIds = new HashSet<>();
        ReconciliationBatch batch;
        if (existing != null) {
            // 解析器升级后重新解析同一文件：保留手工匹配、补录和忽略结果，其余重新匹配
            batch = existing;
            for (ReconciliationItem old : itemMapper.selectList(
                    new LambdaQueryWrapper<ReconciliationItem>()
                            .eq(ReconciliationItem::getBatchId, batch.getId()))) {
                if (old.getMatchTransactionId() != null) {
                    oldLinkedTxIds.add(old.getMatchTransactionId());
                }
                if (isPreserved(old)) {
                    preserved.put(old.getItemKey(), old);
                }
            }
            itemMapper.delete(new LambdaQueryWrapper<ReconciliationItem>()
                    .eq(ReconciliationItem::getBatchId, batch.getId()));
            oldLinkedTxIds.forEach(this::clearTransactionIfUnreferenced);

            batch.setFileName(file.getOriginalFilename() == null
                    ? defaultFileName(channel) : file.getOriginalFilename());
            batch.setAccountId(accountId);
            batch.setBillAccount(bill.getBillAccount());
            batch.setPeriodStart(bill.getPeriodStart());
            batch.setPeriodEnd(bill.getPeriodEnd());
            batch.setTotalCount(0);
            batch.setTotalAmount(BigDecimal.ZERO);
            batch.setMatchedCount(0);
            batch.setUnmatchedCount(0);
            batch.setIgnoredCount(0);
            batch.setStatus(ReconcileConstants.BATCH_DONE);
            batchMapper.updateById(batch);
        } else {
            batch = new ReconciliationBatch();
            batch.setChannel(channel.name());
            batch.setFileName(file.getOriginalFilename() == null
                    ? defaultFileName(channel) : file.getOriginalFilename());
            batch.setFileHash(fileHash);
            batch.setAccountId(accountId);
            batch.setBillAccount(bill.getBillAccount());
            batch.setPeriodStart(bill.getPeriodStart());
            batch.setPeriodEnd(bill.getPeriodEnd());
            batch.setTotalCount(0);
            batch.setTotalAmount(BigDecimal.ZERO);
            batch.setMatchedCount(0);
            batch.setUnmatchedCount(0);
            batch.setIgnoredCount(0);
            batch.setStatus(ReconcileConstants.BATCH_DONE);
            batchMapper.insert(batch);
        }

        // 同一渠道下已存在的账单行跳过，保证跨文件重复导入幂等
        List<String> keys = bill.getItems().stream().map(ParsedBillItem::getItemKey).toList();
        Set<String> existingKeys = new HashSet<>();
        if (!keys.isEmpty()) {
            existingKeys = itemMapper.selectList(new LambdaQueryWrapper<ReconciliationItem>()
                            .select(ReconciliationItem::getItemKey)
                            .eq(ReconciliationItem::getChannel, channel.name())
                            .in(ReconciliationItem::getItemKey, keys))
                    .stream().map(ReconciliationItem::getItemKey).collect(Collectors.toSet());
        }

        int skipped = 0;
        int restored = 0;
        List<ReconciliationItem> inserted = new ArrayList<>();
        for (ParsedBillItem parsed : bill.getItems()) {
            ReconciliationItem old = preserved.get(parsed.getItemKey());
            if (existingKeys.contains(parsed.getItemKey()) && old == null) {
                skipped++;
                continue;
            }
            ReconciliationItem item = toEntity(batch.getId(), channel, parsed);
            if (old != null) {
                copyMatchState(item, old);
                restored++;
            }
            try {
                itemMapper.insert(item);
                inserted.add(item);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 同一份账单或跨文件出现完全相同的行，保留第一条
                skipped++;
            }
        }

        List<Transaction> candidates = loadCandidateTransactions(batch);
        if (!inserted.isEmpty()) {
            List<ReconciliationMatcher.ItemMatch> matches = matcher.match(inserted, candidates, channel);
            applyMatches(inserted, candidates, matches, batch.getId());
        }

        Map<String, Object> summary = new LinkedHashMap<>(bill.getSummary());
        summary.put("channelLabel", channel.getLabel());
        summary.put("skippedDuplicates", skipped);
        if (existing != null) {
            summary.put("reparsed", true);
            summary.put("restoredManualMatches", restored);
        }
        batch.setSummaryJson(writeJson(summary));
        refreshBatchCounts(batch);

        log.info("Reconciliation batch {} {}: channel={}, lines={}, inserted={}, skipped={}, restored={}, matched={}, pending={}",
                batch.getId(), existing != null ? "reparsed" : "imported", channel,
                bill.getItems().size(), inserted.size(), skipped, restored,
                batch.getMatchedCount(), batch.getUnmatchedCount());

        ReconciliationImportVO vo = new ReconciliationImportVO();
        vo.setDuplicate(false);
        vo.setMessage(existing != null ? "重新解析完成" : "导入完成");
        vo.setBatch(toBatchVO(batch));
        return vo;
    }

    // --------------------------------------------------------------- querying

    @Override
    public List<ReconciliationBatchVO> listBatches() {
        List<ReconciliationBatch> batches = batchMapper.selectList(
                new LambdaQueryWrapper<ReconciliationBatch>()
                        .orderByDesc(ReconciliationBatch::getCreatedAt)
                        .last("LIMIT 100"));
        return batches.stream().map(this::toBatchVO).toList();
    }

    @Override
    public ReconciliationBatchVO getBatch(Long batchId) {
        return toBatchVO(requireBatch(batchId));
    }

    @Override
    public PageResult<ReconciliationItemVO> listItems(Long batchId, String status, int page, int size) {
        requireBatch(batchId);
        LambdaQueryWrapper<ReconciliationItem> query = new LambdaQueryWrapper<ReconciliationItem>()
                .eq(ReconciliationItem::getBatchId, batchId);
        if ("MATCHED".equalsIgnoreCase(status)) {
            query.eq(ReconciliationItem::getMatchStatus, ReconcileConstants.STATUS_MATCHED);
        } else if ("PENDING".equalsIgnoreCase(status)) {
            query.in(ReconciliationItem::getMatchStatus,
                    ReconcileConstants.STATUS_UNMATCHED, ReconcileConstants.STATUS_AMBIGUOUS);
        } else if ("IGNORED".equalsIgnoreCase(status)) {
            query.eq(ReconciliationItem::getMatchStatus, ReconcileConstants.STATUS_IGNORED);
        } else if ("AMBIGUOUS".equalsIgnoreCase(status)) {
            query.eq(ReconciliationItem::getMatchStatus, ReconcileConstants.STATUS_AMBIGUOUS);
        }
        query.orderByDesc(ReconciliationItem::getTradeDate)
                .orderByDesc(ReconciliationItem::getPaidTime)
                .orderByDesc(ReconciliationItem::getId);

        Page<ReconciliationItem> result = itemMapper.selectPage(new Page<>(page, size), query);
        List<ReconciliationItem> records = result.getRecords();
        Map<Long, Transaction> txMap = loadTransactions(records.stream()
                .map(ReconciliationItem::getMatchTransactionId).filter(Objects::nonNull).toList());
        NameMaps names = loadNames(txMap.values());

        List<ReconciliationItemVO> items = records.stream()
                .map(item -> toItemVO(item, item.getMatchTransactionId() == null
                        ? null : txMap.get(item.getMatchTransactionId()), names))
                .toList();
        return PageResult.of(items, result.getTotal(), page, size);
    }

    @Override
    public List<MatchedTransactionVO> listUnmatchedTransactions(Long batchId) {
        ReconciliationBatch batch = requireBatch(batchId);
        BillChannel channel = BillChannel.of(batch.getChannel());
        if (channel == null) {
            return List.of();
        }
        Set<Long> linked = itemMapper.selectList(new LambdaQueryWrapper<ReconciliationItem>()
                        .eq(ReconciliationItem::getBatchId, batchId)
                        .isNotNull(ReconciliationItem::getMatchTransactionId))
                .stream().map(ReconciliationItem::getMatchTransactionId).collect(Collectors.toSet());

        List<Transaction> candidates = loadCandidateTransactions(batch).stream()
                // 候选池会向两侧多取 1 天用于匹配，但「流水有账单无」只展示账单周期内
                .filter(tx -> tx.getTransactionDate() != null
                        && !tx.getTransactionDate().isBefore(batch.getPeriodStart())
                        && !tx.getTransactionDate().isAfter(batch.getPeriodEnd()))
                .filter(tx -> reconcilableWith(tx, channel))
                .filter(tx -> channelCoversTransaction(channel, tx))
                .filter(tx -> !linked.contains(tx.getId()))
                .filter(tx -> channelAccepts(channel, tx))
                .sorted(Comparator.comparing(Transaction::getTransactionDate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Transaction::getTransactionTime,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(500)
                .toList();
        NameMaps names = loadNames(candidates);
        return candidates.stream().map(tx -> toMatchedTxVO(tx, names)).toList();
    }

    @Override
    public List<MatchedTransactionVO> listCandidates(Long itemId) {
        ReconciliationItem item = requireItem(itemId);
        Integer type = transactionType(item);
        if (type == null) {
            return List.of();
        }
        ReconciliationBatch batch = requireBatch(item.getBatchId());
        BillChannel channel = BillChannel.of(item.getChannel());
        LocalDate date = item.getTradeDate();
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<Transaction>()
                .ge(Transaction::getTransactionDate, date.minusDays(3))
                .le(Transaction::getTransactionDate, date.plusDays(3));
        if (type == 4) {
            // 账单退款行同时看支出流水，兼容邮件把退款记成支出的历史数据
            query.in(Transaction::getType, List.of(4, 1));
        } else {
            // 转账/收支在账本里可能被记成另一种类型，候选先放宽，再按摘要过滤
            query.in(Transaction::getType, List.of(1, 2, 3));
        }
        // 账单账户的流水包括「本账户支出」和「转入本账户」两类：
        // 后者 account_id 是对方账户、target_account_id 才是账单账户，只查 account_id 会漏掉
        if (batch.getAccountId() != null) {
            query.and(wrapper -> wrapper.eq(Transaction::getAccountId, batch.getAccountId())
                    .or().eq(Transaction::getTargetAccountId, batch.getAccountId()));
        }
        List<Transaction> txs = transactionMapper.selectList(query);
        LocalDateTime target = itemTime(item);
        List<Transaction> filtered = txs.stream()
                .filter(tx -> ReconcileConstants.matchesTransactionType(item.getDirection(),
                        item.getTradeStatus(), tx.getType(), tx.getNote()))
                .filter(tx -> channel == null || reconcilableWith(tx, channel))
                .filter(tx -> channelAccepts(channel, tx))
                // 只展示金额接近或时间接近的流水，避免把无关交易排进候选
                .filter(tx -> withinReasonableCandidate(tx, item, target))
                .sorted(Comparator
                        .comparing((Transaction tx) -> amountDistance(tx, item))
                        .thenComparingLong(tx -> timeDistanceSeconds(tx, target)))
                .limit(MAX_CANDIDATES)
                .toList();
        NameMaps names = loadNames(filtered);
        return filtered.stream().map(tx -> toMatchedTxVO(tx, names)).toList();
    }

    // ---------------------------------------------------------------- actions

    @Override
    @Transactional
    public ReconciliationItemVO match(Long itemId, Long transactionId) {
        ReconciliationItem item = requireItem(itemId);
        Transaction tx = transactionMapper.selectById(transactionId);
        if (tx == null) {
            throw new BizException(ErrorCode.TRANSACTION_NOT_FOUND);
        }
        BillChannel itemChannel = BillChannel.of(item.getChannel());
        BillChannel txChannel = BillChannel.of(tx.getReconcileChannel());
        // 同渠道的转账允许同时核对两个账户的对账单（转出侧 / 转入侧）
        if (Boolean.TRUE.equals(tx.getReconciled())
                && !Objects.equals(tx.getId(), item.getMatchTransactionId())
                && (txChannel == null || txChannel == itemChannel)
                && !Objects.equals(tx.getType(), 3)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该流水已在本渠道对账，请先取消原对账");
        }
        List<ReconciliationItem> usedByOthers = itemMapper.selectList(new LambdaQueryWrapper<ReconciliationItem>()
                .eq(ReconciliationItem::getMatchTransactionId, transactionId)
                .eq(ReconciliationItem::getMatchStatus, ReconcileConstants.STATUS_MATCHED)
                .ne(ReconciliationItem::getId, itemId));
        boolean transfer = Objects.equals(tx.getType(), 3);
        if (usedByOthers.stream().anyMatch(other ->
                Objects.equals(other.getChannel(), item.getChannel())
                        && (!transfer || Objects.equals(other.getBatchId(), item.getBatchId())))) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该流水已匹配本渠道的其他账单行");
        }

        Long previousTxId = item.getMatchTransactionId();
        item.setMatchStatus(ReconcileConstants.STATUS_MATCHED);
        item.setMatchType(ReconcileConstants.TYPE_MANUAL);
        item.setMatchTransactionId(transactionId);
        item.setMatchedAt(LocalDateTime.now());
        item.setIgnoreReason(null);
        persistMatch(item);

        if (previousTxId != null && !previousTxId.equals(transactionId)) {
            clearTransactionIfUnreferenced(previousTxId);
        }
        markTransaction(tx, item, item.getBatchId());
        refreshBatchCounts(requireBatch(item.getBatchId()));

        Transaction refreshed = transactionMapper.selectById(tx.getId());
        if (refreshed == null) {
            refreshed = tx;
        }
        NameMaps names = loadNames(List.of(refreshed));
        log.info("Reconciliation item matched: itemId={} transactionId={}", itemId, transactionId);
        return toItemVO(item, refreshed, names);
    }

    @Override
    @Transactional
    public ReconciliationItemVO unmatch(Long itemId) {
        ReconciliationItem item = requireItem(itemId);
        Long txId = item.getMatchTransactionId();
        // MyBatis-Plus 默认忽略 null 字段，清理关联必须显式 set NULL
        itemMapper.update(null, new LambdaUpdateWrapper<ReconciliationItem>()
                .eq(ReconciliationItem::getId, itemId)
                .set(ReconciliationItem::getMatchStatus, ReconcileConstants.STATUS_UNMATCHED)
                .set(ReconciliationItem::getMatchType, null)
                .set(ReconciliationItem::getMatchTransactionId, null)
                .set(ReconciliationItem::getMatchedAt, null)
                .set(ReconciliationItem::getIgnoreReason, null));
        item.setMatchStatus(ReconcileConstants.STATUS_UNMATCHED);
        item.setMatchType(null);
        item.setMatchTransactionId(null);
        item.setMatchedAt(null);
        item.setIgnoreReason(null);
        if (txId != null) {
            clearTransactionIfUnreferenced(txId);
        }
        refreshBatchCounts(requireBatch(item.getBatchId()));
        log.info("Reconciliation item unmatched: itemId={} transactionId={}", itemId, txId);
        return toItemVO(item, null, NameMaps.empty());
    }

    @Override
    @Transactional
    public ReconciliationItemVO ignore(Long itemId, String reason) {
        ReconciliationItem item = requireItem(itemId);
        Long txId = item.getMatchTransactionId();
        String ignoreReason = reason == null || reason.isBlank() ? "人工忽略" : reason.trim();
        itemMapper.update(null, new LambdaUpdateWrapper<ReconciliationItem>()
                .eq(ReconciliationItem::getId, itemId)
                .set(ReconciliationItem::getMatchStatus, ReconcileConstants.STATUS_IGNORED)
                .set(ReconciliationItem::getMatchType, null)
                .set(ReconciliationItem::getMatchTransactionId, null)
                .set(ReconciliationItem::getMatchedAt, null)
                .set(ReconciliationItem::getIgnoreReason, ignoreReason));
        item.setMatchStatus(ReconcileConstants.STATUS_IGNORED);
        item.setMatchType(null);
        item.setMatchTransactionId(null);
        item.setMatchedAt(null);
        item.setIgnoreReason(ignoreReason);
        if (txId != null) {
            clearTransactionIfUnreferenced(txId);
        }
        refreshBatchCounts(requireBatch(item.getBatchId()));
        log.info("Reconciliation item ignored: itemId={} reason={}", itemId, ignoreReason);
        return toItemVO(item, null, NameMaps.empty());
    }

    @Override
    @Transactional
    public ReconciliationItemVO createTransaction(Long itemId, ReconciliationCreateTxDTO dto) {
        ReconciliationItem item = requireItem(itemId);
        boolean selfTransfer = isSelfTransfer(item);
        Integer inferredType = transactionType(item);
        if (inferredType == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该账单行不支持补录为交易");
        }
        // 本人账户互转只能按转账补录；外部转账（如租金/租车费）允许改选支出/收入
        Integer type = selfTransfer ? 3 : (dto.getType() == null ? inferredType : dto.getType());
        if (type < 1 || type > 4) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不支持的补录类型");
        }
        if (type == 3 && dto.getTargetAccountId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    selfTransfer ? "本人账户互转请选择转出和转入账户" : "转账补录请选择转出和转入账户");
        }
        if (type != 3 && dto.getCategoryId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择分类");
        }
        if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "零金额账单行不能补录");
        }

        TransactionCreateDTO create = new TransactionCreateDTO();
        create.setType(type);
        create.setAmount(item.getAmount());
        create.setCurrency("CNY");
        create.setTransactionDate(item.getTradeDate());
        LocalDateTime time = item.getPaidTime() != null ? item.getPaidTime() : item.getCreatedTime();
        create.setTransactionTime(time == null ? LocalTime.NOON : time.toLocalTime());
        create.setAccountId(dto.getAccountId());
        create.setTargetAccountId(type == 3 ? dto.getTargetAccountId() : null);
        create.setCategoryId(dto.getCategoryId());
        String note = item.getSubject();
        if (note == null || note.isBlank()) {
            note = ((item.getTradeStatus() == null ? "" : item.getTradeStatus()) + " "
                    + (item.getCounterparty() == null ? "" : item.getCounterparty())).trim();
        }
        create.setNote(truncate(note, 200));
        create.setMerchantName(truncate(item.getCounterparty(), 50));
        create.setExternalId("recon:" + item.getChannel() + ":" + item.getItemKey());
        create.setExtFields(new LinkedHashMap<>(billExtFields(item)));
        create.setTags(List.of("对账补录"));
        // 普通收支保持默认入账；历史漏记的转账默认不计入余额，避免重复影响当前余额
        create.setIsConfirmed(dto.getConfirmed() != null ? dto.getConfirmed() : type != 3);
        create.setReviewStatus("REVIEWED");

        TransactionVO created = transactionService.create(create);
        Transaction tx = transactionMapper.selectById(created.getId());

        item.setMatchStatus(ReconcileConstants.STATUS_MATCHED);
        item.setMatchType(ReconcileConstants.TYPE_CREATED);
        item.setMatchTransactionId(created.getId());
        item.setMatchedAt(LocalDateTime.now());
        item.setIgnoreReason(null);
        persistMatch(item);

        if (tx != null) {
            markTransaction(tx, item, item.getBatchId());
        }
        refreshBatchCounts(requireBatch(item.getBatchId()));
        Transaction refreshed = tx == null ? null : transactionMapper.selectById(tx.getId());
        NameMaps names = refreshed != null ? loadNames(List.of(refreshed)) : NameMaps.empty();
        log.info("Transaction created from reconciliation item: itemId={} transactionId={}",
                itemId, created.getId());
        return toItemVO(item, refreshed, names);
    }

    @Override
    @Transactional
    public ReconciliationBatchVO rematch(Long batchId) {
        ReconciliationBatch batch = requireBatch(batchId);
        List<ReconciliationItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<ReconciliationItem>().eq(ReconciliationItem::getBatchId, batchId));

        Set<Long> autoTxIds = items.stream()
                .filter(i -> ReconcileConstants.STATUS_MATCHED.equals(i.getMatchStatus()))
                .filter(i -> ReconcileConstants.TYPE_AUTO.equals(i.getMatchType())
                        || ReconcileConstants.TYPE_AGGREGATE.equals(i.getMatchType()))
                .map(ReconciliationItem::getMatchTransactionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        itemMapper.resetAutoMatches(batchId);
        for (Long txId : autoTxIds) {
            clearTransactionIfUnreferenced(txId);
        }

        List<ReconciliationItem> refreshed = itemMapper.selectList(
                new LambdaQueryWrapper<ReconciliationItem>().eq(ReconciliationItem::getBatchId, batchId));
        BillChannel channel = BillChannel.of(batch.getChannel());
        if (channel != null) {
            List<Transaction> candidates = loadCandidateTransactions(batch);
            releaseStaleChannelMarkers(candidates, channel);
            List<ReconciliationMatcher.ItemMatch> matches = matcher.match(refreshed, candidates, channel);
            applyMatches(refreshed, candidates, matches, batchId);
        }
        refreshBatchCounts(batch);
        log.info("Rematched reconciliation batch: batchId={} channel={}", batchId, batch.getChannel());
        return toBatchVO(batch);
    }

    @Override
    @Transactional
    public void deleteBatch(Long batchId) {
        ReconciliationBatch batch = requireBatch(batchId);
        List<Long> linkedTxIds = itemMapper.selectList(new LambdaQueryWrapper<ReconciliationItem>()
                        .eq(ReconciliationItem::getBatchId, batchId)
                        .eq(ReconciliationItem::getMatchStatus, ReconcileConstants.STATUS_MATCHED)
                        .isNotNull(ReconciliationItem::getMatchTransactionId))
                .stream().map(ReconciliationItem::getMatchTransactionId)
                .filter(Objects::nonNull).distinct().toList();
        itemMapper.delete(new LambdaQueryWrapper<ReconciliationItem>()
                .eq(ReconciliationItem::getBatchId, batchId));
        batchMapper.deleteById(batchId);
        // 只清理没有其它渠道账单行引用的流水，避免误删交叉核对标记
        for (Long txId : linkedTxIds) {
            clearTransactionIfUnreferenced(txId);
        }
        log.info("Deleted reconciliation batch {} ({})", batchId, batch.getChannel());
    }

    // -------------------------------------------------------------- internals

    private ReconciliationBatch requireBatch(Long batchId) {
        ReconciliationBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "对账批次");
        }
        return batch;
    }

    /** 上传组件缺失文件名时的兜底名称，按渠道使用对应扩展名 */
    private String defaultFileName(BillChannel channel) {
        return switch (channel) {
            case WECHAT -> "WECHAT.xlsx";
            case CMB_DEBIT, CMB_CREDIT -> channel.name() + ".pdf";
            default -> channel.name() + ".csv";
        };
    }

    private ReconciliationItem requireItem(Long itemId) {
        ReconciliationItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "账单明细");
        }
        return item;
    }

    /** 重新解析时保留用户已经做过的决定 */
    private boolean isPreserved(ReconciliationItem item) {
        if (ReconcileConstants.STATUS_IGNORED.equals(item.getMatchStatus())) {
            return true;
        }
        return ReconcileConstants.STATUS_MATCHED.equals(item.getMatchStatus())
                && (ReconcileConstants.TYPE_MANUAL.equals(item.getMatchType())
                || ReconcileConstants.TYPE_CREATED.equals(item.getMatchType()));
    }

    private void copyMatchState(ReconciliationItem target, ReconciliationItem source) {
        target.setMatchStatus(source.getMatchStatus());
        target.setMatchType(source.getMatchType());
        target.setMatchTransactionId(source.getMatchTransactionId());
        target.setMatchedAt(source.getMatchedAt());
        target.setIgnoreReason(source.getIgnoreReason());
    }

    private ReconciliationItem toEntity(Long batchId, BillChannel channel, ParsedBillItem parsed) {
        ReconciliationItem item = new ReconciliationItem();
        item.setBatchId(batchId);
        item.setChannel(channel.name());
        item.setItemKey(parsed.getItemKey());
        item.setBillNo(parsed.getBillNo());
        item.setMerchantOrderNo(parsed.getMerchantOrderNo());
        item.setCreatedTime(parsed.getCreatedTime());
        item.setPaidTime(parsed.getPaidTime());
        item.setTradeDate(parsed.getTradeDate());
        item.setDirection(parsed.getDirection());
        item.setAmount(parsed.getAmount());
        item.setCounterparty(truncate(parsed.getCounterparty(), 255));
        item.setSubject(truncate(parsed.getSubject(), 500));
        item.setTradeStatus(truncate(parsed.getTradeStatus(), 32));
        item.setFundStatus(truncate(parsed.getFundStatus(), 32));
        item.setSource(truncate(parsed.getSource(), 64));
        item.setOrderType(truncate(parsed.getOrderType(), 64));
        item.setFee(parsed.getFee() == null ? BigDecimal.ZERO : parsed.getFee());
        item.setRefunded(parsed.getRefunded() == null ? BigDecimal.ZERO : parsed.getRefunded());
        item.setRemark(truncate(parsed.getRemark(), 500));
        item.setRawJson(writeJson(parsed.getRaw()));
        item.setMatchStatus(ReconcileConstants.STATUS_UNMATCHED);
        // 朝朝宝申赎、大额存单到期还本都是招行账户内部理财处理，不是对外收支，自动忽略
        if (channel == BillChannel.CMB_DEBIT && parsed.getTradeStatus() != null) {
            String status = parsed.getTradeStatus();
            if (status.contains("朝朝宝")) {
                item.setMatchStatus(ReconcileConstants.STATUS_IGNORED);
                item.setIgnoreReason(ReconcileConstants.IGNORE_CMB_INTERNAL);
            } else if (status.contains("大额存单")) {
                item.setMatchStatus(ReconcileConstants.STATUS_IGNORED);
                item.setIgnoreReason(ReconcileConstants.IGNORE_CMB_CD_MATURITY);
            }
        }
        return item;
    }

    private void applyMatches(List<ReconciliationItem> items, List<Transaction> candidates,
                              List<ReconciliationMatcher.ItemMatch> matches, Long batchId) {
        Map<Long, ReconciliationItem> itemMap = items.stream()
                .collect(Collectors.toMap(ReconciliationItem::getId, Function.identity(), (a, b) -> a));
        Map<Long, Transaction> txMap = candidates.stream()
                .collect(Collectors.toMap(Transaction::getId, Function.identity(), (a, b) -> a));
        Map<Long, Map<String, Object>> extCache = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (ReconciliationMatcher.ItemMatch match : matches) {
            ReconciliationItem item = itemMap.get(match.itemId());
            if (item == null) {
                continue;
            }
            item.setMatchStatus(match.status());
            if (ReconcileConstants.STATUS_MATCHED.equals(match.status())) {
                item.setMatchType(match.matchType());
                item.setMatchTransactionId(match.transactionId());
                item.setMatchedAt(now);
                item.setIgnoreReason(null);
                Transaction tx = txMap.get(match.transactionId());
                if (tx != null) {
                    Map<String, Object> ext = extCache.computeIfAbsent(tx.getId(),
                            id -> parseExtFields(tx.getExtFields()));
                    mergeBillExtFields(ext, item);
                    if (canMark(tx, item)) {
                        transactionMapper.markReconciled(tx.getId(), item.getChannel(), batchId,
                                writeJson(ext));
                    } else {
                        // 已被其它渠道对账：只补充本渠道的扩展字段，保留原有对账标记
                        transactionMapper.updateExtFields(tx.getId(), writeJson(ext));
                    }
                }
            } else if (ReconcileConstants.STATUS_IGNORED.equals(match.status())) {
                item.setMatchType(null);
                item.setMatchTransactionId(null);
                item.setMatchedAt(null);
                if (item.getIgnoreReason() == null || item.getIgnoreReason().isBlank()) {
                    item.setIgnoreReason(ReconcileConstants.IGNORE_ZERO_AMOUNT);
                }
            } else {
                item.setMatchType(null);
                item.setMatchTransactionId(null);
                item.setMatchedAt(null);
                item.setIgnoreReason(null);
            }
            persistMatch(item);
        }
    }

    private void markTransaction(Transaction tx, ReconciliationItem item, Long batchId) {
        Map<String, Object> ext = parseExtFields(tx.getExtFields());
        mergeBillExtFields(ext, item);
        if (canMark(tx, item)) {
            transactionMapper.markReconciled(tx.getId(), item.getChannel(), batchId, writeJson(ext));
        } else {
            transactionMapper.updateExtFields(tx.getId(), writeJson(ext));
        }
    }

    /** 同一渠道的旧扩展字段先清掉再写入，避免重复导入/重匹配累积过期键 */
    private void mergeBillExtFields(Map<String, Object> target, ReconciliationItem item) {
        String prefix = extPrefix(item.getChannel());
        target.keySet().removeIf(key -> key.startsWith(prefix));
        target.putAll(billExtFields(item));
    }

    private String extPrefix(String channel) {
        return switch (channel == null ? "" : channel) {
            case "ALIPAY" -> "支付宝";
            case "WECHAT" -> "微信";
            case "CMB_DEBIT" -> "招行储蓄卡";
            case "CMB_CREDIT" -> "招行信用卡";
            case "CMB" -> "招行";
            default -> channel;
        };
    }

    /**
     * 一笔流水可以被多个渠道的账单同时核对（支付宝账单 + 信用卡账单）。
     * 交易表只有一组对账字段，保留最先写入的渠道；跨渠道只记在账单明细上，
     * 不覆盖交易本身的对账标记。
     */
    private boolean canMark(Transaction tx, ReconciliationItem item) {
        if (!Boolean.TRUE.equals(tx.getReconciled())) {
            return true;
        }
        BillChannel existing = BillChannel.of(tx.getReconcileChannel());
        if (existing == null) {
            return true;
        }
        if (!Objects.equals(existing.name(), item.getChannel())) {
            return false;
        }
        // 同渠道跨批次共享的转账保留最先写入的批次，后一次只合并扩展字段
        return Objects.equals(tx.getReconcileBatchId(), item.getBatchId());
    }

    /** 信用卡账单只覆盖消费与退款；还款/转账流水不算「流水有账单无」 */
    private boolean channelCoversTransaction(BillChannel channel, Transaction tx) {
        if (channel == BillChannel.CMB_CREDIT) {
            return tx.getType() != null && (tx.getType() == 1 || tx.getType() == 4);
        }
        return true;
    }

    /** 流水是否还能作为当前渠道的候选：同渠道已对账的排除，其它渠道的保留作交叉核对 */
    private boolean reconcilableWith(Transaction tx, BillChannel channel) {
        if (!Boolean.TRUE.equals(tx.getReconciled())) {
            return true;
        }
        BillChannel existing = BillChannel.of(tx.getReconcileChannel());
        if (existing == null) {
            return false;
        }
        if (existing != channel) {
            return true;
        }
        // 同渠道的转账可以在两个账户的对账单里各核对一次（转出侧 / 转入侧）
        return Objects.equals(tx.getType(), 3);
    }

    /**
     * 跨渠道对账时交易只保留最先写入的渠道标记。若该渠道的账单行已被重新匹配清掉，
     * 而标记没有释放（因为其它渠道的账单行还引用着这笔流水），本渠道重匹配时会把
     * 自己的旧标记当成「已对账」而跳过这笔流水，导致大量账单行无故变成待处理。
     * 重匹配前把这类残留标记释放掉；其它渠道的标记不动。
     */
    private void releaseStaleChannelMarkers(List<Transaction> candidates, BillChannel channel) {
        if (channel == null || candidates.isEmpty()) {
            return;
        }
        List<Transaction> stale = candidates.stream()
                .filter(tx -> Boolean.TRUE.equals(tx.getReconciled()))
                .filter(tx -> BillChannel.of(tx.getReconcileChannel()) == channel)
                .toList();
        if (stale.isEmpty()) {
            return;
        }
        Set<Long> ids = stale.stream().map(Transaction::getId).collect(Collectors.toSet());
        Set<Long> referenced = itemMapper.selectList(new LambdaQueryWrapper<ReconciliationItem>()
                        .in(ReconciliationItem::getMatchTransactionId, ids)
                        .eq(ReconciliationItem::getMatchStatus, ReconcileConstants.STATUS_MATCHED)
                        .eq(ReconciliationItem::getChannel, channel.name()))
                .stream()
                .map(ReconciliationItem::getMatchTransactionId)
                .collect(Collectors.toSet());
        for (Transaction tx : stale) {
            if (referenced.contains(tx.getId())) {
                continue;
            }
            transactionMapper.clearReconciled(tx.getId());
            tx.setReconciled(false);
            tx.setReconcileChannel(null);
            tx.setReconcileBatchId(null);
            tx.setReconciledAt(null);
        }
    }

    private void clearTransactionIfUnreferenced(Long txId) {
        long stillUsed = itemMapper.selectCount(new LambdaQueryWrapper<ReconciliationItem>()
                .eq(ReconciliationItem::getMatchTransactionId, txId)
                .eq(ReconciliationItem::getMatchStatus, ReconcileConstants.STATUS_MATCHED));
        if (stillUsed == 0) {
            transactionMapper.clearReconciled(txId);
        }
    }

    /** 显式写对账字段，确保 null 也能覆盖旧值（updateById 默认跳过 null） */
    private void persistMatch(ReconciliationItem item) {
        itemMapper.update(null, new LambdaUpdateWrapper<ReconciliationItem>()
                .eq(ReconciliationItem::getId, item.getId())
                .set(ReconciliationItem::getMatchStatus, item.getMatchStatus())
                .set(ReconciliationItem::getMatchType, item.getMatchType())
                .set(ReconciliationItem::getMatchTransactionId, item.getMatchTransactionId())
                .set(ReconciliationItem::getMatchedAt, item.getMatchedAt())
                .set(ReconciliationItem::getIgnoreReason, item.getIgnoreReason()));
    }

    private void refreshBatchCounts(ReconciliationBatch batch) {
        statsUpdater.refresh(batch);
    }

    private List<Transaction> loadCandidateTransactions(ReconciliationBatch batch) {
        LocalDate start = batch.getPeriodStart().minusDays(1);
        LocalDate end = batch.getPeriodEnd().plusDays(1);
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<Transaction>()
                .ge(Transaction::getTransactionDate, start)
                .le(Transaction::getTransactionDate, end);
        if (batch.getAccountId() != null) {
            query.and(wrapper -> wrapper.eq(Transaction::getAccountId, batch.getAccountId())
                    .or().eq(Transaction::getTargetAccountId, batch.getAccountId()));
        }
        return transactionMapper.selectList(query);
    }

    private Map<Long, Transaction> loadTransactions(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return transactionMapper.selectBatchIds(ids.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(Transaction::getId, Function.identity(), (a, b) -> a));
    }

    private NameMaps loadNames(Collection<Transaction> txs) {
        if (txs == null || txs.isEmpty()) {
            return NameMaps.empty();
        }
        Set<Long> accountIds = txs.stream().map(Transaction::getAccountId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> merchantIds = txs.stream().map(Transaction::getMerchantId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> categoryIds = txs.stream().map(Transaction::getCategoryId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Account> accounts = accountIds.isEmpty() ? java.util.Collections.emptyMap()
                : accountMapper.selectBatchIds(accountIds).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity(), (a, b) -> a));
        Map<Long, Merchant> merchants = merchantIds.isEmpty() ? java.util.Collections.emptyMap()
                : merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, Function.identity(), (a, b) -> a));
        Map<Long, Category> categories = categoryIds.isEmpty() ? java.util.Collections.emptyMap()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity(), (a, b) -> a));
        return new NameMaps(accounts, merchants, categories);
    }

    private ReconciliationBatchVO toBatchVO(ReconciliationBatch batch) {
        ReconciliationBatchVO vo = new ReconciliationBatchVO();
        vo.setId(batch.getId());
        vo.setChannel(batch.getChannel());
        BillChannel channel = BillChannel.of(batch.getChannel());
        vo.setChannelLabel(channel == null ? batch.getChannel() : channel.getLabel());
        vo.setFileName(batch.getFileName());
        vo.setAccountId(batch.getAccountId());
        if (batch.getAccountId() != null) {
            Account account = accountMapper.selectById(batch.getAccountId());
            vo.setAccountName(account == null ? null : account.getName());
        }
        vo.setBillAccount(batch.getBillAccount());
        vo.setPeriodStart(batch.getPeriodStart());
        vo.setPeriodEnd(batch.getPeriodEnd());
        vo.setTotalCount(batch.getTotalCount());
        vo.setTotalAmount(batch.getTotalAmount());
        vo.setMatchedCount(batch.getMatchedCount());
        vo.setUnmatchedCount(batch.getUnmatchedCount());
        vo.setIgnoredCount(batch.getIgnoredCount());
        vo.setAmbiguousCount(Math.toIntExact(itemMapper.selectCount(
                new LambdaQueryWrapper<ReconciliationItem>()
                        .eq(ReconciliationItem::getBatchId, batch.getId())
                        .eq(ReconciliationItem::getMatchStatus, ReconcileConstants.STATUS_AMBIGUOUS))));
        vo.setStatus(batch.getStatus());
        vo.setSummary(parseMap(batch.getSummaryJson()));
        vo.setCreatedAt(batch.getCreatedAt());
        vo.setUpdatedAt(batch.getUpdatedAt());
        return vo;
    }

    private ReconciliationItemVO toItemVO(ReconciliationItem item, Transaction tx, NameMaps names) {
        ReconciliationItemVO vo = new ReconciliationItemVO();
        vo.setId(item.getId());
        vo.setBatchId(item.getBatchId());
        vo.setChannel(item.getChannel());
        vo.setBillNo(item.getBillNo());
        vo.setMerchantOrderNo(item.getMerchantOrderNo());
        vo.setCreatedTime(item.getCreatedTime());
        vo.setPaidTime(item.getPaidTime());
        vo.setTradeDate(item.getTradeDate());
        vo.setDirection(item.getDirection());
        vo.setDirectionLabel(switch (item.getDirection() == null ? "" : item.getDirection()) {
            case ReconcileConstants.DIRECTION_EXPENSE -> "支出";
            case ReconcileConstants.DIRECTION_INCOME -> "收入";
            default -> "不计收支";
        });
        vo.setAmount(item.getAmount());
        vo.setCounterparty(item.getCounterparty());
        vo.setSubject(item.getSubject());
        vo.setTradeStatus(item.getTradeStatus());
        vo.setFundStatus(item.getFundStatus());
        vo.setSource(item.getSource());
        vo.setOrderType(item.getOrderType());
        vo.setFee(item.getFee());
        vo.setRefunded(item.getRefunded());
        vo.setRemark(item.getRemark());
        vo.setSelfTransfer(isSelfTransfer(item));
        vo.setMatchStatus(item.getMatchStatus());
        vo.setMatchType(item.getMatchType());
        vo.setMatchTransactionId(item.getMatchTransactionId());
        vo.setMatchedAt(item.getMatchedAt());
        vo.setIgnoreReason(item.getIgnoreReason());
        vo.setRaw(parseStringMap(item.getRawJson()));
        if (tx != null) {
            vo.setTransaction(toMatchedTxVO(tx, names));
        }
        return vo;
    }

    private MatchedTransactionVO toMatchedTxVO(Transaction tx, NameMaps names) {
        MatchedTransactionVO vo = new MatchedTransactionVO();
        vo.setId(tx.getId());
        vo.setType(tx.getType());
        vo.setAmount(tx.getAmount());
        vo.setAmountBase(tx.getAmountBase());
        vo.setCurrency(tx.getCurrency());
        vo.setTransactionDate(tx.getTransactionDate());
        vo.setTransactionTime(tx.getTransactionTime());
        vo.setAccountId(tx.getAccountId());
        Account account = tx.getAccountId() == null ? null : names.accounts().get(tx.getAccountId());
        vo.setAccountName(account == null ? null : account.getName());
        Merchant merchant = tx.getMerchantId() == null ? null : names.merchants().get(tx.getMerchantId());
        vo.setMerchantName(merchant == null ? null : merchant.getName());
        Category category = tx.getCategoryId() == null ? null : names.categories().get(tx.getCategoryId());
        vo.setCategoryName(category == null ? null : category.getName());
        vo.setNote(tx.getNote());
        vo.setIsConfirmed(tx.getIsConfirmed());
        vo.setReviewStatus(tx.getReviewStatus());
        vo.setReconciled(tx.getReconciled());
        vo.setReconcileChannel(tx.getReconcileChannel());
        return vo;
    }

    private Integer transactionType(ReconciliationItem item) {
        if (isSelfTransfer(item)) {
            return 3;
        }
        return ReconcileConstants.transactionType(item.getDirection(), item.getTradeStatus());
    }

    /**
     * 本人账户互转：对手名是账户本人（PDF 户名或配置的 self-names），
     * 且摘要确实是转账/转入/转出。这类只影响余额，不能补录成收入或支出。
     */
    private boolean isSelfTransfer(ReconciliationItem item) {
        String counterparty = item.getCounterparty();
        String status = item.getTradeStatus();
        if (counterparty == null || counterparty.isBlank() || status == null) {
            return false;
        }
        if (!status.contains("转账") && !status.contains("转入") && !status.contains("转出")) {
            return false;
        }
        String name = counterparty.trim();
        Map<String, String> raw = parseStringMap(item.getRawJson());
        String holder = raw.get("户名");
        if (holder != null && holder.trim().equals(name)) {
            return true;
        }
        return selfNames().contains(name);
    }

    private Set<String> selfNames() {
        if (selfNamesConfig == null || selfNamesConfig.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(selfNamesConfig.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());
    }

    private Map<String, Object> billExtFields(ReconciliationItem item) {
        if ("CMB_CREDIT".equals(item.getChannel())) {
            return cmbCreditExtFields(item);
        }
        if ("CMB_DEBIT".equals(item.getChannel())) {
            return cmbDebitExtFields(item);
        }
        if ("WECHAT".equals(item.getChannel())) {
            return wechatExtFields(item);
        }
        String prefix = extPrefix(item.getChannel());
        Map<String, Object> ext = new LinkedHashMap<>();
        putIfText(ext, prefix + "交易号", item.getBillNo());
        putIfText(ext, prefix + "商家订单号", item.getMerchantOrderNo());
        putIfText(ext, prefix + "交易创建时间", formatDateTime(item.getCreatedTime()));
        putIfText(ext, prefix + "付款时间", formatDateTime(item.getPaidTime()));
        putIfText(ext, prefix + "交易对方", item.getCounterparty());
        putIfText(ext, prefix + "商品名称", item.getSubject());
        putIfText(ext, prefix + "交易状态", item.getTradeStatus());
        putIfText(ext, prefix + "资金状态", item.getFundStatus());
        putIfText(ext, prefix + "交易来源地", item.getSource());
        putIfText(ext, prefix + "业务类型", item.getOrderType());
        if (item.getFee() != null && item.getFee().compareTo(BigDecimal.ZERO) > 0) {
            ext.put(prefix + "服务费", item.getFee());
        }
        if (item.getRefunded() != null && item.getRefunded().compareTo(BigDecimal.ZERO) > 0) {
            ext.put(prefix + "成功退款", item.getRefunded());
        }
        putIfText(ext, prefix + "备注", item.getRemark());
        return ext;
    }

    /** 信用卡账单能提供的额外信息：卡号、交易日/记账日、摘要、账单分类、交易地金额 */
    private Map<String, Object> cmbCreditExtFields(ReconciliationItem item) {
        Map<String, String> raw = parseStringMap(item.getRawJson());
        Map<String, Object> ext = new LinkedHashMap<>();
        putIfText(ext, "招行信用卡卡号末四位", raw.get("卡号末四位"));
        putIfText(ext, "招行信用卡交易日", raw.get("交易日"));
        putIfText(ext, "招行信用卡记账日", raw.get("记账日"));
        putIfText(ext, "招行信用卡交易摘要", item.getCounterparty());
        putIfText(ext, "招行信用卡账单分类", item.getOrderType());
        String original = raw.get("交易地金额");
        String currency = raw.get("交易地币种");
        if (original != null && !original.isBlank()) {
            String suffix = switch (currency == null ? "" : currency.toUpperCase(java.util.Locale.ROOT)) {
                case "", "CN", "CNY" -> " CNY";
                case "HK", "HKD" -> " HKD";
                case "US", "USD" -> " USD";
                default -> " " + currency;
            };
            ext.put("招行信用卡交易地金额", original + suffix);
        }
        putIfText(ext, "招行信用卡账单来源", item.getSource());
        return ext;
    }

    /** 微信账单能提供的额外信息：支付方式、交易类型、当前状态、交易单号、昵称 */
    private Map<String, Object> wechatExtFields(ReconciliationItem item) {
        Map<String, String> raw = parseStringMap(item.getRawJson());
        Map<String, Object> ext = new LinkedHashMap<>();
        putIfText(ext, "微信昵称", raw.get("微信昵称"));
        putIfText(ext, "微信支付方式", raw.get("支付方式"));
        putIfText(ext, "微信交易类型", item.getTradeStatus());
        putIfText(ext, "微信当前状态", item.getFundStatus());
        putIfText(ext, "微信交易单号", item.getBillNo());
        putIfText(ext, "微信商户单号", item.getMerchantOrderNo());
        putIfText(ext, "微信商品", item.getSubject());
        putIfText(ext, "微信备注", item.getRemark());
        putIfText(ext, "微信账单来源", item.getSource());
        return ext;
    }

    /** 储蓄卡流水能提供的额外信息：账号、记账日期、摘要、联机余额、币种 */
    private Map<String, Object> cmbDebitExtFields(ReconciliationItem item) {
        Map<String, String> raw = parseStringMap(item.getRawJson());
        Map<String, Object> ext = new LinkedHashMap<>();
        putIfText(ext, "招行储蓄卡账号", raw.get("账号"));
        putIfText(ext, "招行储蓄卡记账日期", raw.get("记账日期"));
        putIfText(ext, "招行储蓄卡交易摘要", raw.get("交易摘要"));
        putIfText(ext, "招行储蓄卡联机余额", raw.get("联机余额"));
        putIfText(ext, "招行储蓄卡交易币种", raw.get("币种"));
        putIfText(ext, "招行储蓄卡对手信息", item.getCounterparty());
        putIfText(ext, "招行储蓄卡流水来源", item.getSource());
        return ext;
    }

    private void putIfText(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value.trim());
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString().replace('T', ' ');
    }

    private LocalDateTime itemTime(ReconciliationItem item) {
        if (item.getPaidTime() != null) {
            return item.getPaidTime();
        }
        if (item.getCreatedTime() != null) {
            return item.getCreatedTime();
        }
        return item.getTradeDate() == null
                ? LocalDateTime.now() : item.getTradeDate().atTime(LocalTime.NOON);
    }

    private long timeDistanceSeconds(Transaction tx, LocalDateTime target) {
        LocalDate date = tx.getTransactionDate() == null ? LocalDate.now() : tx.getTransactionDate();
        LocalDateTime time = tx.getTransactionTime() == null
                ? date.atTime(LocalTime.NOON) : date.atTime(tx.getTransactionTime());
        return Math.abs(java.time.temporal.ChronoUnit.SECONDS.between(time, target));
    }

    /** 金额差在 2 元或 20% 以内，或者时间差在 2 小时以内，才作为候选 */
    private boolean withinReasonableCandidate(Transaction tx, ReconciliationItem item, LocalDateTime target) {
        if (item.getAmount() == null) {
            return true;
        }
        BigDecimal limit = BigDecimal.valueOf(Math.max(2.0, item.getAmount().doubleValue() * 0.2));
        if (amountDistance(tx, item).compareTo(limit) <= 0) {
            return true;
        }
        return timeDistanceSeconds(tx, target) <= 7200;
    }

    private BigDecimal amountDistance(Transaction tx, ReconciliationItem item) {
        BigDecimal amount = tx.getAmountBase() != null ? tx.getAmountBase() : tx.getAmount();
        if (amount == null || item.getAmount() == null) {
            return new BigDecimal("999999999");
        }
        return amount.subtract(item.getAmount()).abs();
    }

    /** 渠道识别：微信账单还接受对账补录生成的、没有渠道关键字的银行侧交易 */
    private boolean channelAccepts(BillChannel channel, Transaction tx) {
        if (channel == null) {
            return true;
        }
        if (channel.matches(extChannel(tx), tx.getNote())) {
            return true;
        }
        return channel == BillChannel.WECHAT && ReconcileConstants.isBackfilled(tx);
    }

    private String extChannel(Transaction tx) {
        Map<String, Object> ext = parseExtFields(tx.getExtFields());
        Object value = ext.get("channel");
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> parseExtFields(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, String> parseStringMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception e) {
            return Integer.toHexString(java.util.Arrays.hashCode(content));
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private record NameMaps(Map<Long, Account> accounts, Map<Long, Merchant> merchants,
                            Map<Long, Category> categories) {
        static NameMaps empty() {
            return new NameMaps(java.util.Collections.emptyMap(),
                    java.util.Collections.emptyMap(), java.util.Collections.emptyMap());
        }
    }
}