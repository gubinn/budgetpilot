package uk.gubin.budgetpilot.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.dto.PrepaidCardAdjustDTO;
import uk.gubin.budgetpilot.dto.PrepaidCardCreateDTO;
import uk.gubin.budgetpilot.dto.PrepaidCardUpdateDTO;
import uk.gubin.budgetpilot.entity.PrepaidCard;
import uk.gubin.budgetpilot.entity.PrepaidCardUsage;
import uk.gubin.budgetpilot.mapper.PrepaidCardMapper;
import uk.gubin.budgetpilot.mapper.PrepaidCardUsageMapper;
import uk.gubin.budgetpilot.vo.PrepaidCardSummaryVO;
import uk.gubin.budgetpilot.vo.PrepaidCardVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PrepaidCardServiceTest {

    @Mock private PrepaidCardMapper cardMapper;
    @Mock private PrepaidCardUsageMapper usageMapper;
    @InjectMocks private PrepaidCardServiceImpl service;

    private static final LocalDate TODAY = LocalDate.now();

    @Test
    @DisplayName("创建时未填余额，默认按面值入账，并记一条开卡流水")
    void createDefaultsBalanceToFaceValue() {
        when(cardMapper.insert(any(PrepaidCard.class))).thenAnswer(invocation -> {
            PrepaidCard card = invocation.getArgument(0);
            card.setId(9L);
            return 1;
        });

        PrepaidCardCreateDTO dto = new PrepaidCardCreateDTO();
        dto.setName("星巴克星礼卡");
        dto.setIssuer("星巴克");
        dto.setFaceValue(new BigDecimal("500"));
        dto.setUnit(1);

        PrepaidCardVO vo = service.create(dto);

        assertThat(vo.getId()).isEqualTo(9L);
        assertThat(vo.getBalance()).isEqualByComparingTo("500.00");
        assertThat(vo.getState()).isEqualTo("ACTIVE");

        ArgumentCaptor<PrepaidCardUsage> captor = ArgumentCaptor.forClass(PrepaidCardUsage.class);
        verify(usageMapper).insert(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(1);
        assertThat(captor.getValue().getDelta()).isEqualByComparingTo("500.00");
        assertThat(captor.getValue().getBalanceAfter()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("状态由余额、有效期和作废标记实时推导")
    void derivesStateFromBalanceAndExpiry() {
        LocalDate today = LocalDate.now();
        PrepaidCard expiring = card(1L, "电影通兑券", "万达影城", 1, new BigDecimal("6"),
                today.plusDays(10), 1);
        PrepaidCard expired = card(2L, "京东E卡", "京东", 1, new BigDecimal("200"),
                today.minusDays(2), 1);
        PrepaidCard usedUp = card(3L, "盒马礼品卡", "盒马", 1, BigDecimal.ZERO,
                today.plusDays(100), 1);
        PrepaidCard voided = card(4L, "旧购物卡", "某商场", 1, new BigDecimal("50"),
                null, 2);

        when(cardMapper.selectList(any())).thenReturn(List.of(expiring, expired, usedUp, voided));

        List<PrepaidCardVO> cards = service.list("ALL", null, null, null, null);

        assertThat(find(cards, 1L).getState()).isEqualTo("ACTIVE");
        assertThat(find(cards, 1L).getExpiringSoon()).isTrue();
        assertThat(find(cards, 1L).getDaysToExpiry()).isEqualTo(10);
        assertThat(find(cards, 2L).getState()).isEqualTo("EXPIRED");
        assertThat(find(cards, 2L).getDaysToExpiry()).isEqualTo(-2);
        assertThat(find(cards, 3L).getState()).isEqualTo("USED_UP");
        assertThat(find(cards, 4L).getState()).isEqualTo("VOID");
    }

    @Test
    @DisplayName("使用卡券扣减余额并记录流水，最近使用时间同步更新")
    void useDeductsBalanceAndLogs() {
        PrepaidCard card = card(1L, "星巴克星礼卡", "星巴克", 1, new BigDecimal("100"),
                TODAY.plusDays(30), 1);
        when(cardMapper.selectById(1L)).thenReturn(card);

        PrepaidCardAdjustDTO dto = adjust("30");
        PrepaidCardVO vo = service.use(1L, dto);

        assertThat(vo.getBalance()).isEqualByComparingTo("70.00");
        assertThat(card.getLastUsedAt()).isEqualTo(TODAY);
        verify(cardMapper).updateById(card);

        ArgumentCaptor<PrepaidCardUsage> captor = ArgumentCaptor.forClass(PrepaidCardUsage.class);
        verify(usageMapper).insert(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(2);
        assertThat(captor.getValue().getDelta()).isEqualByComparingTo("-30.00");
        assertThat(captor.getValue().getBalanceAfter()).isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("使用金额超过余额时拒绝")
    void useRejectsInsufficientBalance() {
        PrepaidCard card = card(1L, "购物卡", "某商场", 1, new BigDecimal("20"),
                null, 1);
        when(cardMapper.selectById(1L)).thenReturn(card);

        assertThatThrownBy(() -> service.use(1L, adjust("50")))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode())
                                .isEqualTo(ErrorCode.PREPAID_CARD_BALANCE_NOT_ENOUGH));
    }

    @Test
    @DisplayName("已过有效期的卡券不能使用，先改有效期")
    void useRejectsExpiredCard() {
        PrepaidCard card = card(1L, "过期券", "某影院", 1, new BigDecimal("2"),
                TODAY.minusDays(1), 1);
        when(cardMapper.selectById(1L)).thenReturn(card);

        assertThatThrownBy(() -> service.use(1L, adjust("1")))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode())
                                .isEqualTo(ErrorCode.PREPAID_CARD_EXPIRED));
    }

    @Test
    @DisplayName("次数卡只接受整数次数")
    void countUnitRejectsFractionalValue() {
        PrepaidCard card = card(1L, "洗车次卡", "洗车店", 2, new BigDecimal("5"),
                null, 1);
        when(cardMapper.selectById(1L)).thenReturn(card);

        assertThatThrownBy(() -> service.use(1L, adjust("1.5")))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode())
                                .isEqualTo(ErrorCode.PREPAID_CARD_VALUE_INVALID));
    }

    @Test
    @DisplayName("充值增加余额并记录正向流水")
    void rechargeAddsBalance() {
        PrepaidCard card = card(1L, "加油卡", "中石化", 1, new BigDecimal("100"),
                null, 1);
        when(cardMapper.selectById(1L)).thenReturn(card);

        PrepaidCardVO vo = service.recharge(1L, adjust("50"));

        assertThat(vo.getBalance()).isEqualByComparingTo("150.00");
        ArgumentCaptor<PrepaidCardUsage> captor = ArgumentCaptor.forClass(PrepaidCardUsage.class);
        verify(usageMapper).insert(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(3);
        assertThat(captor.getValue().getDelta()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("汇总把金额卡和次数卡分开统计，并识别即将到期/已过期/已用完")
    void summarySplitsMoneyAndCounts() {
        LocalDate today = LocalDate.now();
        when(cardMapper.selectList(any())).thenReturn(List.of(
                card(1L, "星礼卡", "星巴克", 1, new BigDecimal("100"), today.plusDays(10), 1),
                card(2L, "购物卡", "某商场", 1, new BigDecimal("50"), null, 1),
                card(3L, "电影票", "万达影城", 2, new BigDecimal("3"), today.plusDays(5), 1),
                card(4L, "过期卡", "某超市", 1, new BigDecimal("20"), today.minusDays(3), 1),
                card(5L, "用完的卡", "某咖啡", 1, BigDecimal.ZERO, null, 1)
        ));

        PrepaidCardSummaryVO summary = service.summary();

        assertThat(summary.getMoneyBalanceTotal()).isEqualByComparingTo("150.00");
        assertThat(summary.getCountBalanceTotal()).isEqualByComparingTo("3.00");
        assertThat(summary.getActiveCount()).isEqualTo(3);
        assertThat(summary.getExpiringSoonCount()).isEqualTo(2);
        assertThat(summary.getExpiringSoonMoney()).isEqualByComparingTo("100.00");
        assertThat(summary.getExpiringSoonCountValue()).isEqualByComparingTo("3.00");
        assertThat(summary.getExpiredCount()).isEqualTo(1);
        assertThat(summary.getExpiredMoney()).isEqualByComparingTo("20.00");
        assertThat(summary.getUsedUpCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("编辑时手工改余额会留一条调整流水")
    void updateBalanceLogsAdjustment() {
        PrepaidCard card = card(1L, "购物卡", "某商场", 1, new BigDecimal("100"),
                null, 1);
        when(cardMapper.selectById(1L)).thenReturn(card);

        PrepaidCardUpdateDTO dto = new PrepaidCardUpdateDTO();
        dto.setBalance(new BigDecimal("80"));

        PrepaidCardVO vo = service.update(1L, dto);

        assertThat(vo.getBalance()).isEqualByComparingTo("80.00");
        ArgumentCaptor<PrepaidCardUsage> captor = ArgumentCaptor.forClass(PrepaidCardUsage.class);
        verify(usageMapper).insert(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(4);
        assertThat(captor.getValue().getDelta()).isEqualByComparingTo("-20.00");
    }

    @Test
    @DisplayName("作废和恢复只改状态，不动余额")
    void voidAndRestoreKeepBalance() {
        PrepaidCard card = card(1L, "购物卡", "某商场", 1, new BigDecimal("100"),
                null, 1);
        when(cardMapper.selectById(1L)).thenReturn(card);

        service.voidCard(1L);
        assertThat(card.getStatus()).isEqualTo(2);

        service.restoreCard(1L);
        assertThat(card.getStatus()).isEqualTo(1);
    }

    private PrepaidCard card(Long id, String name, String issuer, int unit,
                             BigDecimal balance, LocalDate expiry, int status) {
        PrepaidCard card = new PrepaidCard();
        card.setId(id);
        card.setName(name);
        card.setIssuer(issuer);
        card.setUnit(unit);
        card.setKind(unit == 2 ? 4 : 1);
        card.setCurrency("CNY");
        card.setBalance(balance);
        card.setFaceValue(balance);
        card.setExpiryDate(expiry);
        card.setStatus(status);
        card.setSource(1);
        return card;
    }

    private PrepaidCardAdjustDTO adjust(String value) {
        PrepaidCardAdjustDTO dto = new PrepaidCardAdjustDTO();
        dto.setValue(new BigDecimal(value));
        dto.setOccurredAt(TODAY);
        return dto;
    }

    private PrepaidCardVO find(List<PrepaidCardVO> cards, Long id) {
        return cards.stream().filter(c -> id.equals(c.getId())).findFirst().orElseThrow();
    }
}