package uk.gubin.budgetpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.dto.PrepaidCardAdjustDTO;
import uk.gubin.budgetpilot.dto.PrepaidCardCreateDTO;
import uk.gubin.budgetpilot.dto.PrepaidCardUpdateDTO;
import uk.gubin.budgetpilot.entity.PrepaidCard;
import uk.gubin.budgetpilot.entity.PrepaidCardUsage;
import uk.gubin.budgetpilot.mapper.PrepaidCardMapper;
import uk.gubin.budgetpilot.mapper.PrepaidCardUsageMapper;
import uk.gubin.budgetpilot.service.PrepaidCardService;
import uk.gubin.budgetpilot.vo.PrepaidCardSummaryVO;
import uk.gubin.budgetpilot.vo.PrepaidCardUsageVO;
import uk.gubin.budgetpilot.vo.PrepaidCardVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 卡券包实现。
 *
 * <p>刻意不继承 ServiceImpl、不碰账户和交易：卡券是表外备忘，
 * 任何操作都只改 t_prepaid_card / t_prepaid_card_usage。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrepaidCardServiceImpl implements PrepaidCardService {

    static final String STATE_ACTIVE = "ACTIVE";
    static final String STATE_USED_UP = "USED_UP";
    static final String STATE_EXPIRED = "EXPIRED";
    static final String STATE_VOID = "VOID";

    static final int ACTION_INIT = 1;
    static final int ACTION_USE = 2;
    static final int ACTION_RECHARGE = 3;
    static final int ACTION_ADJUST = 4;
    static final int ACTION_VOID = 5;
    static final int ACTION_RESTORE = 6;

    private static final int EXPIRING_DAYS = 30;
    private static final int DORMANT_DAYS = 90;
    private static final BigDecimal ZERO =
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final PrepaidCardMapper cardMapper;
    private final PrepaidCardUsageMapper usageMapper;

    @Override
    public List<PrepaidCardVO> list(String state, String keyword, String channel,
                                    Integer kind, Integer source) {
        LocalDate today = LocalDate.now();
        String normalizedState = state == null ? "" : state.trim().toUpperCase(Locale.ROOT);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        List<PrepaidCardVO> result = new ArrayList<>();
        for (PrepaidCard card : listAll()) {
            PrepaidCardVO vo = toVO(card, today);
            if (!matchesState(vo, normalizedState)) {
                continue;
            }
            if (!normalizedKeyword.isEmpty() && !matchesKeyword(vo, normalizedKeyword)) {
                continue;
            }
            if (channel != null && !channel.isBlank() && !channel.equals(vo.getChannel())) {
                continue;
            }
            if (kind != null && !kind.equals(vo.getKind())) {
                continue;
            }
            if (source != null && !source.equals(vo.getSource())) {
                continue;
            }
            result.add(vo);
        }
        result.sort(cardComparator());
        return result;
    }

    @Override
    public List<PrepaidCard> listAll() {
        return cardMapper.selectList(new LambdaQueryWrapper<PrepaidCard>()
                .orderByDesc(PrepaidCard::getId));
    }

    @Override
    public PrepaidCardVO get(Long id) {
        return toVO(requireCard(id), LocalDate.now());
    }

    @Override
    public PrepaidCardSummaryVO summary() {
        LocalDate today = LocalDate.now();
        List<PrepaidCard> cards = listAll();

        PrepaidCardSummaryVO summary = new PrepaidCardSummaryVO();
        summary.setAsOf(today);
        summary.setTotalCount(cards.size());

        BigDecimal moneyBalance = ZERO;
        BigDecimal countBalance = ZERO;
        BigDecimal expiringMoney = ZERO;
        BigDecimal expiringCountValue = ZERO;
        BigDecimal expiredMoney = ZERO;
        BigDecimal expiredCountValue = ZERO;
        int active = 0;
        int expiring = 0;
        int expired = 0;
        int usedUp = 0;
        int voided = 0;
        int dormant = 0;

        for (PrepaidCard card : cards) {
            BigDecimal balance = money(card.getBalance());
            if (isVoid(card)) {
                voided++;
                continue;
            }
            if (balance.signum() <= 0) {
                usedUp++;
                continue;
            }
            if (isExpired(card, today)) {
                expired++;
                if (isCountUnit(card)) {
                    expiredCountValue = expiredCountValue.add(balance);
                } else {
                    expiredMoney = expiredMoney.add(balance);
                }
                continue;
            }

            active++;
            if (isCountUnit(card)) {
                countBalance = countBalance.add(balance);
            } else {
                moneyBalance = moneyBalance.add(balance);
            }
            if (isExpiringSoon(card, today)) {
                expiring++;
                if (isCountUnit(card)) {
                    expiringCountValue = expiringCountValue.add(balance);
                } else {
                    expiringMoney = expiringMoney.add(balance);
                }
            }
            Integer dormantDays = dormantDays(card, today);
            if (dormantDays != null && dormantDays >= DORMANT_DAYS) {
                dormant++;
            }
        }

        summary.setMoneyBalanceTotal(money(moneyBalance));
        summary.setCountBalanceTotal(money(countBalance));
        summary.setActiveCount(active);
        summary.setExpiringSoonCount(expiring);
        summary.setExpiringSoonMoney(money(expiringMoney));
        summary.setExpiringSoonCountValue(money(expiringCountValue));
        summary.setExpiredCount(expired);
        summary.setExpiredMoney(money(expiredMoney));
        summary.setExpiredCountValue(money(expiredCountValue));
        summary.setUsedUpCount(usedUp);
        summary.setVoidCount(voided);
        summary.setDormantCount(dormant);
        return summary;
    }

    @Override
    @Transactional
    public PrepaidCardVO create(PrepaidCardCreateDTO dto) {
        LocalDate today = LocalDate.now();
        BigDecimal faceValue = moneyOrNull(dto.getFaceValue());
        BigDecimal balance = dto.getBalance() != null
                ? money(dto.getBalance())
                : (faceValue != null ? faceValue : ZERO);

        validateUnitValue(dto.getUnit(), faceValue, balance);

        PrepaidCard card = new PrepaidCard();
        card.setName(requireText(dto.getName(), "卡券名称不能为空"));
        card.setIssuer(requireText(dto.getIssuer(), "发卡方不能为空"));
        card.setChannel(trimToNull(dto.getChannel()));
        card.setAccountHint(trimToNull(dto.getAccountHint()));
        card.setKind(dto.getKind() != null ? dto.getKind() : 1);
        card.setUnit(dto.getUnit() != null ? dto.getUnit() : 1);
        card.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "CNY");
        card.setFaceValue(faceValue);
        card.setBalance(balance);
        card.setIssueDate(dto.getIssueDate());
        card.setExpiryDate(dto.getExpiryDate());
        card.setSource(dto.getSource() != null ? dto.getSource() : 4);
        card.setStatus(1);
        card.setNote(trimToNull(dto.getNote()));

        cardMapper.insert(card);
        LocalDate occurredAt = dto.getIssueDate() != null ? dto.getIssueDate() : today;
        insertUsage(card, ACTION_INIT, balance, balance, occurredAt, "开卡");
        log.info("Created prepaid card: id={} name={} balance={}", card.getId(), card.getName(), balance);
        return toVO(card, today);
    }

    @Override
    @Transactional
    public PrepaidCardVO update(Long id, PrepaidCardUpdateDTO dto) {
        PrepaidCard card = requireCard(id);
        LocalDate today = LocalDate.now();

        String name = dto.getName() == null ? card.getName()
                : requireText(dto.getName(), "卡券名称不能为空");
        String issuer = dto.getIssuer() == null ? card.getIssuer()
                : requireText(dto.getIssuer(), "发卡方不能为空");
        Integer kind = dto.getKind() != null ? dto.getKind() : card.getKind();
        Integer unit = dto.getUnit() != null ? dto.getUnit() : card.getUnit();
        String currency = dto.getCurrency() != null ? dto.getCurrency() : card.getCurrency();
        Integer source = dto.getSource() != null ? dto.getSource() : card.getSource();
        BigDecimal faceValue = moneyOrNull(dto.getFaceValue());
        BigDecimal balance = dto.getBalance() != null ? money(dto.getBalance()) : money(card.getBalance());

        validateUnitValue(unit, faceValue, balance);

        cardMapper.update(null, new UpdateWrapper<PrepaidCard>()
                .eq("id", id)
                .set("name", name)
                .set("issuer", issuer)
                .set("channel", trimToNull(dto.getChannel()))
                .set("account_hint", trimToNull(dto.getAccountHint()))
                .set("kind", kind)
                .set("unit", unit)
                .set("currency", currency)
                .set("face_value", faceValue)
                .set("balance", balance)
                .set("issue_date", dto.getIssueDate())
                .set("expiry_date", dto.getExpiryDate())
                .set("source", source)
                .set("note", trimToNull(dto.getNote())));

        BigDecimal oldBalance = money(card.getBalance());
        if (oldBalance.compareTo(balance) != 0) {
            BigDecimal delta = balance.subtract(oldBalance);
            insertUsage(card, ACTION_ADJUST, delta, balance, today, "手工调整");
        }

        card.setName(name);
        card.setIssuer(issuer);
        card.setChannel(trimToNull(dto.getChannel()));
        card.setAccountHint(trimToNull(dto.getAccountHint()));
        card.setKind(kind);
        card.setUnit(unit);
        card.setCurrency(currency);
        card.setFaceValue(faceValue);
        card.setBalance(balance);
        card.setIssueDate(dto.getIssueDate());
        card.setExpiryDate(dto.getExpiryDate());
        card.setSource(source);
        card.setNote(trimToNull(dto.getNote()));
        log.info("Updated prepaid card: id={} name={} balance={}", id, name, balance);
        return toVO(card, today);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        PrepaidCard card = requireCard(id);
        usageMapper.delete(new LambdaQueryWrapper<PrepaidCardUsage>()
                .eq(PrepaidCardUsage::getCardId, card.getId()));
        cardMapper.deleteById(card.getId());
        log.info("Deleted prepaid card: id={} name={}", id, card.getName());
    }

    @Override
    @Transactional
    public PrepaidCardVO use(Long id, PrepaidCardAdjustDTO dto) {
        PrepaidCard card = requireCard(id);
        LocalDate occurredAt = dto.getOccurredAt() != null ? dto.getOccurredAt() : LocalDate.now();
        BigDecimal value = money(dto.getValue());

        ensureActive(card, occurredAt);
        validateUnitValue(card.getUnit(), value);

        BigDecimal balance = money(card.getBalance());
        if (value.compareTo(balance) > 0) {
            throw new BizException(ErrorCode.PREPAID_CARD_BALANCE_NOT_ENOUGH);
        }
        BigDecimal after = balance.subtract(value);
        card.setBalance(after);
        if (card.getLastUsedAt() == null || occurredAt.isAfter(card.getLastUsedAt())) {
            card.setLastUsedAt(occurredAt);
        }
        cardMapper.updateById(card);
        insertUsage(card, ACTION_USE, value.negate(), after, occurredAt, trimToNull(dto.getNote()));
        log.info("Used prepaid card: id={} name={} value={} balance={}",
                id, card.getName(), value, after);
        return toVO(card, LocalDate.now());
    }

    @Override
    @Transactional
    public PrepaidCardVO recharge(Long id, PrepaidCardAdjustDTO dto) {
        PrepaidCard card = requireCard(id);
        LocalDate occurredAt = dto.getOccurredAt() != null ? dto.getOccurredAt() : LocalDate.now();
        BigDecimal value = money(dto.getValue());

        ensureActive(card, occurredAt);
        validateUnitValue(card.getUnit(), value);

        BigDecimal after = money(card.getBalance()).add(value);
        card.setBalance(after);
        cardMapper.updateById(card);
        insertUsage(card, ACTION_RECHARGE, value, after, occurredAt, trimToNull(dto.getNote()));
        log.info("Recharged prepaid card: id={} name={} value={} balance={}",
                id, card.getName(), value, after);
        return toVO(card, LocalDate.now());
    }

    @Override
    @Transactional
    public void voidCard(Long id) {
        changeStatus(id, 2, ACTION_VOID, "作废");
    }

    @Override
    @Transactional
    public void restoreCard(Long id) {
        changeStatus(id, 1, ACTION_RESTORE, "恢复");
    }

    @Override
    public List<PrepaidCardUsageVO> usages(Long id) {
        requireCard(id);
        List<PrepaidCardUsage> rows = usageMapper.selectList(
                new LambdaQueryWrapper<PrepaidCardUsage>()
                        .eq(PrepaidCardUsage::getCardId, id)
                        .orderByDesc(PrepaidCardUsage::getOccurredAt)
                        .orderByDesc(PrepaidCardUsage::getId));
        List<PrepaidCardUsageVO> result = new ArrayList<>(rows.size());
        for (PrepaidCardUsage row : rows) {
            PrepaidCardUsageVO vo = new PrepaidCardUsageVO();
            vo.setId(row.getId());
            vo.setAction(row.getAction());
            vo.setActionName(actionName(row.getAction()));
            vo.setDelta(money(row.getDelta()));
            vo.setBalanceAfter(money(row.getBalanceAfter()));
            vo.setOccurredAt(row.getOccurredAt());
            vo.setNote(row.getNote());
            result.add(vo);
        }
        return result;
    }

    private void changeStatus(Long id, int status, int action, String note) {
        PrepaidCard card = requireCard(id);
        if (Integer.valueOf(status).equals(card.getStatus())) {
            return;
        }
        cardMapper.update(null, new UpdateWrapper<PrepaidCard>()
                .eq("id", id)
                .set("status", status));
        card.setStatus(status);
        insertUsage(card, action, ZERO, money(card.getBalance()), LocalDate.now(), note);
        log.info("Changed prepaid card status: id={} status={} action={}", id, status, action);
    }

    private PrepaidCard requireCard(Long id) {
        PrepaidCard card = cardMapper.selectById(id);
        if (card == null) {
            throw new BizException(ErrorCode.PREPAID_CARD_NOT_FOUND);
        }
        return card;
    }

    private void ensureActive(PrepaidCard card, LocalDate occurredAt) {
        if (isVoid(card)) {
            throw new BizException(ErrorCode.PREPAID_CARD_VOIDED);
        }
        if (isExpired(card, occurredAt)) {
            throw new BizException(ErrorCode.PREPAID_CARD_EXPIRED);
        }
    }

    private void insertUsage(PrepaidCard card, int action, BigDecimal delta,
                             BigDecimal balanceAfter, LocalDate occurredAt, String note) {
        PrepaidCardUsage usage = new PrepaidCardUsage();
        usage.setCardId(card.getId());
        usage.setAction(action);
        usage.setDelta(money(delta));
        usage.setBalanceAfter(money(balanceAfter));
        usage.setOccurredAt(occurredAt != null ? occurredAt : LocalDate.now());
        usage.setNote(note);
        usageMapper.insert(usage);
    }

    private PrepaidCardVO toVO(PrepaidCard card, LocalDate today) {
        PrepaidCardVO vo = new PrepaidCardVO();
        vo.setId(card.getId());
        vo.setName(card.getName());
        vo.setIssuer(card.getIssuer());
        vo.setChannel(card.getChannel());
        vo.setAccountHint(card.getAccountHint());
        vo.setKind(card.getKind());
        vo.setKindName(kindName(card.getKind()));
        vo.setUnit(card.getUnit());
        vo.setUnitName(isCountUnit(card) ? "次数" : "金额");
        vo.setCurrency(card.getCurrency());
        vo.setFaceValue(moneyOrNull(card.getFaceValue()));
        vo.setBalance(money(card.getBalance()));
        vo.setIssueDate(card.getIssueDate());
        vo.setExpiryDate(card.getExpiryDate());
        vo.setSource(card.getSource());
        vo.setSourceName(sourceName(card.getSource()));
        vo.setStatus(card.getStatus());
        vo.setNote(card.getNote());
        vo.setLastUsedAt(card.getLastUsedAt());
        vo.setCreatedAt(card.getCreatedAt());

        String state;
        if (isVoid(card)) {
            state = STATE_VOID;
        } else if (money(card.getBalance()).signum() <= 0) {
            state = STATE_USED_UP;
        } else if (isExpired(card, today)) {
            state = STATE_EXPIRED;
        } else {
            state = STATE_ACTIVE;
        }
        Integer days = card.getExpiryDate() == null ? null
                : (int) ChronoUnit.DAYS.between(today, card.getExpiryDate());
        boolean expiringSoon = STATE_ACTIVE.equals(state) && days != null
                && days >= 0 && days <= EXPIRING_DAYS;
        vo.setState(state);
        vo.setStateName(stateName(state, expiringSoon));
        vo.setDaysToExpiry(days);
        vo.setExpiringSoon(expiringSoon);
        vo.setDormantDays(STATE_ACTIVE.equals(state) && money(card.getBalance()).signum() > 0
                ? dormantDays(card, today) : null);
        return vo;
    }

    private boolean matchesState(PrepaidCardVO vo, String state) {
        if (state.isEmpty() || "ALL".equals(state)) {
            return true;
        }
        return switch (state) {
            case STATE_ACTIVE -> STATE_ACTIVE.equals(vo.getState());
            case "HAS_BALANCE" -> Integer.valueOf(1).equals(vo.getStatus())
                    && vo.getBalance() != null && vo.getBalance().signum() > 0;
            case "EXPIRING" -> Boolean.TRUE.equals(vo.getExpiringSoon());
            case STATE_EXPIRED, STATE_USED_UP, STATE_VOID -> state.equals(vo.getState());
            default -> true;
        };
    }

    private boolean matchesKeyword(PrepaidCardVO vo, String keyword) {
        return contains(vo.getName(), keyword)
                || contains(vo.getIssuer(), keyword)
                || contains(vo.getChannel(), keyword)
                || contains(vo.getAccountHint(), keyword)
                || contains(vo.getNote(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Comparator<PrepaidCardVO> cardComparator() {
        return Comparator
                .comparingInt((PrepaidCardVO vo) -> stateRank(vo.getState()))
                .thenComparingInt(vo -> Boolean.TRUE.equals(vo.getExpiringSoon()) ? 0 : 1)
                .thenComparingInt(vo -> vo.getDaysToExpiry() == null
                        ? Integer.MAX_VALUE : vo.getDaysToExpiry())
                .thenComparing(PrepaidCardVO::getBalance,
                        Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int stateRank(String state) {
        return switch (state) {
            case STATE_ACTIVE -> 0;
            case STATE_USED_UP -> 1;
            case STATE_EXPIRED -> 2;
            default -> 3;
        };
    }

    private Integer dormantDays(PrepaidCard card, LocalDate today) {
        LocalDate last = card.getLastUsedAt();
        if (last == null && card.getCreatedAt() != null) {
            last = card.getCreatedAt().toLocalDate();
        }
        if (last == null) {
            return null;
        }
        return (int) Math.max(0, ChronoUnit.DAYS.between(last, today));
    }

    private boolean isVoid(PrepaidCard card) {
        return Integer.valueOf(2).equals(card.getStatus());
    }

    private boolean isCountUnit(PrepaidCard card) {
        return Integer.valueOf(2).equals(card.getUnit());
    }

    private boolean isExpired(PrepaidCard card, LocalDate today) {
        return card.getExpiryDate() != null && card.getExpiryDate().isBefore(today);
    }

    private boolean isExpiringSoon(PrepaidCard card, LocalDate today) {
        if (card.getExpiryDate() == null) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(today, card.getExpiryDate());
        return days >= 0 && days <= EXPIRING_DAYS;
    }

    private void validateUnitValue(Integer unit, BigDecimal... values) {
        if (!Integer.valueOf(2).equals(unit)) {
            return;
        }
        for (BigDecimal value : values) {
            if (value != null && value.stripTrailingZeros().scale() > 0) {
                throw new BizException(ErrorCode.PREPAID_CARD_VALUE_INVALID,
                        "次数卡只能记录整数次数");
            }
        }
    }

    private String requireText(String value, String message) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, message);
        }
        return text;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal moneyOrNull(BigDecimal value) {
        return value == null ? null : money(value);
    }

    private String kindName(Integer kind) {
        if (kind == null) {
            return "其他";
        }
        return switch (kind) {
            case 1 -> "储值卡";
            case 2 -> "礼品卡";
            case 3 -> "兑换券";
            case 4 -> "次数卡";
            default -> "其他";
        };
    }

    private String sourceName(Integer source) {
        if (source == null) {
            return "其他";
        }
        return switch (source) {
            case 1 -> "单位发放";
            case 2 -> "自购";
            case 3 -> "赠送";
            default -> "其他";
        };
    }

    private String stateName(String state, boolean expiringSoon) {
        return switch (state) {
            case STATE_ACTIVE -> expiringSoon ? "即将到期" : "有效";
            case STATE_USED_UP -> "已用完";
            case STATE_EXPIRED -> "已过期";
            default -> "已作废";
        };
    }

    private String actionName(Integer action) {
        if (action == null) {
            return "变动";
        }
        return switch (action) {
            case ACTION_INIT -> "开卡";
            case ACTION_USE -> "使用";
            case ACTION_RECHARGE -> "充值";
            case ACTION_ADJUST -> "调整";
            case ACTION_VOID -> "作废";
            case ACTION_RESTORE -> "恢复";
            default -> "变动";
        };
    }
}