package uk.gubin.budgetpilot.service.reconciliation;

/** 对账状态与方向常量，与数据库字段取值保持一致 */
public final class ReconcileConstants {
    private ReconcileConstants() {
    }

    public static final String DIRECTION_EXPENSE = "EXPENSE";
    public static final String DIRECTION_INCOME = "INCOME";
    public static final String DIRECTION_NEUTRAL = "NEUTRAL";

    public static final String STATUS_MATCHED = "MATCHED";
    public static final String STATUS_AMBIGUOUS = "AMBIGUOUS";
    public static final String STATUS_UNMATCHED = "UNMATCHED";
    public static final String STATUS_IGNORED = "IGNORED";

    public static final String TYPE_AUTO = "AUTO";
    public static final String TYPE_AGGREGATE = "AGGREGATE";
    public static final String TYPE_MANUAL = "MANUAL";
    public static final String TYPE_CREATED = "CREATED";

    public static final String BATCH_DONE = "DONE";
    public static final String BATCH_PARTIAL = "PARTIAL";

    public static final String IGNORE_ZERO_AMOUNT = "零金额授权/先用后付";
    public static final String IGNORE_TRANSACTION_CHANGED = "流水已修改/删除";
    public static final String IGNORE_CMB_INTERNAL = "朝朝宝内部划转";
    public static final String IGNORE_CMB_CD_MATURITY = "大额存单到期内部划转";

    /**
     * 账单方向 + 交易摘要 → 交易 type（1 支出 / 2 收入 / 3 转账 / 4 退款）。
     * 储蓄卡流水里的「行内转账转入/转出」在账本中记成转账，不能当成收入或支出。
     */
    public static Integer transactionType(String direction, String tradeStatus) {
        String status = tradeStatus == null ? "" : tradeStatus;
        if (isRefund(status)) {
            return 4;
        }
        if (status.contains("转账")) {
            return 3;
        }
        // 微信零钱充值/提现是微信余额与银行卡之间的内部划转，不影响收支
        if (status.contains("零钱充值") || status.contains("零钱提现")) {
            return 3;
        }
        return switch (direction == null ? "" : direction) {
            case DIRECTION_EXPENSE -> 1;
            case DIRECTION_INCOME -> 2;
            default -> null;
        };
    }

    public static boolean isRefund(String tradeStatus) {
        if (tradeStatus == null) {
            return false;
        }
        return tradeStatus.contains("退款") || tradeStatus.contains("退货")
                || tradeStatus.contains("冲正");
    }

    /**
     * 账单行与流水类型是否对应。
     * 邮件记账曾把储蓄卡退款记成支出（type=1），账单退款行仍允许与备注含「退款」的支出流水核对，
     * 否则这类退款永远匹配不上，也无法在手工候选中看到。
     */
    public static boolean matchesTransactionType(String direction, String tradeStatus,
                                                 Integer transactionType, String note) {
        Integer expected = transactionType(direction, tradeStatus);
        if (expected == null || transactionType == null) {
            return false;
        }
        if (expected.equals(transactionType)) {
            return true;
        }
        if (expected == 4) {
            return transactionType == 1 && note != null && note.contains("退款");
        }
        // 银行摘要里的「转账汇款/行内转账」在账本中可能被记成普通收支，
        // 普通收支也可能被记成转账；类型不同时仍允许金额、日期和对手信息去判断。
        if (expected == 3) {
            return (DIRECTION_EXPENSE.equals(direction) && transactionType == 1)
                    || (DIRECTION_INCOME.equals(direction) && transactionType == 2)
                    // 微信零钱充值/提现没有收支方向，账本里可能记成收入或支出
                    || (DIRECTION_NEUTRAL.equals(direction)
                    && (transactionType == 1 || transactionType == 2));
        }
        return (expected == 1 || expected == 2) && transactionType == 3;
    }
}