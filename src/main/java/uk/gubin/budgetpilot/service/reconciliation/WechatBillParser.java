package uk.gubin.budgetpilot.service.reconciliation;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 微信支付「账单流水文件」XLSX 解析器。
 *
 * <p>文件由微信客户端导出：前 17 行是昵称、导出区间、笔数/金额汇总和说明，
 * 第 18 行是表头，之后是明细。交易时间是 Excel 日期序列号，不一定带日期格式，
 * 因此按数值日期处理。</p>
 *
 * <p>微信账单的「交易类型」决定流水性质（转账/零钱充值提现/退款），
 * 「当前状态」只表示资金状态；这里把交易类型写入 tradeStatus，当前状态写入 fundStatus，
 * 这样匹配器能正确识别转账和退款。</p>
 */
@Component
public class WechatBillParser implements BillParser {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern NICKNAME = Pattern.compile("微信昵称\\s*[：:]\\s*\\[([^]]*)]");
    private static final Pattern PERIOD = Pattern.compile(
            "起始时间\\s*[：:]\\s*\\[([^]]*)]\\s*终止时间\\s*[：:]\\s*\\[([^]]*)]");
    private static final Pattern EXPORT_TYPE = Pattern.compile("导出类型\\s*[：:]\\s*\\[([^]]*)]");
    private static final Pattern TOTAL_RECORDS = Pattern.compile("共\\s*(\\d+)\\s*笔记录");
    private static final Pattern SUMMARY_COUNT = Pattern.compile(
            "(收入|支出|中性交易)\\s*[：:]\\s*(\\d+)\\s*笔\\s*([\\d,.]+)\\s*元");
    private static final Pattern SERVICE_FEE = Pattern.compile("服务费\\s*[¥￥]\\s*([\\d.]+)");
    private static final Pattern REFUNDED = Pattern.compile("已退款\\s*[（(]?\\s*[¥￥]?\\s*([\\d.]+)");

    /** 表头别名 → 内部字段名；微信偶尔会在表头里加空格或全角括号 */
    private static final Map<String, String> COLUMN_ALIASES = Map.ofEntries(
            Map.entry("交易时间", "time"),
            Map.entry("交易类型", "orderType"),
            Map.entry("交易对方", "counterparty"),
            Map.entry("商品", "subject"),
            Map.entry("收/支", "direction"),
            Map.entry("金额(元)", "amount"),
            Map.entry("支付方式", "payMethod"),
            Map.entry("当前状态", "fundStatus"),
            Map.entry("交易单号", "billNo"),
            Map.entry("商户单号", "merchantOrderNo"),
            Map.entry("备注", "remark")
    );

    @Override
    public boolean supports(BillChannel channel) {
        return channel == BillChannel.WECHAT;
    }

    @Override
    public ParsedBill parse(byte[] content) throws Exception {
        ParsedBill bill = new ParsedBill();
        bill.setChannel(BillChannel.WECHAT.name());

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("文件中没有工作表");
            }
            Sheet sheet = workbook.getSheetAt(0);

            Map<String, Integer> columns = null;
            Map<String, Object> headerCounts = new HashMap<>();
            List<String> warnings = new ArrayList<>();
            Map<String, Integer> keySequence = new HashMap<>();
            BigDecimal expenseTotal = BigDecimal.ZERO;
            BigDecimal incomeTotal = BigDecimal.ZERO;
            BigDecimal neutralTotal = BigDecimal.ZERO;
            String nickname = null;
            String exportType = null;
            LocalDate periodStart = null;
            LocalDate periodEnd = null;

