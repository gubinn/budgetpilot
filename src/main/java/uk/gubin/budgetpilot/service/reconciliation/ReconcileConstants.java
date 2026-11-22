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
}