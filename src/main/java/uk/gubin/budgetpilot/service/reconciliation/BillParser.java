package uk.gubin.budgetpilot.service.reconciliation;

/** 渠道账单解析器：支付宝 CSV、招行储蓄卡/信用卡 PDF 已实现，微信后续按同一接口扩展 */
public interface BillParser {
    boolean supports(BillChannel channel);

    ParsedBill parse(byte[] content) throws Exception;
}