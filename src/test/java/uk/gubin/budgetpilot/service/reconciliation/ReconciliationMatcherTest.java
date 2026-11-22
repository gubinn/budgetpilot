package uk.gubin.budgetpilot.service.reconciliation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gubin.budgetpilot.entity.ReconciliationItem;
import uk.gubin.budgetpilot.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationMatcherTest {

    private final ReconciliationMatcher matcher = new ReconciliationMatcher(new ObjectMapper());

    @Test
    void matchesExactAmountAndTime() {
        ReconciliationItem item = item(1L, "2026-10-31 17:50:29", "7.25",
                ReconcileConstants.DIRECTION_EXPENSE, "交易成功", "李雅新");
        Transaction tx = tx(10L, 1, "7.25", "2026-10-31", "17:50:30", "支付宝-巴黎贝甜");

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(item), List.of(tx), BillChannel.ALIPAY);

        assertThat(result).singleElement().satisfies(m -> {
            assertThat(m.status()).isEqualTo(ReconcileConstants.STATUS_MATCHED);
            assertThat(m.matchType()).isEqualTo(ReconcileConstants.TYPE_AUTO);
            assertThat(m.transactionId()).isEqualTo(10L);
        });
    }

    @Test
    void ignoresTransactionsOutsideTimeWindow() {
        ReconciliationItem item = item(1L, "2026-10-31 17:50:29", "7.25",
                ReconcileConstants.DIRECTION_EXPENSE, "交易成功", "李雅新");
        Transaction tx = tx(10L, 1, "7.25", "2026-10-31", "18:30:00", "支付宝-巴黎贝甜");

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(item), List.of(tx), BillChannel.ALIPAY);

        assertThat(result).singleElement()
                .extracting(ReconciliationMatcher.ItemMatch::status)
                .isEqualTo(ReconcileConstants.STATUS_UNMATCHED);
    }

    @Test
    void marksAmbiguousWhenTwoCandidatesHaveSameAmountAndTime() {
        ReconciliationItem item = item(1L, "2026-10-31 17:50:29", "7.25",
                ReconcileConstants.DIRECTION_EXPENSE, "交易成功", "李雅新");
        Transaction first = tx(10L, 1, "7.25", "2026-10-31", "17:50:30", "支付宝-A");
        Transaction second = tx(11L, 1, "7.25", "2026-10-31", "17:50:31", "支付宝-B");

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(item), List.of(first, second), BillChannel.ALIPAY);

        assertThat(result).singleElement()
                .extracting(ReconciliationMatcher.ItemMatch::status)
                .isEqualTo(ReconcileConstants.STATUS_AMBIGUOUS);
    }

    @Test
    void aggregatesSplitPaymentsIntoOneTransaction() {
        ReconciliationItem first = item(1L, "2026-10-28 19:16:14", "61.00",
                ReconcileConstants.DIRECTION_EXPENSE, "交易成功", "李雅新");
        ReconciliationItem second = item(2L, "2026-10-28 19:16:14", "0.10",
                ReconcileConstants.DIRECTION_EXPENSE, "交易成功", "李雅新");
        Transaction tx = tx(10L, 1, "61.10", "2026-10-28", "19:16:13", "支付宝-饿了么");

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(first, second), List.of(tx), BillChannel.ALIPAY);

        assertThat(result).hasSize(2).allSatisfy(m -> {
            assertThat(m.status()).isEqualTo(ReconcileConstants.STATUS_MATCHED);
            assertThat(m.matchType()).isEqualTo(ReconcileConstants.TYPE_AGGREGATE);
            assertThat(m.transactionId()).isEqualTo(10L);
        });
    }

    @Test
    void matchesRefundToTypeFourTransaction() {
        ReconciliationItem item = item(1L, "2026-10-19 17:59:11", "0.31",
                ReconcileConstants.DIRECTION_NEUTRAL, "退款成功", "上海盒马");
        Transaction tx = tx(10L, 4, "0.31", "2026-10-19", "17:59:13", "支付宝-上海盒马");

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(item), List.of(tx), BillChannel.ALIPAY);

        assertThat(result).singleElement().satisfies(m -> {
            assertThat(m.status()).isEqualTo(ReconcileConstants.STATUS_MATCHED);
            assertThat(m.transactionId()).isEqualTo(10L);
        });
    }

    @Test
    void matchesDateOnlyCreditCardItemByAmountAndDate() {
        ReconciliationItem item = datedItem(1L, "2026-09-15", "6.00",
                ReconcileConstants.DIRECTION_EXPENSE, "消费",
                "支付宝-示例商户乙");
        Transaction tx = tx(10L, 1, "6.00", "2026-09-15", "08:51:04",
                "08:51:04 CNY 6.00 尾号1234 消费 支付宝-示例商户乙");

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(item), List.of(tx), BillChannel.CMB_CREDIT);

        assertThat(result).singleElement().satisfies(m -> {
            assertThat(m.status()).isEqualTo(ReconcileConstants.STATUS_MATCHED);
            assertThat(m.transactionId()).isEqualTo(10L);
        });
    }

    @Test
    void matchesDuplicateDateOnlyItemsToDistinctTransactions() {
        ReconciliationItem first = datedItem(1L, "2026-10-03", "3.34",
                ReconcileConstants.DIRECTION_EXPENSE, "消费", "支付宝-示例商户甲");
        ReconciliationItem second = datedItem(2L, "2026-10-03", "3.34",
                ReconcileConstants.DIRECTION_EXPENSE, "消费", "支付宝-示例商户甲");
        Transaction morning = tx(10L, 1, "3.34", "2026-10-03", "13:26:25",
                "13:26:25 CNY 3.34 尾号1234 消费 支付宝-示例商户甲");
        Transaction evening = tx(11L, 1, "3.34", "2026-10-03", "17:13:57",
                "17:13:57 CNY 3.34 尾号1234 消费 支付宝-示例商户甲");

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(first, second), List.of(morning, evening), BillChannel.CMB_CREDIT);

        assertThat(result).hasSize(2).allSatisfy(m ->
                assertThat(m.status()).isEqualTo(ReconcileConstants.STATUS_MATCHED));
        assertThat(result).extracting(ReconciliationMatcher.ItemMatch::transactionId)
                .containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    void prefersMerchantMatchWhenDateOnlyAmountIsAmbiguous() {
        ReconciliationItem item = datedItem(1L, "2026-10-03", "3.34",
                ReconcileConstants.DIRECTION_EXPENSE, "消费", "支付宝-示例商户丙");
        Transaction other = tx(10L, 1, "3.34", "2026-10-03", "13:26:25",
                "13:26:25 CNY 3.34 尾号1234 消费 支付宝-示例商户甲");
        Transaction sameMerchant = tx(11L, 1, "3.34", "2026-10-03", "18:54:28",
                "18:54:28 CNY 3.34 尾号1234 消费 支付宝-示例商户丙");

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(item), List.of(other, sameMerchant), BillChannel.CMB_CREDIT);

        assertThat(result).singleElement().satisfies(m -> {
            assertThat(m.status()).isEqualTo(ReconcileConstants.STATUS_MATCHED);
            assertThat(m.transactionId()).isEqualTo(11L);
        });
    }

    @Test
    void allowsCrossChannelReconciliation() {
        ReconciliationItem item = datedItem(1L, "2026-09-15", "6.00",
                ReconcileConstants.DIRECTION_EXPENSE, "消费", "支付宝-示例商户乙");
        Transaction tx = tx(10L, 1, "6.00", "2026-09-15", "08:51:04",
                "08:51:04 CNY 6.00 尾号1234 消费 支付宝-示例商户乙");
        tx.setReconciled(true);
        tx.setReconcileChannel(BillChannel.ALIPAY.name());

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(item), List.of(tx), BillChannel.CMB_CREDIT);

        assertThat(result).singleElement().satisfies(m -> {
            assertThat(m.status()).isEqualTo(ReconcileConstants.STATUS_MATCHED);
            assertThat(m.transactionId()).isEqualTo(10L);
        });
    }

    @Test
    void skipsTransactionsReconciledBySameChannel() {
        ReconciliationItem item = item(1L, "2026-10-31 17:50:29", "7.25",
                ReconcileConstants.DIRECTION_EXPENSE, "交易成功", "李雅新");
        Transaction tx = tx(10L, 1, "7.25", "2026-10-31", "17:50:30", "支付宝-巴黎贝甜");
        tx.setReconciled(true);
        tx.setReconcileChannel(BillChannel.ALIPAY.name());

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(item), List.of(tx), BillChannel.ALIPAY);

        assertThat(result).singleElement()
                .extracting(ReconciliationMatcher.ItemMatch::status)
                .isEqualTo(ReconcileConstants.STATUS_UNMATCHED);
    }

    @Test
    void matchesRefundBillToExpenseTransactionRecordedAsRefund() {
        ReconciliationItem refund = datedItem(1L, "2026-05-21", "520.00",
                ReconcileConstants.DIRECTION_NEUTRAL, "快捷退款", "微信红包");
        Transaction tx = tx(10L, 1, "520.00", "2026-05-21", "15:44:00",
                "您账户1234于05月21日15:44在财付通-微信支付-微信红包退款520.00元，余额2509.80");

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(refund), List.of(tx), BillChannel.CMB_DEBIT);

        assertThat(result).singleElement().satisfies(m -> {
            assertThat(m.status()).isEqualTo(ReconcileConstants.STATUS_MATCHED);
            assertThat(m.transactionId()).isEqualTo(10L);
        });
    }

    @Test
    void matchesTransferStatementToTypeThreeTransaction() {
        ReconciliationItem transfer = datedItem(1L, "2026-02-28", "3000.00",
                ReconcileConstants.DIRECTION_INCOME, "行内转账转入", "示例用户");
        Transaction tx = tx(10L, 3, "3000.00", "2026-02-28", "12:00:00",
                "您账户1234于02月28日收到本行转入人民币3000.00，余额5000.00，付方示例用户（1234），备注：转账");
        tx.setTransactionTime(null);

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(transfer), List.of(tx), BillChannel.CMB_DEBIT);

        assertThat(result).singleElement().satisfies(m -> {
            assertThat(m.status()).isEqualTo(ReconcileConstants.STATUS_MATCHED);
            assertThat(m.transactionId()).isEqualTo(10L);
        });
    }

    @Test
    void matchesSavingsCardExpenseByDateAndMerchant() {
        ReconciliationItem item = datedItem(1L, "2026-10-01", "174.47",
                ReconcileConstants.DIRECTION_EXPENSE, "快捷支付", "示例超市");
        Transaction tx = tx(10L, 1, "174.47", "2026-10-01", "12:08:31",
                "您账户1234于10月01日12:08在支付宝-示例超市快捷支付174.47元，余额2529.31");
        tx.setExtFields("{\"channel\":\"支付宝\"}");

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(item), List.of(tx), BillChannel.CMB_DEBIT);

        assertThat(result).singleElement().satisfies(m -> {
            assertThat(m.status()).isEqualTo(ReconcileConstants.STATUS_MATCHED);
            assertThat(m.transactionId()).isEqualTo(10L);
        });
    }

    @Test
    void allowsSameChannelTransferForOtherAccountSide() {
        ReconciliationItem item = datedItem(1L, "2026-02-28", "3000.00",
                ReconcileConstants.DIRECTION_EXPENSE, "转账汇款", "示例用户");
        Transaction tx = tx(10L, 3, "3000.00", "2026-02-28", "12:00:00", "转账汇款 示例用户");
        tx.setReconciled(true);
        tx.setReconcileChannel(BillChannel.CMB_DEBIT.name());

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(item), List.of(tx), BillChannel.CMB_DEBIT);

        assertThat(result).singleElement().satisfies(m -> {
            assertThat(m.status()).isEqualTo(ReconcileConstants.STATUS_MATCHED);
            assertThat(m.transactionId()).isEqualTo(10L);
        });
    }

    @Test
    void matchesWalletTransferToDateOnlyCreatedTransaction() {
        ReconciliationItem item = item(1L, "2026-01-17 17:37:09", "2.34",
                ReconcileConstants.DIRECTION_NEUTRAL, "零钱充值", "招商银行(0611)");
        item.setChannel(BillChannel.WECHAT.name());
        Transaction tx = tx(10L, 3, "2.34", "2026-01-17", "12:00:00", "快捷支付 示例用户");
        tx.setExtFields("{\"channel\":\"微信支付\"}");
        tx.setReconciled(true);
        tx.setReconcileChannel(BillChannel.CMB_DEBIT.name());

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(item), List.of(tx), BillChannel.WECHAT);

        assertThat(result).singleElement().satisfies(m -> {
            assertThat(m.status()).isEqualTo(ReconcileConstants.STATUS_MATCHED);
            assertThat(m.transactionId()).isEqualTo(10L);
        });
    }

    @Test
    void matchesWechatPaymentToDateOnlyBackfilledBankTransaction() {
        ReconciliationItem item = item(1L, "2026-02-17 11:18:52", "50.00",
                ReconcileConstants.DIRECTION_EXPENSE, "扫二维码付款", "示例收款人");
        item.setChannel(BillChannel.WECHAT.name());
        Transaction tx = tx(10L, 1, "50.00", "2026-02-17", "12:00:00", "快捷支付 示例收款人");
        tx.setExternalId("recon:CMB_DEBIT:example");
        tx.setTags("[\"对账补录\"]");
        tx.setReconciled(true);
        tx.setReconcileChannel(BillChannel.CMB_DEBIT.name());

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(item), List.of(tx), BillChannel.WECHAT);

        assertThat(result).singleElement().satisfies(m -> {
            assertThat(m.status()).isEqualTo(ReconcileConstants.STATUS_MATCHED);
            assertThat(m.transactionId()).isEqualTo(10L);
        });
    }

    @Test
    void matchesWalletWithdrawalNetOfServiceFee() {
        ReconciliationItem item = item(1L, "2026-01-17 17:35:59", "1.46",
                ReconcileConstants.DIRECTION_NEUTRAL, "零钱提现", "招商银行(0611)");
        item.setChannel(BillChannel.WECHAT.name());
        item.setFee(new BigDecimal("0.01"));
        Transaction tx = tx(10L, 2, "1.45", "2026-01-17", "12:00:00", "银联代付 示例用户");
        tx.setExternalId("recon:CMB_DEBIT:example");
        tx.setTags("[\"对账补录\"]");
        tx.setReconciled(true);
        tx.setReconcileChannel(BillChannel.CMB_DEBIT.name());

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(item), List.of(tx), BillChannel.WECHAT);

        assertThat(result).singleElement().satisfies(m -> {
            assertThat(m.status()).isEqualTo(ReconcileConstants.STATUS_MATCHED);
            assertThat(m.transactionId()).isEqualTo(10L);
        });
    }

    @Test
    void ignoresZeroAmountAuthorizationRows() {
        ReconciliationItem item = item(1L, "2026-10-23 11:55:45", "0.00",
                ReconcileConstants.DIRECTION_EXPENSE, "支付成功", "拼多多");

        List<ReconciliationMatcher.ItemMatch> result = matcher.match(
                List.of(item), List.of(), BillChannel.ALIPAY);

        assertThat(result).singleElement()
                .extracting(ReconciliationMatcher.ItemMatch::status)
                .isEqualTo(ReconcileConstants.STATUS_IGNORED);
    }

    private ReconciliationItem item(Long id, String time, String amount, String direction,
                                    String status, String counterparty) {
        ReconciliationItem item = new ReconciliationItem();
        item.setId(id);
        item.setChannel(BillChannel.ALIPAY.name());
        item.setDirection(direction);
        item.setAmount(new BigDecimal(amount));
        item.setTradeStatus(status);
        item.setCounterparty(counterparty);
        LocalDateTime dateTime = LocalDateTime.parse(time.replace(' ', 'T'));
        item.setPaidTime(dateTime);
        item.setTradeDate(dateTime.toLocalDate());
        item.setMatchStatus(ReconcileConstants.STATUS_UNMATCHED);
        return item;
    }

    private ReconciliationItem datedItem(Long id, String date, String amount, String direction,
                                         String status, String counterparty) {
        ReconciliationItem item = new ReconciliationItem();
        item.setId(id);
        item.setChannel(BillChannel.CMB_CREDIT.name());
        item.setDirection(direction);
        item.setAmount(new BigDecimal(amount));
        item.setTradeStatus(status);
        item.setCounterparty(counterparty);
        item.setTradeDate(LocalDate.parse(date));
        item.setMatchStatus(ReconcileConstants.STATUS_UNMATCHED);
        return item;
    }

    private Transaction tx(Long id, int type, String amount, String date, String time, String note) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setType(type);
        tx.setAmount(new BigDecimal(amount));
        tx.setAmountBase(new BigDecimal(amount));
        tx.setCurrency("CNY");
        tx.setTransactionDate(LocalDate.parse(date));
        tx.setTransactionTime(LocalTime.parse(time));
        tx.setNote(note);
        tx.setExtFields("{\"channel\":\"支付宝\"}");
        tx.setReconciled(false);
        return tx;
    }
}