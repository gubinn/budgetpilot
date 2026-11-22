package uk.gubin.budgetpilot.service.reconciliation;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AlipayBillParserTest {

    private static final String HEADER = "交易号                  ,商家订单号               ,交易创建时间              ,"
            + "付款时间                ,最近修改时间              ,交易来源地     ,类型              ,"
            + "交易对方            ,商品名称                ,金额（元）   ,收/支     ,交易状态    ,"
            + "服务费（元）   ,成功退款（元）  ,备注                  ,资金状态     ,";

    private final AlipayBillParser parser = new AlipayBillParser();

    @Test
    void parsesGb18030BillWithHeaderAndFooter() throws Exception {
        String csv = """
                支付宝交易记录明细查询
                账号:[test@example.com]
                起始日期:[2026-10-01 00:00:00]    终止日期:[2026-11-01 00:00:00]
                ---------------------------------交易记录明细列表------------------------------------
                """ + HEADER + """

                2026000000000000000000000001\t,ORDER1\t,2026-10-31 17:50:29 ,2026-10-31 17:50:30 ,2026-10-31 17:50:30 ,支付宝网站     ,即时到账交易          ,李雅新             ,亲情卡                 ,7.25    ,支出      ,交易成功    ,0.00     ,0.00     ,                    ,已支出      ,
                2026000000000000000000000002\t,ORDER2\t,2026-10-31 12:15:15 ,2026-10-31 12:15:15 ,2026-10-31 12:15:15 ,其他（包括阿里巴巴和外部商家）,即时到账交易          ,美团              ,"团购, 午餐"           ,8.99    ,支出      ,交易成功    ,0.00     ,0.00     ,                    ,已支出      ,
                2026000000000000000000000003\t,ORDER3\t,2026-10-30 10:00:00 ,                    ,2026-10-30 10:00:00 ,其他（包括阿里巴巴和外部商家）,即时到账交易          ,铁路12306         ,退款-火车票              ,1633.92 ,不计收支    ,退款成功    ,0.00     ,0.00     ,                    ,已收入      ,
                2026000000000000000000000004\t,ORDER4\t,2026-10-29 11:00:00 ,2026-10-29 11:00:00 ,2026-10-29 11:00:00 ,其他（包括阿里巴巴和外部商家）,即时到账交易          ,拼多多平台商户         ,先用后付                ,0.00    ,支出      ,支付成功    ,0.00     ,0.00     ,                    ,已支出      ,
                ------------------------------------------------------------------------------------
                共4笔记录
                已收入:1笔,1633.92元
                待收入:0笔,0.00元
                已支出:3笔,16.24元
                待支出:0笔,0.00元
                导出时间:[2026-11-22 10:37:08]    用户:测试
                """;

        byte[] content = csv.getBytes(Charset.forName("GB18030"));
        ParsedBill bill = parser.parse(content);

        assertThat(bill.getBillAccount()).isEqualTo("test@example.com");
        assertThat(bill.getPeriodStart()).hasToString("2026-10-01");
        assertThat(bill.getPeriodEnd()).hasToString("2026-10-31");
        assertThat(bill.getItems()).hasSize(4);

        ParsedBillItem first = bill.getItems().get(0);
        assertThat(first.getAmount()).isEqualByComparingTo("7.25");
        assertThat(first.getDirection()).isEqualTo(ReconcileConstants.DIRECTION_EXPENSE);
        assertThat(first.getCounterparty()).isEqualTo("李雅新");
        assertThat(first.getTradeDate()).hasToString("2026-10-31");

        ParsedBillItem quoted = bill.getItems().get(1);
        assertThat(quoted.getSubject()).isEqualTo("团购, 午餐");

        ParsedBillItem refund = bill.getItems().get(2);
        assertThat(refund.getDirection()).isEqualTo(ReconcileConstants.DIRECTION_NEUTRAL);
        assertThat(refund.getPaidTime()).isNull();
        assertThat(refund.getTradeStatus()).isEqualTo("退款成功");

        assertThat(bill.getItems().get(3).getAmount()).isEqualByComparingTo("0.00");
        assertThat(bill.getSummary().get("parsedCount")).isEqualTo(4);
    }

    @Test
    void decodesUtf8WithBom() throws Exception {
        String csv = "﻿" + """
                支付宝交易记录明细查询
                账号:[test@example.com]
                起始日期:[2026-10-01 00:00:00]    终止日期:[2026-11-01 00:00:00]
                ---------------------------------交易记录明细列表------------------------------------
                """ + HEADER + """

                2026000000000000000000000001\t,ORDER1\t,2026-10-31 17:50:29 ,2026-10-31 17:50:30 ,2026-10-31 17:50:30 ,支付宝网站     ,即时到账交易          ,商户             ,商品                 ,1.00    ,支出      ,交易成功    ,0.00     ,0.00     ,                    ,已支出      ,
                ------------------------------------------------------------------------------------
                共1笔记录
                已支出:1笔,1.00元
                """;

        ParsedBill bill = parser.parse(csv.getBytes(StandardCharsets.UTF_8));
        assertThat(bill.getItems()).hasSize(1);
        assertThat(bill.getItems().get(0).getAmount()).isEqualByComparingTo("1.00");
    }

    @Test
    void sameBillNoWithDifferentRowsProducesDifferentKeys() throws Exception {
        String row1 = "2026000000000000000000000001\t,ORDER1\t,2026-10-20 12:20:45 ,2026-10-20 12:20:45 ,"
                + "2026-10-20 12:20:45 ,支付宝网站     ,即时到账交易          ,李雅新             ,亲情卡                 ,"
                + "411.30  ,支出      ,交易成功    ,0.00     ,0.00     ,                    ,已支出      ,";
        String row2 = "2026000000000000000000000001\t,ORDER1\t,2026-10-21 14:48:07 ,                    ,"
                + "2026-10-21 14:48:07 ,支付宝网站     ,即时到账交易          ,李雅新             ,退款-亲情卡             ,"
                + "14.97   ,不计收支    ,退款成功    ,0.00     ,0.00     ,                    ,已收入      ,";

        String csv = """
                支付宝交易记录明细查询
                账号:[test@example.com]
                起始日期:[2026-10-01 00:00:00]    终止日期:[2026-11-01 00:00:00]
                ---------------------------------交易记录明细列表------------------------------------
                """ + HEADER + "\n" + row1 + "\n" + row2 + """

                ------------------------------------------------------------------------------------
                共2笔记录
                """;

        ParsedBill bill = parser.parse(csv.getBytes(StandardCharsets.UTF_8));
        assertThat(bill.getItems()).hasSize(2);
        assertThat(bill.getItems().get(0).getItemKey())
                .isNotEqualTo(bill.getItems().get(1).getItemKey());
    }
}