package uk.gubin.budgetpilot.service.reconciliation;

/** 渠道账单解析器：支付宝 CSV、微信 XLSX、招行储蓄卡/信用卡 PDF */
public interface BillParser {
    boolean supports(BillChannel channel);

    ParsedBill parse(byte[] content) throws Exception;
}