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
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 招商银行信用卡电子账单（PDF）解析器。
 *
 * <p>账单按「还款 / 退款 / 消费 / 取现 …」分节，每行只有交易日和记账日、没有时分秒。
 * 因此账单明细的 paidTime / createdTime 留空，匹配器会改用「按日匹配 + 商户相似度」
 * 的规则，与每日信用管家邮件记下的流水对账。</p>
 *
 * <p>还款行是「从储蓄卡转入信用卡」的还款流水，不是信用卡消费，本解析器会把它们
 * 汇总到 summary 里但不生成对账明细，避免几十万的还款额干扰消费核对。</p>
 */
@Component
public class CmbCreditCardBillParser implements BillParser {

    private static final Pattern TITLE = Pattern.compile("(\\d{4})年(\\d{1,2})月");
    // 交易地金额与币种可能缺省（还款行通常只有卡号末四位和金额，没有 (CN)）
    private static final Pattern ROW_WITH_TRADE_DATE = Pattern.compile(
            "^\\s*(\\d{2}/\\d{2})\\s+(\\d{2}/\\d{2})\\s+(.+?)\\s+(-?[\\d,]+\\.\\d{2})\\s+(\\d{4})"
                    + "(?:\\s+(-?[\\d,]+\\.\\d{2})(?:\\(([A-Z]{2,3})\\))?)?\\s*$");
    private static final Pattern ROW_POST_DATE_ONLY = Pattern.compile(
            "^\\s*(\\d{2}/\\d{2})\\s+(.+?)\\s+(-?[\\d,]+\\.\\d{2})\\s+(\\d{4})"
                    + "(?:\\s+(-?[\\d,]+\\.\\d{2})(?:\\(([A-Z]{2,3})\\))?)?\\s*$");
    private static final Pattern FOOTER_AMOUNT = Pattern.compile("¥\\s*([\\d,]+\\.\\d{2})");
    private static final Pattern ACCOUNT_HEADER = Pattern.compile(
            "(人民币|美元|港币|欧元|日元|英镑|澳元|加元)账户");
    private static final Set<String> SECTIONS = new LinkedHashSet<>(List.of(
            "还款", "退款", "退货", "冲正", "溢缴款", "消费", "取现", "费用", "利息",
            "转账", "分期", "年费", "其他"));

    private static final String SECTION_REPAYMENT = "还款";
    private static final String SECTION_REFUND = "退款";

    @Override
    public boolean supports(BillChannel channel) {
        return channel == BillChannel.CMB_CREDIT;
    }

