package uk.gubin.budgetpilot.service.reconciliation;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 一份渠道账单的解析结果 */
@Data
public class ParsedBill {
    private String channel;
    private String billAccount;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private List<ParsedBillItem> items = new ArrayList<>();
    /** 账单页脚汇总、解析告警等，原样保存到批次 summary_json */
    private Map<String, Object> summary = new LinkedHashMap<>();
}