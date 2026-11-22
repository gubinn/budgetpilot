package uk.gubin.budgetpilot.service.reconciliation;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 支付宝「交易记录明细查询」CSV 解析器。
 *
 * 文件由支付宝网页导出：GB18030（也可能 UTF-8）、CRLF、前 4 行说明、表头带尾随空格、
 * 末尾有汇总行。交易号在同一份账单里可能被原交易和退款行共用，因此 itemKey 用
 * 交易号 + 创建时间 + 金额 + 收支方向 + 商品名称 的摘要做幂等键。
 */
@Component
public class AlipayBillParser implements BillParser {

    private static final Charset GB18030 = Charset.forName("GB18030");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern ACCOUNT = Pattern.compile("账号:\\[([^]]*)]");
    private static final Pattern PERIOD = Pattern.compile(
            "起始日期:\\[([^]]*)]\\s*终止日期:\\[([^]]*)]");
    private static final Pattern FOOTER_COUNT = Pattern.compile("共(\\d+)笔记录");
    // 页脚 "已支出:115笔,15929.84元" 会被 CSV 按逗号拆成两列，金额在下一列
    private static final Pattern FOOTER_LABEL = Pattern.compile("(已支出|待支出|已收入|待收入):(\\d+)笔");

    private static final Map<String, String> COLUMN_ALIASES = Map.ofEntries(
            Map.entry("交易号", "billNo"),
            Map.entry("商家订单号", "merchantOrderNo"),
            Map.entry("交易创建时间", "createdTime"),
            Map.entry("付款时间", "paidTime"),
            Map.entry("最近修改时间", "modifiedTime"),
            Map.entry("交易来源地", "source"),
            Map.entry("类型", "orderType"),
            Map.entry("交易对方", "counterparty"),
            Map.entry("商品名称", "subject"),
            Map.entry("金额(元)", "amount"),
            Map.entry("收/支", "direction"),
            Map.entry("交易状态", "tradeStatus"),
            Map.entry("服务费(元)", "fee"),
            Map.entry("成功退款(元)", "refunded"),
            Map.entry("备注", "remark"),
            Map.entry("资金状态", "fundStatus")
    );

    @Override
    public boolean supports(BillChannel channel) {
        return channel == BillChannel.ALIPAY;
    }

    @Override
    public ParsedBill parse(byte[] content) throws Exception {
        String text = decode(content);
        ParsedBill bill = new ParsedBill();
        bill.setChannel(BillChannel.ALIPAY.name());

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        List<CSVRecord> records;
        try (CSVParser parser = CSVParser.parse(new StringReader(text), format)) {
            records = parser.getRecords();
        }

        boolean inItems = false;
        Map<String, Integer> headerIndex = null;
        int footerCount = -1;
        BigDecimal footerExpense = null;
        BigDecimal footerIncome = null;
        List<String> warnings = new ArrayList<>();

        for (CSVRecord record : records) {
            if (record.size() == 0) {
                continue;
            }
            String first = cell(record, 0);
            if (!inItems) {
                if (first.startsWith("账号:")) {
                    Matcher m = ACCOUNT.matcher(first);
                    if (m.find()) {
                        bill.setBillAccount(m.group(1).trim());
                    }
                }
                if (first.startsWith("起始日期:")) {
                    Matcher m = PERIOD.matcher(first);
                    if (m.find()) {
                        LocalDateTime start = parseDateTime(m.group(1));
                        LocalDateTime end = parseDateTime(m.group(2));
                        bill.setPeriodStart(start == null ? null : start.toLocalDate());
                        bill.setPeriodEnd(end == null ? null
                                // 支付宝网页导出常用「次日 00:00:00」表示不含当日，需回退一天
                                : (end.toLocalTime().equals(LocalTime.MIDNIGHT)
                                ? end.toLocalDate().minusDays(1) : end.toLocalDate()));
                    }
                }
                if ("交易号".equals(normalizeHeader(first))) {
                    headerIndex = buildHeaderIndex(record);
                    inItems = true;
                }
                continue;
            }

            if (first.startsWith("---") || first.contains("笔记录") || first.startsWith("导出时间:")) {
                break;
            }
            if (headerIndex == null || first.isBlank()) {
                continue;
            }

            Map<String, String> row = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
                row.put(entry.getKey(), cell(record, entry.getValue()));
            }

            ParsedBillItem item = toItem(row);
            if (item != null) {
                bill.getItems().add(item);
            }
        }

        // 页脚在明细之后，继续扫一遍拿到校验信息
        for (CSVRecord record : records) {
            if (record.size() == 0) {
                continue;
            }
            String line = cell(record, 0);
            Matcher count = FOOTER_COUNT.matcher(line);
            if (count.find()) {
                footerCount = Integer.parseInt(count.group(1));
            }
            Matcher summary = FOOTER_LABEL.matcher(line);
            if (summary.find()) {
                String label = summary.group(1);
                BigDecimal amount = parseAmount(cell(record, 1).replace("元", ""));
                if ("已支出".equals(label)) {
                    footerExpense = amount;
                } else if ("已收入".equals(label)) {
                    footerIncome = amount;
                }
                bill.getSummary().put(label, Map.of(
                        "count", Integer.parseInt(summary.group(2)),
                        "amount", amount));
            }
        }

        bill.getSummary().put("parsedCount", bill.getItems().size());
        if (footerCount >= 0 && footerCount != bill.getItems().size()) {
            warnings.add("账单页脚记录数 " + footerCount + " 与解析结果 " + bill.getItems().size() + " 不一致");
        }

