package uk.gubin.budgetpilot.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 通用 10001-10099
    SUCCESS(0, "ok"),
    PARAM_ERROR(10001, "参数校验失败: %s"),
    RESOURCE_NOT_FOUND(10002, "资源不存在: %s"),
    SYSTEM_ERROR(10003, "系统内部异常"),
    DUPLICATE_NAME(10004, "名称已存在: %s"),

    // 账户 20001-20099
    ACCOUNT_NOT_FOUND(20001, "账户不存在"),
    ACCOUNT_DISABLED(20002, "账户已停用"),
    ACCOUNT_BALANCE_NOT_ENOUGH(20003, "账户余额不足"),
    ACCOUNT_NAME_EXISTS(20004, "账户名称已存在"),
    ACCOUNT_HAS_TRANSACTIONS(20005, "账户有关联交易记录，不可停用"),

    // 交易 30001-30099
    TRANSACTION_NOT_FOUND(30001, "交易记录不存在"),
    TRANSACTION_AMOUNT_INVALID(30002, "交易金额无效: %s"),
    TRANSACTION_SAME_ACCOUNT(30003, "转账不能是同一账户"),
    TRANSACTION_CATEGORY_MISMATCH(30004, "分类与交易类型不匹配"),
    TRANSACTION_ALREADY_CONFIRMED(30005, "交易已确认"),
    TRANSACTION_CATEGORY_NOT_FOUND(30006, "分类不存在"),

    // 分类 40001-40099
    CATEGORY_NOT_FOUND(40001, "分类不存在"),
    CATEGORY_SYSTEM_IMMUTABLE(40002, "系统分类不可删除或修改类型"),
    CATEGORY_HAS_CHILDREN(40003, "分类有子分类，不可删除"),
    CATEGORY_HAS_TRANSACTIONS(40004, "分类下有交易记录，不可删除"),
    CATEGORY_CIRCULAR_REFERENCE(40005, "分类存在循环引用: %s"),

    // 预算 50001-50099
    BUDGET_NOT_FOUND(50001, "预算不存在"),
    BUDGET_LOCKED(50002, "预算已锁定，不可修改"),
    BUDGET_MONTH_DUPLICATE(50003, "该月份预算已存在"),

    // 汇率 60001-60099
    CURRENCY_UNSUPPORTED(60001, "币种不支持: %s"),
    CURRENCY_API_FAILED(60002, "汇率获取失败: %s"),
    CURRENCY_API_LIMIT(60003, "汇率 API 限流"),

    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public String format(Object... args) {
        return String.format(message, args);
    }
}