    @Override
    public ParsedBill parse(byte[] content) throws Exception {
        String text;
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setLineSeparator("\n");
            text = stripper.getText(document);
        }
        return parseText(text);
    }

    /** 从账单文本解析，单独抽出便于用固定文本做单元测试。 */
    ParsedBill parseText(String text) {
        // PDFBox 的列间分隔有时是 U+00A0，Java 的 \s 和 String.trim() 都不认
        String normalized = text == null ? "" : text
                .replace('\u00A0', ' ')
                .replace('\u2007', ' ')
                .replace('\u202F', ' ');
        List<String> lines = normalized.lines().toList();

        Integer year = null;
        Integer month = null;
        for (String line : lines) {
            Matcher matcher = TITLE.matcher(line);
            if (matcher.find()) {
                year = Integer.parseInt(matcher.group(1));
                month = Integer.parseInt(matcher.group(2));
                break;
            }
        }
        if (year == null) {
            throw new IllegalArgumentException("无法识别账单月份，请确认是招行信用卡电子账单");
        }

        ParsedBill bill = new ParsedBill();
        bill.setChannel(BillChannel.CMB_CREDIT.name());
        String section = null;
        Map<String, Integer> cardCounts = new LinkedHashMap<>();
        Map<String, Integer> sectionCounts = new LinkedHashMap<>();
        Map<String, BigDecimal> sectionAmounts = new LinkedHashMap<>();
        Map<String, Integer> keySequence = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        int unparsedRows = 0;
        int repaymentCount = 0;
        BigDecimal repaymentAmount = BigDecimal.ZERO;
        int foreignAccountRows = 0;
        String accountCurrency = "CNY";

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // 账单可能分「人民币账户 / 美元账户」多段；非人民币账户的金额无法直接入账
            Matcher account = ACCOUNT_HEADER.matcher(trimmed);
            if (account.find()) {
                accountCurrency = switch (account.group(1)) {
                    case "人民币" -> "CNY";
                    case "美元" -> "USD";
                    case "港币" -> "HKD";
                    case "欧元" -> "EUR";
                    case "日元" -> "JPY";
                    case "英镑" -> "GBP";
                    case "澳元" -> "AUD";
                    case "加元" -> "CAD";
                    default -> accountCurrency;
                };
                continue;
            }
            if (SECTIONS.contains(trimmed)) {
                section = trimmed;
                continue;
            }

            Matcher withTrade = ROW_WITH_TRADE_DATE.matcher(line);
            Matcher postOnly = null;
            String tradeDateText;
            String postDateText;
            String description;
            String amountText;
            String cardSuffix;
            String originalAmount;
            String currency;
            if (withTrade.matches()) {
                tradeDateText = withTrade.group(1);
                postDateText = withTrade.group(2);
                description = withTrade.group(3).trim();
                amountText = withTrade.group(4);
                cardSuffix = withTrade.group(5);
                originalAmount = withTrade.group(6);
                currency = withTrade.group(7);
            } else {
                postOnly = ROW_POST_DATE_ONLY.matcher(line);
                if (postOnly.matches()) {
                    tradeDateText = null;
                    postDateText = postOnly.group(1);
                    description = postOnly.group(2).trim();
                    amountText = postOnly.group(3);
                    cardSuffix = postOnly.group(4);
                    originalAmount = postOnly.group(5);
                    currency = postOnly.group(6);
                } else {
                    if (trimmed.matches("^\\d{2}/\\d{2}.*")) {
                        unparsedRows++;
                    }
                    continue;
                }
            }

            // 未归到分节的行（通常是表头/页眉）如果字段齐全也按消费处理，避免漏账
            String rowSection = section == null ? "消费" : section;
            BigDecimal signedAmount = parseAmount(amountText);
            BigDecimal absAmount = signedAmount.abs();
            LocalDate tradeDate = resolveDate(year, month, tradeDateText != null ? tradeDateText : postDateText);
            LocalDate postDate = resolveDate(year, month, postDateText);

            if (!"CNY".equals(accountCurrency)) {
                foreignAccountRows++;
                continue;
            }

            sectionCounts.merge(rowSection, 1, Integer::sum);
            sectionAmounts.merge(rowSection, signedAmount, BigDecimal::add);

            if (SECTION_REPAYMENT.equals(rowSection)) {
                repaymentCount++;
                repaymentAmount = repaymentAmount.add(signedAmount);
                continue;
            }
            if (tradeDate == null) {
                unparsedRows++;
                continue;
            }

            ParsedBillItem item = new ParsedBillItem();
            item.setTradeDate(tradeDate);
            item.setBillNo(null);
            item.setMerchantOrderNo(null);
            item.setCreatedTime(null);
            item.setPaidTime(null);
            item.setCounterparty(description);
            item.setSubject(foreignCurrencySubject(originalAmount, currency));
            item.setTradeStatus(SECTION_REFUND.equals(rowSection) || signedAmount.signum() < 0
                    ? SECTION_REFUND : rowSection);
            item.setDirection(SECTION_REFUND.equals(rowSection) || signedAmount.signum() < 0
                    ? ReconcileConstants.DIRECTION_NEUTRAL
                    : ReconcileConstants.DIRECTION_EXPENSE);
            item.setAmount(absAmount);
            item.setSource("招商银行信用卡账单");
            item.setOrderType(rowSection);
            item.setFee(BigDecimal.ZERO);
            item.setRefunded(BigDecimal.ZERO);
            item.setRemark("卡号 " + cardSuffix + " · 记账日 "
                    + (postDate == null ? postDateText : postDate));

            Map<String, String> raw = new LinkedHashMap<>();
            raw.put("交易日", tradeDateText == null ? "" : resolveDate(year, month, tradeDateText).toString());
            raw.put("记账日", postDate == null ? postDateText : postDate.toString());
            raw.put("交易摘要", description);
            raw.put("人民币金额", amountText);
            raw.put("卡号末四位", cardSuffix);
            raw.put("交易地金额", originalAmount == null ? "" : originalAmount);
            raw.put("交易地币种", currency == null ? "" : currency);
            raw.put("账单分类", rowSection);
            item.setRaw(raw);

            String keyBase = String.join("|",
                    BillChannel.CMB_CREDIT.name(),
                    tradeDate.toString(),
                    postDate == null ? postDateText : postDate.toString(),
                    description,
                    absAmount.toPlainString(),
                    item.getDirection());
            int seq = keySequence.merge(keyBase, 1, Integer::sum);
            item.setItemKey(sha256(keyBase + "|" + seq));

            bill.getItems().add(item);
            cardCounts.merge(cardSuffix, 1, Integer::sum);
        }

        bill.setBillAccount(cardCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null));

        if (!bill.getItems().isEmpty()) {
            bill.setPeriodStart(bill.getItems().stream().map(ParsedBillItem::getTradeDate)
                    .min(LocalDate::compareTo).orElse(null));
            bill.setPeriodEnd(bill.getItems().stream().map(ParsedBillItem::getTradeDate)
                    .max(LocalDate::compareTo).orElse(null));
        }

        loadFooter(bill, lines, warnings);
        BigDecimal expenseSum = bill.getItems().stream()
                .filter(i -> ReconcileConstants.DIRECTION_EXPENSE.equals(i.getDirection()))
                .map(ParsedBillItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundSum = bill.getItems().stream()
                .filter(i -> ReconcileConstants.DIRECTION_NEUTRAL.equals(i.getDirection()))
                .map(ParsedBillItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> repayment = new LinkedHashMap<>();
        repayment.put("count", repaymentCount);
        repayment.put("amount", repaymentAmount);
        bill.getSummary().put("还款", repayment);
        bill.getSummary().put("cards", cardCounts);
        bill.getSummary().put("sections", sectionSummary(sectionCounts, sectionAmounts));
        bill.getSummary().put("parsedCount", bill.getItems().size());
        bill.getSummary().put("expenseTotal", expenseSum);
        bill.getSummary().put("refundTotal", refundSum);
        bill.getSummary().put("netAmount", expenseSum.subtract(refundSum));
        bill.getSummary().put("foreignAccountSkipped", foreignAccountRows);

        if (foreignAccountRows > 0) {
            warnings.add("账单包含 " + foreignAccountRows + " 笔非人民币账户交易，未纳入本次对账");
        }
        if (cardCounts.size() > 1) {
            warnings.add("账单包含多张卡的消费（" + String.join("/", cardCounts.keySet())
                    + "），只会匹配所选账户的流水");
        }
        if (unparsedRows > 0) {
            warnings.add("有 " + unparsedRows + " 行账单明细未能识别，请检查 PDF 是否完整");
        }
        BigDecimal footerExpense = toDecimal(bill.getSummary().get("footerNewCharges"));
        BigDecimal footerRefund = toDecimal(bill.getSummary().get("footerAdjustment"));
        if (footerExpense != null && footerExpense.compareTo(expenseSum) != 0) {
            warnings.add("账单「本期账单金额」" + footerExpense + " 与解析消费合计 " + expenseSum + " 不一致");
        }
        if (footerRefund != null && footerRefund.compareTo(refundSum) != 0) {
            warnings.add("账单「本期调整金额」" + footerRefund + " 与解析退款合计 " + refundSum + " 不一致");
        }
        bill.getSummary().put("warnings", warnings);
        return bill;
    }

    /**
     * 解析页脚的本期应还金额 = 上期账单金额 - 上期还款金额 + 本期账单金额 - 本期调整金额 + 循环利息。
     * 六个数全部在一行，带 ¥ 前缀。
     */
    private void loadFooter(ParsedBill bill, List<String> lines, List<String> warnings) {
        for (String line : lines) {
            Matcher matcher = FOOTER_AMOUNT.matcher(line);
            List<BigDecimal> amounts = new ArrayList<>();
            while (matcher.find()) {
                amounts.add(parseAmount(matcher.group(1)));
            }
            if (amounts.size() < 6) {
                continue;
            }
            BigDecimal newBalance = amounts.get(0);
            BigDecimal balanceBf = amounts.get(1);
            BigDecimal payment = amounts.get(2);
            BigDecimal newCharges = amounts.get(3);
            BigDecimal adjustment = amounts.get(4);
            BigDecimal interest = amounts.get(5);
            bill.getSummary().put("footerNewBalance", newBalance);
            bill.getSummary().put("footerBalanceBf", balanceBf);
            bill.getSummary().put("footerPayment", payment);
            bill.getSummary().put("footerNewCharges", newCharges);
            bill.getSummary().put("footerAdjustment", adjustment);
            bill.getSummary().put("footerInterest", interest);

            BigDecimal computed = balanceBf.subtract(payment).add(newCharges)
                    .subtract(adjustment).add(interest);
            if (computed.compareTo(newBalance) != 0) {
                warnings.add("账单金额勾稽不一致：计算值 " + computed + "，账单本期应还 " + newBalance);
            }
            return;
        }
    }

    private Map<String, Object> sectionSummary(Map<String, Integer> counts, Map<String, BigDecimal> amounts) {
        Map<String, Object> summary = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("count", entry.getValue());
            detail.put("amount", amounts.getOrDefault(entry.getKey(), BigDecimal.ZERO));
            summary.put(entry.getKey(), detail);
        }
        return summary;
    }

    private String foreignCurrencySubject(String originalAmount, String currency) {
        if (currency == null || "CN".equals(currency)) {
            return null;
        }
        return "交易地金额 " + originalAmount + "(" + currency + ")";
    }

    private LocalDate resolveDate(int statementYear, int statementMonth, String mmdd) {
        if (mmdd == null || !mmdd.matches("\\d{2}/\\d{2}")) {
            return null;
        }
        int month = Integer.parseInt(mmdd.substring(0, 2));
        int day = Integer.parseInt(mmdd.substring(3, 5));
        // 账单周期跨年时（例如 1 月账单里出现 12/31），月份大于账单月即属于上一年
        int year = month > statementMonth ? statementYear - 1 : statementYear;
        try {
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
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

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
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