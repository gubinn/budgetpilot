package uk.gubin.budgetpilot.service.reconciliation;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WechatBillParserTest {

    private final WechatBillParser parser = new WechatBillParser();

    @Test
    void parsesWechatBillWithTransfersRefundsAndWalletRows() throws Exception {
        byte[] content = workbook(List.of(
                List.of("微信支付账单明细"),
                List.of("微信昵称：[示例用户]"),
                List.of("起始时间：[2026-01-01 00:00:00] 终止时间：[2026-11-20 23:59:59]"),
                List.of("导出类型：[全部账单]"),
                List.of("导出时间：[2026-11-22 20:06:27]"),
                List.of(),
                List.of("共8笔记录"),
                List.of("收入：3笔 769.57元"),
                List.of("支出：3笔 342.50元"),
                List.of("中性交易：2笔 3.80元"),
                List.of("4. 本账单中所有时间均为UTC+08:00时间"),
                List.of(),
                List.of("----------------------微信支付账单明细列表--------------------"),
                header(),
                row("2026-02-28 12:00:00", "转账", "示例用户", "转账备注:微信转账",
                        "收入", "668.00", "/", "已存入零钱", "1000001", "/", "/"),
                row("2026-03-03 09:15:00", "商户消费", "示例商户甲", "订单付款",
                        "支出", "25.80", "招商银行信用卡(0886)", "支付成功", "1000002", "2000002", "/"),
                row("2026-05-20 15:43:59", "微信红包（单发）", "发给示例好友", "/",
                        "支出", "100.00", "零钱", "已全额退款", "1000003", "2000003", "/"),
                row("2026-05-21 15:44:03", "微信红包-退款", "/", "/",
                        "收入", "100.00", "零钱", "已全额退款", "1000004", "", "/"),
                row("2026-02-28 20:03:17", "七鲜-退款", "示例生鲜", "示例生鲜",
                        "收入", "1.57", "浦发银行信用卡(7271)", "已退款¥1.57", "1000005", "", "/"),
                row("2026-02-28 19:53:35", "商户消费", "示例生鲜超市", "示例生鲜",
                        "支出", "216.70", "浦发银行信用卡(7271)", "已退款(¥1.57)", "1000006", "2000006", "/"),
                row("2026-01-17 17:37:09", "零钱充值", "招商银行(0611)", "/",
                        "/", "2.34", "招商银行储蓄卡(0611)", "充值完成", "1000007", "/", "/"),
                row("2026-01-17 17:35:59", "零钱提现", "招商银行(0611)", "/",
                        "/", "1.46", "招商银行储蓄卡(0611)", "提现已到账", "1000008", "/", "服务费¥0.01")
        ));

        ParsedBill bill = parser.parse(content);

        assertThat(bill.getChannel()).isEqualTo(BillChannel.WECHAT.name());
        assertThat(bill.getBillAccount()).isEqualTo("示例用户");
        assertThat(bill.getPeriodStart()).hasToString("2026-01-01");
        assertThat(bill.getPeriodEnd()).hasToString("2026-11-20");
        assertThat(bill.getItems()).hasSize(8);
        assertThat(bill.getItems()).extracting(ParsedBillItem::getItemKey).doesNotHaveDuplicates();

        ParsedBillItem transfer = bill.getItems().get(0);
        assertThat(transfer.getPaidTime()).hasToString("2026-02-28T12:00");
        assertThat(transfer.getTradeDate()).hasToString("2026-02-28");
        assertThat(transfer.getDirection()).isEqualTo(ReconcileConstants.DIRECTION_INCOME);
        assertThat(transfer.getTradeStatus()).isEqualTo("转账");
        assertThat(transfer.getFundStatus()).isEqualTo("已存入零钱");
        assertThat(transfer.getBillNo()).isEqualTo("1000001");
        assertThat(ReconcileConstants.transactionType(
                transfer.getDirection(), transfer.getTradeStatus())).isEqualTo(3);

        ParsedBillItem expense = bill.getItems().get(1);
        assertThat(expense.getDirection()).isEqualTo(ReconcileConstants.DIRECTION_EXPENSE);
        assertThat(expense.getCounterparty()).isEqualTo("示例商户甲");
        assertThat(expense.getAmount()).isEqualByComparingTo("25.80");
        assertThat(expense.getRaw()).containsEntry("支付方式", "招商银行信用卡(0886)")
                .containsEntry("微信昵称", "示例用户");

        // 原支付行即使当前状态是「已全额退款」，也仍是支出而非退款
        ParsedBillItem original = bill.getItems().get(2);
        assertThat(original.getTradeStatus()).isEqualTo("微信红包（单发）");
        assertThat(original.getFundStatus()).isEqualTo("已全额退款");
        assertThat(original.getRefunded()).isEqualByComparingTo("100.00");
        assertThat(ReconcileConstants.transactionType(
                original.getDirection(), original.getTradeStatus())).isEqualTo(1);

        ParsedBillItem refund = bill.getItems().get(3);
        assertThat(refund.getDirection()).isEqualTo(ReconcileConstants.DIRECTION_INCOME);
        assertThat(refund.getTradeStatus()).isEqualTo("微信红包-退款");
        assertThat(ReconcileConstants.transactionType(
                refund.getDirection(), refund.getTradeStatus())).isEqualTo(4);

        ParsedBillItem partialRefund = bill.getItems().get(5);
        assertThat(partialRefund.getRefunded()).isEqualByComparingTo("1.57");
        assertThat(ReconcileConstants.transactionType(
                partialRefund.getDirection(), partialRefund.getTradeStatus())).isEqualTo(1);

        ParsedBillItem recharge = bill.getItems().get(6);
        assertThat(recharge.getDirection()).isEqualTo(ReconcileConstants.DIRECTION_NEUTRAL);
        assertThat(ReconcileConstants.transactionType(
                recharge.getDirection(), recharge.getTradeStatus())).isEqualTo(3);

        ParsedBillItem withdraw = bill.getItems().get(7);
        assertThat(withdraw.getDirection()).isEqualTo(ReconcileConstants.DIRECTION_NEUTRAL);
        assertThat(withdraw.getFee()).isEqualByComparingTo("0.01");
        assertThat(ReconcileConstants.transactionType(
                withdraw.getDirection(), withdraw.getTradeStatus())).isEqualTo(3);

        assertThat(bill.getSummary().get("rowCount")).isEqualTo(8);
        assertThat(bill.getSummary().get("expenseTotal")).isEqualTo(new BigDecimal("342.50"));
        assertThat(bill.getSummary().get("incomeTotal")).isEqualTo(new BigDecimal("769.57"));
        assertThat(bill.getSummary().get("neutralTotal")).isEqualTo(new BigDecimal("3.80"));
        assertThat(bill.getSummary()).doesNotContainKey("warnings");
    }

    @Test
    void keepsItemKeysStableAcrossImports() throws Exception {
        byte[] content = workbook(List.of(
                List.of("微信支付账单明细"),
                List.of("微信昵称：[示例用户]"),
                List.of("起始时间：[2026-01-01 00:00:00] 终止时间：[2026-01-31 23:59:59]"),
                List.of("共1笔记录"),
                header(),
                row("2026-01-02 14:10:46", "商户消费", "示例商户", "商品",
                        "支出", "44.90", "零钱", "支付成功", "9000001", "8000001", "/")
        ));

        List<String> first = parser.parse(content).getItems()
                .stream().map(ParsedBillItem::getItemKey).toList();
        List<String> second = parser.parse(content).getItems()
                .stream().map(ParsedBillItem::getItemKey).toList();

        assertThat(first).isEqualTo(second);
    }

    @Test
    void warnsWhenDeclaredTotalsDoNotMatchRows() throws Exception {
        byte[] content = workbook(List.of(
                List.of("微信支付账单明细"),
                List.of("微信昵称：[示例用户]"),
                List.of("起始时间：[2026-01-01 00:00:00] 终止时间：[2026-01-31 23:59:59]"),
                List.of("共2笔记录"),
                List.of("支出：2笔 99.99元"),
                header(),
                row("2026-01-02 14:10:46", "商户消费", "示例商户", "商品",
                        "支出", "44.90", "零钱", "支付成功", "9000001", "8000001", "/")
        ));

        ParsedBill bill = parser.parse(content);

        assertThat(bill.getItems()).hasSize(1);
        assertThat((List<?>) bill.getSummary().get("warnings"))
                .anyMatch(warning -> String.valueOf(warning).contains("总笔数"))
                .anyMatch(warning -> String.valueOf(warning).contains("支出金额"));
    }

    @Test
    void rejectsUnrelatedWorkbook() throws Exception {
        byte[] content = workbook(List.of(
                List.of("示例表格"),
                List.of("姓名", "金额")
        ));

        assertThatThrownBy(() -> parser.parse(content))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("微信支付账单");
    }

    private List<String> header() {
        return List.of("交易时间", "交易类型", "交易对方", "商品", "收/支", "金额(元)",
                "支付方式", "当前状态", "交易单号", "商户单号", "备注");
    }

    private List<String> row(String time, String type, String counterparty, String subject,
                             String direction, String amount, String payMethod, String fundStatus,
                             String billNo, String merchantOrderNo, String remark) {
        return List.of(time, type, counterparty, subject, direction, amount,
                payMethod, fundStatus, billNo, merchantOrderNo, remark);
    }

    private byte[] workbook(List<List<String>> rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("微信支付账单");
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r);
                List<String> values = rows.get(r);
                for (int c = 0; c < values.size(); c++) {
                    String value = values.get(c);
                    Cell cell = row.createCell(c);
                    if (c == 0 && value.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                        cell.setCellValue(LocalDateTime.parse(value.replace(' ', 'T')));
                        cell.setCellStyle(dateStyle);
                    } else {
                        cell.setCellValue(value);
                    }
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}