            for (Row row : sheet) {
                String rowText = rowText(row);
                if (columns == null) {
                    // 元数据里日期时间之间有空格，不能像表头那样去掉所有空白
                    String metadata = rowText;
                    if (metadata.contains("微信昵称")) {
                        Matcher m = NICKNAME.matcher(metadata);
                        if (m.find() && !m.group(1).isBlank()) {
                            nickname = m.group(1).trim();
                        }
                    }
                    if (metadata.contains("起始时间")) {
                        Matcher m = PERIOD.matcher(metadata);
                        if (m.find()) {
                            LocalDateTime start = parseDateTime(m.group(1), null);
                            LocalDateTime end = parseDateTime(m.group(2), null);
                            periodStart = start == null ? null : start.toLocalDate();
                            periodEnd = end == null ? null : end.toLocalDate();
                        }
                    }
                    if (metadata.contains("导出类型")) {
                        Matcher m = EXPORT_TYPE.matcher(metadata);
                        if (m.find() && !m.group(1).isBlank()) {
                            exportType = m.group(1).trim();
                        }
                    }
                    Matcher total = TOTAL_RECORDS.matcher(metadata);
                    if (total.find()) {
                        headerCounts.put("totalRecords", Integer.parseInt(total.group(1)));
                    }
                    Matcher summary = SUMMARY_COUNT.matcher(metadata);
                    while (summary.find()) {
                        String label = summary.group(1);
                        int count = Integer.parseInt(summary.group(2));
                        BigDecimal amount = parseAmount(summary.group(3));
                        headerCounts.put(label + "Count", count);
                        headerCounts.put(label + "Amount", amount);
                    }
                    Map<String, Integer> candidate = headerIndex(row);
                    if (candidate != null) {
                        columns = candidate;
                    }
                    continue;
                }

                if (rowText.isBlank()) {
                    continue;
                }
                LocalDateTime paidTime = parseDateTime(cell(row, columns, "time"), row.getRowNum() + 1, warnings);
                if (paidTime == null) {
                    continue;
                }
                BigDecimal amount = parseAmount(cell(row, columns, "amount"));
                if (amount == null) {
                    continue;
                }
                String type = trimToNull(cell(row, columns, "orderType"));
                String counterparty = trimToNull(cell(row, columns, "counterparty"));
                String subject = trimToNull(cell(row, columns, "subject"));
                String fundStatus = trimToNull(cell(row, columns, "fundStatus"));
                String payMethod = trimToNull(cell(row, columns, "payMethod"));
                String billNo = trimToNull(cell(row, columns, "billNo"));
                String merchantOrderNo = trimToNull(cell(row, columns, "merchantOrderNo"));
                String remark = trimToNull(cell(row, columns, "remark"));
                String direction = direction(cell(row, columns, "direction"));

                ParsedBillItem item = new ParsedBillItem();
                item.setBillNo(billNo);
                item.setMerchantOrderNo(merchantOrderNo);
                item.setCreatedTime(null);
                item.setPaidTime(paidTime);
                item.setTradeDate(paidTime.toLocalDate());
                item.setDirection(direction);
                item.setAmount(amount);
                item.setCounterparty(counterparty);
                item.setSubject(subject);
                // 交易类型（转账/退款/零钱充值提现）决定匹配时预期的流水类型
                item.setTradeStatus(type);
                item.setFundStatus(fundStatus);
                item.setSource("微信支付账单");
                item.setFee(serviceFee(remark));
                item.setRefunded(refunded(direction, fundStatus, amount));
                item.setRemark(remark);

                Map<String, String> raw = new LinkedHashMap<>();
                raw.put("交易时间", paidTime.format(DATE_TIME));
                putIfText(raw, "交易类型", type);
                putIfText(raw, "交易对方", counterparty);
                putIfText(raw, "商品", subject);
                putIfText(raw, "收/支", cell(row, columns, "direction"));
                raw.put("金额(元)", amount.toPlainString());
                putIfText(raw, "支付方式", payMethod);
                putIfText(raw, "当前状态", fundStatus);
                putIfText(raw, "交易单号", billNo);
                putIfText(raw, "商户单号", merchantOrderNo);
                putIfText(raw, "备注", remark);
                putIfText(raw, "微信昵称", nickname);
                item.setRaw(raw);

                String keyBase = String.join("|",
                        BillChannel.WECHAT.name(),
                        paidTime.format(DATE_TIME),
                        billNo == null ? "" : billNo,
                        amount.toPlainString(),
                        direction,
                        type == null ? "" : type);
                int seq = keySequence.merge(keyBase, 1, Integer::sum);
                item.setItemKey(sha256(keyBase + "|" + seq));

                switch (direction) {
                    case ReconcileConstants.DIRECTION_EXPENSE -> expenseTotal = expenseTotal.add(amount);
                    case ReconcileConstants.DIRECTION_INCOME -> incomeTotal = incomeTotal.add(amount);
                    default -> neutralTotal = neutralTotal.add(amount);
                }
                bill.getItems().add(item);
            }

