package uk.gubin.budgetpilot.service.reconciliation;

import java.util.List;
import java.util.Locale;

/**
 * 对账渠道。候选流水的识别方式随渠道不同：
 * 支付宝/微信在银行流水里体现为备注中的渠道关键字；招行渠道直接按账户筛选。
 *
 * <p>招行账单拆成储蓄卡与信用卡两个渠道：两者流水结构、账单文件格式都不同，
 * 分开后各自的候选账户和匹配窗口才能独立设置。</p>
 */
public enum BillChannel {
    ALIPAY("支付宝", List.of("支付宝", "花呗", "淘宝")),
    WECHAT("微信支付", List.of("财付通", "微信支付")),
    CMB_DEBIT("招商银行储蓄卡", List.of()),
    CMB_CREDIT("招商银行信用卡", List.of());

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

    /** 是否为银行自己的账单渠道（流水就是银行数据，不再按备注关键字识别） */
    public boolean isBankChannel() {
        return this == CMB_DEBIT || this == CMB_CREDIT;
    }

    public static BillChannel of(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        // 兼容拆分前的历史数据：旧 CMB 渠道按储蓄卡处理
        if ("CMB".equals(normalized)) {
            return CMB_DEBIT;
        }
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 判断一笔流水是否属于该渠道。
     * 优先看邮件记账写入的 extFields.channel；历史数据没有该字段时回退到备注关键字。
     * 招行渠道的候选流水由批次指定的账户范围收窄，这里不再按备注过滤。
     */
    public boolean matches(String extChannel, String note) {
        if (isBankChannel()) {
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