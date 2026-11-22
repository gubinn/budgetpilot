package uk.gubin.budgetpilot.service.reconciliation;

/** 渠道账单解析器：支付宝已实现，微信/招行后续按同一接口扩展 */
public interface BillParser {
    boolean supports(BillChannel channel);

    ParsedBill parse(byte[] content) throws Exception;
}