        BigDecimal expenseSum = bill.getItems().stream()
                .filter(i -> ReconcileConstants.DIRECTION_EXPENSE.equals(i.getDirection()))
                .map(i -> i.getAmount().subtract(i.getRefunded() == null ? BigDecimal.ZERO : i.getRefunded()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal incomeSum = bill.getItems().stream()
                .filter(i -> ReconcileConstants.DIRECTION_INCOME.equals(i.getDirection()))
                .map(ParsedBillItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (footerExpense != null && footerExpense.compareTo(expenseSum) != 0) {
            warnings.add("已支出汇总 " + footerExpense + " 与解析净支出 " + expenseSum + " 不一致");
        }
        if (footerIncome != null && footerIncome.compareTo(incomeSum) != 0) {
            warnings.add("已收入汇总 " + footerIncome + " 与解析收入 " + incomeSum + " 不一致");
        }
        bill.getSummary().put("expenseNet", expenseSum);
        bill.getSummary().put("incomeTotal", incomeSum);
        bill.getSummary().put("warnings", warnings);

        if (bill.getPeriodStart() == null || bill.getPeriodEnd() == null) {
            LocalDate min = bill.getItems().stream().map(ParsedBillItem::getTradeDate)
                    .filter(java.util.Objects::nonNull).min(LocalDate::compareTo).orElse(null);
            LocalDate max = bill.getItems().stream().map(ParsedBillItem::getTradeDate)
                    .filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(null);
            if (bill.getPeriodStart() == null) {
                bill.setPeriodStart(min);
            }
            if (bill.getPeriodEnd() == null) {
                bill.setPeriodEnd(max);
            }
        }
        return bill;
    }

    private Map<String, Integer> buildHeaderIndex(CSVRecord header) {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String normalized = normalizeHeader(cell(header, i));
            String field = COLUMN_ALIASES.get(normalized);
            if (field != null) {
                index.put(field, i);
            }
        }
        return index;
    }

    private String normalizeHeader(String raw) {
        return raw.replace('（', '(').replace('）', ')').replace(" ", "").trim();
    }

    private ParsedBillItem toItem(Map<String, String> row) {
        String billNo = row.getOrDefault("billNo", "");
        String created = row.getOrDefault("createdTime", "");
        String paid = row.getOrDefault("paidTime", "");
        String directionText = row.getOrDefault("direction", "");
        String amountText = row.getOrDefault("amount", "");

        LocalDateTime createdTime = parseDateTime(created);
        LocalDateTime paidTime = parseDateTime(paid);
        LocalDate tradeDate = paidTime != null ? paidTime.toLocalDate()
                : (createdTime != null ? createdTime.toLocalDate() : null);
        if (tradeDate == null && billNo.isBlank()) {
            return null;
        }
        if (tradeDate == null) {
            return null;
        }

        ParsedBillItem item = new ParsedBillItem();
        item.setBillNo(billNo);
        item.setMerchantOrderNo(row.getOrDefault("merchantOrderNo", ""));
        item.setCreatedTime(createdTime);
        item.setPaidTime(paidTime);
        item.setTradeDate(tradeDate);
        item.setDirection(toDirection(directionText));
        item.setAmount(parseAmount(amountText));
        item.setCounterparty(row.getOrDefault("counterparty", ""));
        item.setSubject(row.getOrDefault("subject", ""));
        item.setTradeStatus(row.getOrDefault("tradeStatus", ""));
        item.setFundStatus(row.getOrDefault("fundStatus", ""));
        item.setSource(row.getOrDefault("source", ""));
        item.setOrderType(row.getOrDefault("orderType", ""));
        item.setFee(parseAmount(row.getOrDefault("fee", "0")));
        item.setRefunded(parseAmount(row.getOrDefault("refunded", "0")));
        item.setRemark(row.getOrDefault("remark", ""));
        item.setRaw(new LinkedHashMap<>(row));
        item.setItemKey(buildItemKey(item));
        return item;
    }

    private String toDirection(String text) {
        String value = text == null ? "" : text.trim();
        return switch (value) {
            case "支出" -> ReconcileConstants.DIRECTION_EXPENSE;
            case "收入" -> ReconcileConstants.DIRECTION_INCOME;
            default -> ReconcileConstants.DIRECTION_NEUTRAL;
        };
    }

    private String buildItemKey(ParsedBillItem item) {
        String raw = String.join("|",
                BillChannel.ALIPAY.name(),
                item.getBillNo() == null ? "" : item.getBillNo().trim(),
                item.getCreatedTime() == null ? "" : item.getCreatedTime().format(DATE_TIME),
                item.getAmount() == null ? "0" : item.getAmount().stripTrailingZeros().toPlainString(),
                item.getDirection() == null ? "" : item.getDirection(),
                item.getSubject() == null ? "" : item.getSubject().trim());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    private String decode(byte[] content) throws CharacterCodingException {
        int offset = 0;
        if (content.length >= 3 && (content[0] & 0xFF) == 0xEF
                && (content[1] & 0xFF) == 0xBB && (content[2] & 0xFF) == 0xBF) {
            offset = 3;
        }
        ByteBuffer buffer = ByteBuffer.wrap(content, offset, content.length - offset);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(buffer)
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(content, offset, content.length - offset, GB18030);
        }
    }

    private String cell(CSVRecord record, int index) {
        if (index < 0 || index >= record.size()) {
            return "";
        }
        String value = record.get(index);
        return value == null ? "" : value.trim();
    }

    private LocalDateTime parseDateTime(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME);
        } catch (Exception ignored) {
            try {
                return LocalDate.parse(value).atStartOfDay();
            } catch (Exception ignored2) {
                return null;
            }
        }
    }

    private LocalDate parseDate(String raw) {
        LocalDateTime value = parseDateTime(raw);
        return value == null ? null : value.toLocalDate();
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        String value = raw.replace(",", "").replace(" ", "").trim();
        if (value.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}