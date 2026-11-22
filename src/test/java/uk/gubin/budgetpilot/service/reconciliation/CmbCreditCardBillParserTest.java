package uk.gubin.budgetpilot.service.reconciliation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CmbCreditCardBillParserTest {

    private final CmbCreditCardBillParser parser = new CmbCreditCardBillParser();

    @Test
    void parsesCreditCardStatementSections() {
        String text = """
                招商银行信用卡对账单（个人消费卡账户 2026年10月）（补）
                CMB Credit Card Statement (2026.10)
                 本期账务明细 Transaction Details
                 人民币账户 RMB A/C
                交易日 记账日 交易摘要 人民币金额 卡号末四位 交易地金额
                 还款
                 09/26  掌上生活还款 -12,345.67 5678 -12,345.67
                 退款
                 09/21  09/22  支付宝-示例商户丁 -0.11 1234 -0.11(CN)
                 消费
                 09/15  09/16  支付宝-示例商户乙 6.00 1234 6.00(CN)
                 10/02  10/03  示例商户-Apple Pay:0000 92.30 1234 108.00(HK)
                ¥ 98.19 ¥ 12,345.67 ¥ 12,345.67 ¥ 98.30 ¥ 0.11 ¥ 0.00
                """;

        ParsedBill bill = parser.parseText(text);

        assertThat(bill.getChannel()).isEqualTo(BillChannel.CMB_CREDIT.name());
        assertThat(bill.getBillAccount()).isEqualTo("1234");
        assertThat(bill.getPeriodStart()).hasToString("2026-09-15");
        assertThat(bill.getPeriodEnd()).hasToString("2026-10-02");
        assertThat(bill.getItems()).hasSize(3);

        ParsedBillItem refund = bill.getItems().get(0);
        assertThat(refund.getTradeDate()).hasToString("2026-09-21");
        assertThat(refund.getDirection()).isEqualTo(ReconcileConstants.DIRECTION_NEUTRAL);
        assertThat(refund.getTradeStatus()).isEqualTo("退款");
        assertThat(refund.getAmount()).isEqualByComparingTo("0.11");
        assertThat(refund.getPaidTime()).isNull();

        ParsedBillItem expense = bill.getItems().get(1);
        assertThat(expense.getTradeDate()).hasToString("2026-09-15");
        assertThat(expense.getDirection()).isEqualTo(ReconcileConstants.DIRECTION_EXPENSE);
        assertThat(expense.getCounterparty()).isEqualTo("支付宝-示例商户乙");
        assertThat(expense.getAmount()).isEqualByComparingTo("6.00");

        ParsedBillItem foreign = bill.getItems().get(2);
        assertThat(foreign.getAmount()).isEqualByComparingTo("92.30");
        assertThat(foreign.getSubject()).isEqualTo("交易地金额 108.00(HK)");
        assertThat(foreign.getOrderType()).isEqualTo("消费");

        @SuppressWarnings("unchecked")
        Map<String, Object> repayment = (Map<String, Object>) bill.getSummary().get("还款");
        assertThat(repayment.get("count")).isEqualTo(1);
        assertThat((BigDecimal) repayment.get("amount")).isEqualByComparingTo("-12345.67");
        assertThat((java.util.List<?>) bill.getSummary().get("warnings")).isEmpty();
    }

    @Test
    void skipsNonRmbAccountSections() {
        String text = """
                招商银行信用卡对账单（个人消费卡账户 2027年1月）
                 人民币账户 RMB A/C
                 消费
                 12/31  01/01  支付宝-跨年商户 10.00 1234 10.00(CN)
                 美元账户 USD A/C
                 消费
                 01/05  01/06  AMAZON US 5.00 1234 5.00(US)
                """;

        ParsedBill bill = parser.parseText(text);

        assertThat(bill.getItems()).singleElement()
                .extracting(ParsedBillItem::getTradeDate)
                .hasToString("2026-12-31");
        assertThat(bill.getSummary().get("foreignAccountSkipped")).isEqualTo(1);
        assertThat((java.util.List<?>) bill.getSummary().get("warnings"))
                .anyMatch(w -> w.toString().contains("非人民币账户"));
    }

    @Test
    void resolvesCrossYearStatementDates() {
        String text = """
                招商银行信用卡对账单（个人消费卡账户 2026年1月）
                 消费
                 12/31  01/01  支付宝-跨年商户 10.00 1234 10.00(CN)
                """;

        ParsedBill bill = parser.parseText(text);

        assertThat(bill.getItems()).singleElement()
                .extracting(ParsedBillItem::getTradeDate)
                .hasToString("2025-12-31");
    }
}