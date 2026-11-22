package uk.gubin.budgetpilot.service.reconciliation;

import java.util.List;
import java.util.Locale;

/**
 * 对账渠道。候选流水的识别方式随渠道不同：
 * 支付宝/微信在银行流水里体现为备注中的渠道关键字，招行流水本身就是银行数据。
 */
public enum BillChannel {
    ALIPAY("支付宝", List.of("支付宝", "花呗", "淘宝")),
    WECHAT("微信支付", List.of("财付通", "微信支付")),
    CMB("招商银行", List.of());

    private final String label;
    private final List<String> noteKeywords;

    BillChannel(String label, List<String> noteKeywords) {
        this.label = label;
        this.noteKeywords = noteKeywords;
    }

    public String getLabel() {
        return label;
    }

    public List<String> getNoteKeywords() {
        return noteKeywords;
    }

    public static BillChannel of(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 判断一笔流水是否属于该渠道。
     * 优先看邮件记账写入的 extFields.channel；历史数据没有该字段时回退到备注关键字。
     */
    public boolean matches(String extChannel, String note) {
        if (this == CMB) {
            return true;
        }
        if (extChannel != null && !extChannel.isBlank()) {
            if (extChannel.contains(label)
                    || ("WECHAT".equals(name()) && (extChannel.contains("微信") || extChannel.contains("财付通")))) {
                return true;
            }
        }
        if (note == null) {
            return false;
        }
        for (String keyword : noteKeywords) {
            if (note.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}