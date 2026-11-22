package uk.gubin.budgetpilot.service.reconciliation;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 招商银行储蓄卡交易流水（PDF）解析器。
 *
 * <p>流水只带记账日期、没有时分秒，字段依次为：记账日期、货币、交易金额、联机余额、
 * 交易摘要、对手信息。负数表示支出，正数表示转入/退款/结息。解析后 paidTime 留空，
 * 由匹配器按「日期 + 金额 + 商户相似度」与邮件记下的流水对账。</p>
 *
 * <p>交易摘要是固定短语，长对手名在 PDF 里会折行。解析时会把明细行上方或下方的
 * 相邻文字拼回对手信息，并按内容顺序、坐标顺序各解析一次，取勾稽更完整的结果。</p>
 */
@Component
public class CmbDebitCardBillParser implements BillParser {

    /** 明细行：日期 货币 交易金额 联机余额 [交易摘要 对手信息] */
    private static final Pattern ROW = Pattern.compile(
            "^\\s*(\\d{4}-\\d{2}-\\d{2})\\s+([A-Z]{3})\\s+(-?[\\d,]+\\.\\d{2})\\s+"
                    + "(-?[\\d,]+\\.\\d{2})(?:\\s+(.*))?\\s*$");
    private static final Pattern PERIOD = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2})\\s*(?:--|—|至|~|～)\\s*(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern ACCOUNT_NO = Pattern.compile("账号[：:]\\s*([0-9*]+)");
    private static final Pattern ACCOUNT_NAME = Pattern.compile("户\\s*名[：:]\\s*(\\S+)");
    private static final Pattern PAGE_NO = Pattern.compile("^\\d+/\\d+$");
    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fff]");

    private static final String SECTION_TRANSFER = "转账";
    private static final String SECTION_REFUND = "退款";

    /** 招行流水常见的交易摘要，长短语优先匹配，避免把对手名首词误当成摘要 */
    private static final List<String> SUMMARY_TYPES = Stream.of(
                    "银联线上有卡支付", "银联在线支付", "银联快捷支付", "行内转账转入",
                    "行内转账转出", "跨行转账转入", "跨行转账转出", "信用卡还款",
                    "贷款还款", "快捷支付", "快捷退款", "网上支付", "手机支付",
                    "扫码支付", "账户结息", "代发工资", "消费退货", "消费",
                    "取现", "存款", "取款", "缴费", "代扣", "转账", "退款",
                    "结息", "利息", "工资", "其他")
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();

    /** 页眉、页脚、说明文字，不能当作折行的对手信息 */
    private static final List<String> NON_DATA_KEYWORDS = List.of(
            "招商银行交易流水", "Transaction Statement", "记账日期", "交易金额",
            "联机余额", "交易摘要", "对手信息", "Date Currency", "Balance",
            "Transaction", "Counter Party", "Amount", "Currency", "Verification",
            "户  名", "户 名", "账户类型", "开 户 行", "开户行", "申请时间",
            "验 证 码", "验证码", "温馨提示", "交易流水验真", "明细项", "基本项",
            "www.cmbchina.com");

    @Override
    public boolean supports(BillChannel channel) {
        return channel == BillChannel.CMB_DEBIT;
    }

    @Override
    public ParsedBill parse(byte[] content) throws Exception {
        String contentOrder;
        String positionOrder;
        try (PDDocument document = Loader.loadPDF(content)) {
            contentOrder = extract(document, false);
            positionOrder = extract(document, true);
        }
        ParsedBill primary = parseText(contentOrder, false);
        if (isClean(primary)) {
            return primary;
        }
        // 有些 PDF 导出工具不按视觉顺序写文本；按坐标排序再解析一次，取更完整的结果
        ParsedBill fallback = parseText(positionOrder, true);
        return score(fallback) > score(primary) ? fallback : primary;
    }

    private String extract(PDDocument document, boolean sortByPosition) throws Exception {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(sortByPosition);
        stripper.setLineSeparator("\n");
        return stripper.getText(document);
    }

    private boolean isClean(ParsedBill bill) {
        return !bill.getItems().isEmpty()
                && zero(bill.getSummary().get("balanceMismatches"))
                && zero(bill.getSummary().get("unparsedRows"));
    }

    private boolean zero(Object value) {
        return value == null || String.valueOf(value).equals("0");
    }

    private int score(ParsedBill bill) {
        return bill.getItems().size() * 10
                - intValue(bill.getSummary().get("balanceMismatches")) * 5
                - intValue(bill.getSummary().get("unparsedRows")) * 5;
    }

    private int intValue(Object value) {
        return value == null ? 0 : Integer.parseInt(String.valueOf(value));
    }

    /** 从流水文本解析，单独抽出便于用固定文本做单元测试。 */
    ParsedBill parseText(String text) {
        return parseText(text, false);
    }

    /**
     * @param allowBefore 是否允许把明细行上方的文字拼回该行。
     *                    按内容顺序导出的 PDF 折行总在明细行下方；允许向上找会把
     *                    上一行的折行尾巴误挂到下一行。
     */
    ParsedBill parseText(String text, boolean allowBefore) {
        // PDFBox 的列间分隔有时是 U+00A0，Java 的 \s 和 String.trim() 都不认
        String normalized = text == null ? "" : text
                .replace(' ', ' ')
                .replace(' ', ' ')
                .replace(' ', ' ');
        List<String> lines = normalized.lines().toList();
        if (lines.stream().noneMatch(line -> line.contains("招商银行交易流水"))
                && lines.stream().noneMatch(line -> ROW.matcher(line).matches())) {
            throw new IllegalArgumentException("无法识别招商银行交易流水，请确认导出的是储蓄卡交易流水 PDF");
        }

        String accountNo = firstMatch(lines, ACCOUNT_NO);
        String accountName = firstMatch(lines, ACCOUNT_NAME);
        LocalDate[] headerPeriod = headerPeriod(lines);

        ParsedBill bill = new ParsedBill();
        bill.setChannel(BillChannel.CMB_DEBIT.name());
        bill.setBillAccount(accountNo);

        Map<String, Integer> summaryCounts = new LinkedHashMap<>();
        Map<String, BigDecimal> summaryAmounts = new LinkedHashMap<>();
        Map<String, Integer> keySequence = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        boolean[] consumed = new boolean[lines.size()];
        BigDecimal previousBalance = null;
        BigDecimal expenseTotal = BigDecimal.ZERO;
        BigDecimal incomeTotal = BigDecimal.ZERO;
        BigDecimal refundTotal = BigDecimal.ZERO;
        BigDecimal transferTotal = BigDecimal.ZERO;
        int transferCount = 0;
        int interestCount = 0;
        int unparsedRows = 0;
        int foreignRows = 0;
        int balanceMismatches = 0;

        for (int i = 0; i < lines.size(); i++) {
            Matcher matcher = ROW.matcher(lines.get(i));
            if (!matcher.matches()) {
                continue;
            }
            String amountText = matcher.group(3);
            String balanceText = matcher.group(4);
            String rest = matcher.group(5) == null ? "" : matcher.group(5).trim();
            BigDecimal signedAmount = parseAmount(amountText);
            BigDecimal balance = parseAmount(balanceText);
            String summary = summaryOf(rest);
            String counterparty = counterpartyOf(rest, summary);

            // 折行的摘要/对手信息：明细行本身没有对手信息时，把相邻文字拼回来看。
            // 按内容顺序导出的 PDF 折行总在下方，不能再向上找，否则会把上一行的
            // 折行尾巴（例如最后一个「司」字）挂到下一行。
            if (counterparty.isEmpty()) {
                List<String> before = new ArrayList<>();
                if (allowBefore) {
                    for (int j = i - 1; j >= 0 && j >= i - 3 && !consumed[j]; j--) {
                        String fragment = fragmentOf(lines.get(j));
                        if (fragment == null) {
                            break;
                        }
                        before.add(0, fragment);
                        consumed[j] = true;
                    }
                }
                List<String> after = new ArrayList<>();
                for (int j = i + 1; j < lines.size() && j <= i + 3 && !consumed[j]; j++) {
                    String fragment = fragmentOf(lines.get(j));
                    if (fragment == null) {
                        break;
                    }
                    after.add(fragment);
                    consumed[j] = true;
                }
                if (!before.isEmpty() || !after.isEmpty()) {
                    if (summary.isEmpty()) {
                        // 摘要本身折到了相邻行：用空格分开「摘要」和「对手信息」再解析
                        StringBuilder combined = new StringBuilder();
                        before.forEach(part -> appendFragment(combined, part));
                        if (combined.length() > 0) {
                            combined.append(' ');
                        }
                        appendFragment(combined, rest);
                        if (!after.isEmpty() && combined.length() > 0) {
                            combined.append(' ');
                        }
                        after.forEach(part -> appendFragment(combined, part));
                        String combinedText = combined.toString();
                        summary = summaryOf(combinedText);
                        counterparty = counterpartyOf(combinedText, summary);
                    } else {
                        // 摘要已经在明细行上，上下折行都属于对手信息
                        StringBuilder joined = new StringBuilder();
                        before.forEach(part -> appendFragment(joined, part));
                        after.forEach(part -> appendFragment(joined, part));
                        counterparty = joined.toString();
                    }
                }
            }

            // 多币种账户：非人民币账户的余额与人民币余额不是同一个池子，不能一起勾稽
            if (!"CNY".equals(matcher.group(2))) {
                foreignRows++;
                continue;
            }
            if (summary.isEmpty()) {
                unparsedRows++;
                continue;
            }

            if (previousBalance != null
                    && previousBalance.add(signedAmount).compareTo(balance) != 0) {
                balanceMismatches++;
            }
            previousBalance = balance;

            String direction;
            if (isRefund(summary)) {
                direction = ReconcileConstants.DIRECTION_NEUTRAL;
            } else if (signedAmount.signum() < 0) {
                direction = ReconcileConstants.DIRECTION_EXPENSE;
            } else {
                direction = ReconcileConstants.DIRECTION_INCOME;
            }

            BigDecimal absAmount = signedAmount.abs();
            summaryCounts.merge(summary, 1, Integer::sum);
            summaryAmounts.merge(summary, absAmount, BigDecimal::add);
            if (summary.contains(SECTION_TRANSFER)) {
                transferCount++;
                transferTotal = transferTotal.add(absAmount);
            }
            if (summary.contains("结息")) {
                interestCount++;
            }
            switch (direction) {
                case ReconcileConstants.DIRECTION_EXPENSE -> expenseTotal = expenseTotal.add(absAmount);
                case ReconcileConstants.DIRECTION_INCOME -> incomeTotal = incomeTotal.add(absAmount);
                default -> refundTotal = refundTotal.add(absAmount);
            }

            ParsedBillItem item = new ParsedBillItem();
            item.setTradeDate(LocalDate.parse(matcher.group(1)));
            item.setBillNo(null);
            item.setMerchantOrderNo(null);
            item.setCreatedTime(null);
            item.setPaidTime(null);
            item.setCounterparty(counterparty);
            item.setSubject(null);
            item.setTradeStatus(summary);
            item.setDirection(direction);
            item.setAmount(absAmount);
            item.setSource("招商银行储蓄卡流水");
            item.setOrderType(accountNo == null ? "储蓄卡流水" : accountNo);
            item.setFee(BigDecimal.ZERO);
            item.setRefunded(BigDecimal.ZERO);
            item.setRemark("账号 " + (accountNo == null ? "-" : accountNo)
                    + " · 余额 " + balanceText);

            Map<String, String> raw = new LinkedHashMap<>();
            raw.put("账号", accountNo == null ? "" : accountNo);
            raw.put("户名", accountName == null ? "" : accountName);
            raw.put("记账日期", matcher.group(1));
            raw.put("币种", matcher.group(2));
            raw.put("交易金额", amountText);
            raw.put("联机余额", balanceText);
            raw.put("交易摘要", summary);
            raw.put("对手信息", counterparty);
            item.setRaw(raw);

            String keyBase = String.join("|",
                    BillChannel.CMB_DEBIT.name(),
                    matcher.group(1),
                    summary,
                    counterparty,
                    signedAmount.toPlainString(),
                    balanceText);
            int seq = keySequence.merge(keyBase, 1, Integer::sum);
            item.setItemKey(sha256(keyBase + "|" + seq));

            bill.getItems().add(item);
        }

        if (!bill.getItems().isEmpty()) {
            bill.setPeriodStart(bill.getItems().stream().map(ParsedBillItem::getTradeDate)
                    .min(LocalDate::compareTo).orElse(null));
            bill.setPeriodEnd(bill.getItems().stream().map(ParsedBillItem::getTradeDate)
                    .max(LocalDate::compareTo).orElse(null));
        }
        // 导出区间以页眉为准：区间内没有流水的日子也是银行给出的对账范围
        if (headerPeriod != null) {
            bill.setPeriodStart(headerPeriod[0]);
            bill.setPeriodEnd(headerPeriod[1]);
        }

        bill.getSummary().put("account", accountNo);
        bill.getSummary().put("accountName", accountName);
        bill.getSummary().put("rowCount", bill.getItems().size());
        bill.getSummary().put("expenseTotal", expenseTotal);
        bill.getSummary().put("incomeTotal", incomeTotal);
        bill.getSummary().put("refundTotal", refundTotal);
        bill.getSummary().put("netAmount", expenseTotal.subtract(refundTotal));
        bill.getSummary().put("transfer", Map.of("count", transferCount, "amount", transferTotal));
        bill.getSummary().put("interestCount", interestCount);
        bill.getSummary().put("summaries", sectionSummary(summaryCounts, summaryAmounts));
        bill.getSummary().put("balanceMismatches", balanceMismatches);
        bill.getSummary().put("balanceEnd", previousBalance);
        bill.getSummary().put("unparsedRows", unparsedRows);
        bill.getSummary().put("foreignAccountSkipped", foreignRows);

        if (foreignRows > 0) {
            warnings.add("流水包含 " + foreignRows + " 笔非人民币账户交易，未纳入本次对账");
        }
        if (unparsedRows > 0) {
            warnings.add("有 " + unparsedRows + " 行流水未能识别交易摘要，请检查 PDF 是否完整");
        }
        if (balanceMismatches > 0) {
            warnings.add("有 " + balanceMismatches + " 行余额勾稽不一致，请确认流水是否被截断");
        }
        bill.getSummary().put("warnings", warnings);
        return bill;
    }

    private String firstMatch(List<String> lines, Pattern pattern) {
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private LocalDate[] headerPeriod(List<String> lines) {
        for (String line : lines) {
            Matcher matcher = PERIOD.matcher(line);
            if (matcher.find()) {
                try {
                    return new LocalDate[]{
                            LocalDate.parse(matcher.group(1)),
                            LocalDate.parse(matcher.group(2))};
                } catch (Exception ignore) {
                    return null;
                }
            }
        }
        return null;
    }

    /** 相邻的折行文字：含中文、不是明细行/页码/页眉页脚时才算对手信息片段 */
    private String fragmentOf(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || PAGE_NO.matcher(trimmed).matches()) {
            return null;
        }
        if (ROW.matcher(trimmed).matches() || !CJK.matcher(trimmed).find()) {
            return null;
        }
        for (String keyword : NON_DATA_KEYWORDS) {
            if (trimmed.contains(keyword)) {
                return null;
            }
        }
        return trimmed;
    }

    /** 中文折行直接拼接；英文单词折行时补一个空格 */
    private void appendFragment(StringBuilder target, String fragment) {
        if (fragment == null || fragment.isBlank()) {
            return;
        }
        if (target.length() > 0) {
            char prev = target.charAt(target.length() - 1);
            char next = fragment.charAt(0);
            if (isAsciiWord(prev) && isAsciiWord(next)) {
                target.append(' ');
            }
        }
        target.append(fragment.trim());
    }

    private boolean isAsciiWord(char value) {
        return (value >= 'a' && value <= 'z') || (value >= 'A' && value <= 'Z')
                || (value >= '0' && value <= '9');
    }

    private String summaryOf(String rest) {
        if (rest == null || rest.isBlank()) {
            return "";
        }
        String value = rest.trim();
        for (String type : SUMMARY_TYPES) {
            if (value.equals(type) || value.startsWith(type + " ")) {
                return type;
            }
        }
        int space = value.indexOf(' ');
        return space < 0 ? value : value.substring(0, space);
    }

    private String counterpartyOf(String rest, String summary) {
        if (rest == null) {
            return "";
        }
        String value = rest.trim();
        if (summary == null || summary.isEmpty() || value.equals(summary)) {
            return "";
        }
        if (value.startsWith(summary + " ")) {
            return value.substring(summary.length()).trim();
        }
        int space = value.indexOf(' ');
        return space < 0 ? "" : value.substring(space + 1).trim();
    }

    private boolean isRefund(String summary) {
        return summary != null && (summary.contains(SECTION_REFUND)
                || summary.contains("退货") || summary.contains("冲正"));
    }

    private Map<String, Object> sectionSummary(Map<String, Integer> counts,
                                               Map<String, BigDecimal> amounts) {
        Map<String, Object> summary = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("count", entry.getValue());
            detail.put("amount", amounts.getOrDefault(entry.getKey(), BigDecimal.ZERO));
            summary.put(entry.getKey(), detail);
        }
        return summary;
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        String value = raw.replace(",", "").replace("¥", "").replace(" ", "").trim();
        if (value.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return Integer.toHexString(raw.hashCode());
        }
    }
}