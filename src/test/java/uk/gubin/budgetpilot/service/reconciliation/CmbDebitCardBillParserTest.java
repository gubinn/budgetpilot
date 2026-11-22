package uk.gubin.budgetpilot.service.reconciliation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CmbDebitCardBillParserTest {

    private final CmbDebitCardBillParser parser = new CmbDebitCardBillParser();

    @Test
    void parsesDebitCardStatementWithWrappedCounterparties() {
        String text = """
                招商银行交易流水
                Transaction Statement of China Merchants Bank
                2026-01-01 -- 2026-11-20
                户  名：示例用户 账号：6212********1234
                Name Account No
                账户类型：ALL/全币种 开 户 行：示例支行
                Account Type Sub Branch
                申请时间：2026-11-22 17:39:22 验 证 码：ABCD
                Date Verification Code
                记账日期 货币 交易金额 联机余额 交易摘要 对手信息
                Transaction
                Date Currency Balance Transaction Type Counter Party
                Amount
                2026-02-28 CNY 3,000.00 3,000.00 行内转账转入 示例用户
                2026-03-03 CNY -5.00 2,995.00 快捷支付 示例商户甲
                2026-03-21 CNY 0.13 2,995.13 账户结息
                应付利息-应付个人活期存款利
                息(自动计提)（新)
                1/6
                记账日期 货币 交易金额 联机余额 交易摘要 对手信息
                Transaction
                Date Currency Balance Transaction Type Counter Party
                Amount
                2026-04-25 CNY -750.00 2,245.13 快捷支付
                示例长商户名称有限
                公司
                2026-05-20 CNY -520.00 1,725.13 快捷支付 微信红包
                2026-05-21 CNY 520.00 2,245.13 快捷退款 微信红包
                2026-09-15 CNY -128.59 2,116.54 银联线上有卡支付 Example Cards Limited
                2026-09-29 CNY 200,573.13 202,689.67
                银证转账(第三方存管) 示例用户
                2026-03-10 USD -2.00 9.98 快捷支付 示例境外商户
                """;

        ParsedBill bill = parser.parseText(text);

        assertThat(bill.getChannel()).isEqualTo(BillChannel.CMB_DEBIT.name());
        assertThat(bill.getBillAccount()).isEqualTo("6212********1234");
        assertThat(bill.getSummary().get("accountName")).isEqualTo("示例用户");
        assertThat(bill.getPeriodStart()).hasToString("2026-01-01");
        assertThat(bill.getPeriodEnd()).hasToString("2026-11-20");
        assertThat(bill.getItems()).hasSize(8);
        assertThat(bill.getItems()).extracting(ParsedBillItem::getItemKey).doesNotHaveDuplicates();

        ParsedBillItem transfer = bill.getItems().get(0);
        assertThat(transfer.getTradeDate()).hasToString("2026-02-28");
        assertThat(transfer.getDirection()).isEqualTo(ReconcileConstants.DIRECTION_INCOME);
        assertThat(transfer.getTradeStatus()).isEqualTo("行内转账转入");
        assertThat(transfer.getCounterparty()).isEqualTo("示例用户");
        assertThat(transfer.getAmount()).isEqualByComparingTo("3000.00");
        assertThat(transfer.getPaidTime()).isNull();
        assertThat(ReconcileConstants.transactionType(
                transfer.getDirection(), transfer.getTradeStatus())).isEqualTo(3);

        ParsedBillItem expense = bill.getItems().get(1);
        assertThat(expense.getDirection()).isEqualTo(ReconcileConstants.DIRECTION_EXPENSE);
        assertThat(expense.getCounterparty()).isEqualTo("示例商户甲");
        assertThat(expense.getAmount()).isEqualByComparingTo("5.00");
        assertThat(expense.getRaw()).containsEntry("记账日期", "2026-03-03")
                .containsEntry("户名", "示例用户")
                .containsEntry("联机余额", "2,995.00");

        ParsedBillItem interest = bill.getItems().get(2);
        assertThat(interest.getDirection()).isEqualTo(ReconcileConstants.DIRECTION_INCOME);
        assertThat(interest.getCounterparty())
                .isEqualTo("应付利息-应付个人活期存款利息(自动计提)（新)");
        assertThat(interest.getTradeStatus()).isEqualTo("账户结息");
        assertThat(ReconcileConstants.transactionType(
                interest.getDirection(), interest.getTradeStatus())).isEqualTo(2);

        ParsedBillItem wrappedMerchant = bill.getItems().get(3);
        assertThat(wrappedMerchant.getCounterparty())
                .isEqualTo("示例长商户名称有限公司");
        assertThat(wrappedMerchant.getAmount()).isEqualByComparingTo("750.00");

        ParsedBillItem refund = bill.getItems().get(5);
        assertThat(refund.getDirection()).isEqualTo(ReconcileConstants.DIRECTION_NEUTRAL);
        assertThat(refund.getTradeStatus()).isEqualTo("快捷退款");
        assertThat(refund.getAmount()).isEqualByComparingTo("520.00");
        assertThat(ReconcileConstants.transactionType(
                refund.getDirection(), refund.getTradeStatus())).isEqualTo(4);

        ParsedBillItem english = bill.getItems().get(6);
        assertThat(english.getCounterparty()).isEqualTo("Example Cards Limited");

        ParsedBillItem summaryOnNextLine = bill.getItems().get(7);
        assertThat(summaryOnNextLine.getTradeStatus()).isEqualTo("银证转账(第三方存管)");
        assertThat(summaryOnNextLine.getCounterparty()).isEqualTo("示例用户");
        assertThat(summaryOnNextLine.getDirection()).isEqualTo(ReconcileConstants.DIRECTION_INCOME);
        assertThat(summaryOnNextLine.getAmount()).isEqualByComparingTo("200573.13");
        assertThat(ReconcileConstants.transactionType(
                summaryOnNextLine.getDirection(), summaryOnNextLine.getTradeStatus())).isEqualTo(3);

        assertThat(bill.getSummary().get("foreignAccountSkipped")).isEqualTo(1);
        assertThat(bill.getSummary().get("balanceMismatches")).isEqualTo(0);
        assertThat((List<?>) bill.getSummary().get("warnings"))
                .singleElement().asString().contains("非人民币账户");
        assertThat(bill.getSummary().get("expenseTotal")).isEqualTo(new java.math.BigDecimal("1403.59"));
        assertThat(bill.getSummary().get("incomeTotal")).isEqualTo(new java.math.BigDecimal("203573.26"));
        assertThat(bill.getSummary().get("refundTotal")).isEqualTo(new java.math.BigDecimal("520.00"));

        @SuppressWarnings("unchecked")
        Map<String, Object> transferSummary = (Map<String, Object>) bill.getSummary().get("transfer");
        assertThat(transferSummary.get("count")).isEqualTo(2);
        assertThat(transferSummary.get("amount")).isEqualTo(new java.math.BigDecimal("203573.13"));
    }

    @Test
    void warnsWhenRunningBalanceDoesNotReconcile() {
        String text = """
                招商银行交易流水
                2026-01-01 -- 2026-01-31
                账号：6212********1234
                2026-01-01 CNY -10.00 90.00 快捷支付 示例商户
                2026-01-02 CNY -10.00 90.00 快捷支付 示例商户
                """;

        ParsedBill bill = parser.parseText(text);

        assertThat(bill.getItems()).hasSize(2);
        assertThat(bill.getSummary().get("balanceMismatches")).isEqualTo(1);
        assertThat((List<?>) bill.getSummary().get("warnings"))
                .anyMatch(warning -> String.valueOf(warning).contains("余额勾稽不一致"));
    }

    @Test
    void rejectsUnrelatedPdfText() {
        assertThatThrownBy(() -> parser.parseText("招商银行信用卡对账单 2026年10月"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("储蓄卡交易流水");
    }
}