            if (columns == null) {
                throw new IllegalArgumentException("未找到微信支付账单明细表头（交易时间/交易单号）");
            }
            if (bill.getItems().isEmpty()) {
                throw new IllegalArgumentException("账单中没有解析到交易明细");
            }

            bill.setBillAccount(nickname);
            bill.setPeriodStart(periodStart != null ? periodStart : minDate(bill));
            bill.setPeriodEnd(periodEnd != null ? periodEnd : maxDate(bill));

            bill.getSummary().put("nickname", nickname);
            bill.getSummary().put("exportType", exportType);
            bill.getSummary().put("rowCount", bill.getItems().size());
            bill.getSummary().put("expenseTotal", expenseTotal);
            bill.getSummary().put("incomeTotal", incomeTotal);
            bill.getSummary().put("neutralTotal", neutralTotal);
            bill.getSummary().putAll(toSummaryCounts(headerCounts));
            compareCounts(bill, headerCounts, expenseTotal, incomeTotal, neutralTotal, warnings);
            if (!warnings.isEmpty()) {
                bill.getSummary().put("warnings", warnings);
            }
        }
        return bill;
    }

    /** 找到明细表头行，返回「列名 → 单元格下标」 */
    private Map<String, Integer> headerIndex(Row row) {
        Map<String, Integer> found = new LinkedHashMap<>();
        for (Cell cell : row) {
            String header = COLUMN_ALIASES.get(normalize(cellText(cell)));
            if (header != null) {
                found.put(header, cell.getColumnIndex());
            }
        }
        return found.containsKey("time") && found.containsKey("amount")
                && found.containsKey("billNo") && found.size() >= 6 ? found : null;
    }

    private String rowText(Row row) {
        StringBuilder text = new StringBuilder();
        for (Cell cell : row) {
            String value = cellText(cell);
            if (!value.isBlank()) {
                if (text.length() > 0) {
                    text.append(' ');
                }
                text.append(value.trim());
            }
        }
        return text.toString();
    }

    private String cell(Row row, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        if (index == null) {
            return "";
        }
        return cellText(row.getCell(index));
    }

    private String cellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType() == CellType.FORMULA
                ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case STRING -> cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? DateUtil.getLocalDateTime(cell.getNumericCellValue()).format(DATE_TIME)
                    : BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private LocalDateTime parseDateTime(String value, Integer rowNumber) {
        return parseDateTime(value, rowNumber, new ArrayList<>());
    }

    private LocalDateTime parseDateTime(String value, Integer rowNumber, List<String> warnings) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, DATE_TIME);
        } catch (Exception ignored) {
            // 兼容日期单元格被格式化成 yyyy/M/d H:mm:ss 或只有日期的情况
        }
        for (String pattern : List.of("yyyy/M/d H:mm:ss", "yyyy-MM-dd HH:mm", "yyyy/M/d H:mm")) {
            try {
                return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
                // 继续尝试下一种格式
            }
        }
        try {
            double serial = Double.parseDouble(text);
            // 有些导出会丢掉日期格式，只剩 Excel 序列号
            if (serial > 20_000 && serial < 80_000) {
                return DateUtil.getLocalDateTime(serial);
            }
        } catch (NumberFormatException ignored) {
            // 不是数值，继续按日期字符串解析
        }
        try {
            return LocalDate.parse(text, DATE).atStartOfDay();
        } catch (Exception ignored) {
            warnings.add("第 " + rowNumber + " 行交易时间无法识别：" + text);
            return null;
        }
    }

    private BigDecimal parseAmount(String value) {
        String text = trimToNull(value);
        if (text == null || "/".equals(text)) {
            return null;
        }
        try {
            return new BigDecimal(text.replace(",", "").replace("¥", "").replace("￥", ""))
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String direction(String value) {
        String text = trimToNull(value);
        if ("收入".equals(text)) {
            return ReconcileConstants.DIRECTION_INCOME;
        }
        if ("支出".equals(text)) {
            return ReconcileConstants.DIRECTION_EXPENSE;
        }
        return ReconcileConstants.DIRECTION_NEUTRAL;
    }

    private BigDecimal serviceFee(String remark) {
        if (remark == null) {
            return BigDecimal.ZERO;
        }
        Matcher m = SERVICE_FEE.matcher(remark);
        BigDecimal fee = m.find() ? parseAmount(m.group(1)) : null;
        return fee == null ? BigDecimal.ZERO : fee;
    }

    private BigDecimal refunded(String direction, String status, BigDecimal amount) {
        if (status == null || ReconcileConstants.DIRECTION_INCOME.equals(direction)) {
            return BigDecimal.ZERO;
        }
        if (status.contains("全额退款")) {
            return amount;
        }
        Matcher m = REFUNDED.matcher(status);
        BigDecimal refunded = m.find() ? parseAmount(m.group(1)) : null;
        return refunded == null ? BigDecimal.ZERO : refunded;
    }

    private Map<String, Object> toSummaryCounts(Map<String, Object> headerCounts) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("declared", headerCounts);
        return summary;
    }

    private void compareCounts(ParsedBill bill, Map<String, Object> declared,
                               BigDecimal expenseTotal, BigDecimal incomeTotal, BigDecimal neutralTotal,
                               List<String> warnings) {
        compare(bill.getItems().size(), (Integer) declared.get("totalRecords"), "总笔数", warnings);
        compare(count(bill, ReconcileConstants.DIRECTION_EXPENSE),
                (Integer) declared.get("支出Count"), "支出笔数", warnings);
        compare(count(bill, ReconcileConstants.DIRECTION_INCOME),
                (Integer) declared.get("收入Count"), "收入笔数", warnings);
        compare(count(bill, ReconcileConstants.DIRECTION_NEUTRAL),
                (Integer) declared.get("中性交易Count"), "中性交易笔数", warnings);
        compareAmount(expenseTotal, (BigDecimal) declared.get("支出Amount"), "支出金额", warnings);
        compareAmount(incomeTotal, (BigDecimal) declared.get("收入Amount"), "收入金额", warnings);
        compareAmount(neutralTotal, (BigDecimal) declared.get("中性交易Amount"), "中性交易金额", warnings);
    }

    private void compare(Integer actual, Integer declared, String label, List<String> warnings) {
        if (declared != null && !declared.equals(actual)) {
            warnings.add("账单汇总" + label + "为 " + declared + "，实际解析 " + actual + " 行，请核对文件是否完整");
        }
    }

    private void compareAmount(BigDecimal actual, BigDecimal declared, String label, List<String> warnings) {
        if (declared != null && actual.compareTo(declared) != 0) {
            warnings.add("账单汇总" + label + "为 " + declared.toPlainString()
                    + "，实际解析 " + actual.toPlainString() + "，请核对文件是否完整");
        }
    }

    private int count(ParsedBill bill, String direction) {
        return (int) bill.getItems().stream().filter(i -> direction.equals(i.getDirection())).count();
    }

    private LocalDate minDate(ParsedBill bill) {
        return bill.getItems().stream().map(ParsedBillItem::getTradeDate)
                .min(LocalDate::compareTo).orElse(null);
    }

    private LocalDate maxDate(ParsedBill bill) {
        return bill.getItems().stream().map(ParsedBillItem::getTradeDate)
                .max(LocalDate::compareTo).orElse(null);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('（', '(').replace('）', ')')
                .replace('：', ':').replaceAll("\\s+", "").trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() || "/".equals(text) ? null : text;
    }

    private void putIfText(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value.trim());
        }
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("计算账单行摘要失败", e);
        }
    }
}