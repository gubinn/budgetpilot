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
    AUTH_WRONG_CREDENTIALS(10005, "用户名或密码错误"),
    AUTH_USER_DISABLED(10006, "账户已被停用"),
    AUTH_WRONG_OLD_PASSWORD(10007, "原密码错误"),
    AUTH_USERNAME_EXISTS(80003, "用户名已存在"),

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
    TRANSACTION_TARGET_ACCOUNT_REQUIRED(30007, "请选择目标账户"),
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
    ANNUAL_BUDGET_NOT_FOUND(50004, "年度预算不存在"),
    ANNUAL_BUDGET_DUPLICATE(50005, "该年度预算已存在"),

    // 汇率 60001-60099
    CURRENCY_UNSUPPORTED(60001, "币种不支持: %s"),
    CURRENCY_API_FAILED(60002, "汇率获取失败: %s"),
    CURRENCY_API_LIMIT(60003, "汇率 API 限流"),

    // 商户 70001-70099
    MERCHANT_NOT_FOUND(70001, "商户不存在"),
    MERCHANT_NAME_EXISTS(70002, "商户名称已存在: %s"),
    MERCHANT_HAS_TRANSACTIONS(70003, "商户有关联交易记录，不可删除"),
    MERCHANT_SYSTEM_IMMUTABLE(70004, "系统预设商户不可删除"),

    // 鉴权 80001-80099
    AUTH_NOT_LOGGED_IN(80001, "未登录或登录已过期"),
    AUTH_NO_PERMISSION(80002, "无权限访问"),

    // 用户 80101-80199
    USER_NOT_FOUND(80101, "用户不存在"),

    // 公网登录验证码 80110-80119
    AUTH_OTP_INVALID(80110, "验证码错误或已过期"),
    AUTH_OTP_TOO_MANY(80111, "验证码尝试次数过多，请重新登录"),
    AUTH_OTP_SEND_FAILED(80112, "验证码发送失败，请检查通知渠道配置"),
    AUTH_OTP_NO_CHANNEL(80113, "该账号未配置 Telegram 或 WhatsApp，无法公网登录"),
    AUTH_OTP_RESEND_LIMIT(80114, "验证码发送过于频繁，请稍后再试"),
    AUTH_ADMIN_PUBLIC_FORBIDDEN(80115, "管理员账号仅允许内网登录"),

    // 卡券包 90001-90099
    PREPAID_CARD_NOT_FOUND(90001, "卡券不存在"),
    PREPAID_CARD_BALANCE_NOT_ENOUGH(90002, "卡券余额/次数不足"),
    PREPAID_CARD_VOIDED(90003, "卡券已作废，请先恢复"),
    PREPAID_CARD_EXPIRED(90004, "卡券已过有效期，请先修改有效期"),
    PREPAID_CARD_VALUE_INVALID(90005, "金额/次数格式不正确"),

    // 只读共享 91001-91099
    SHARE_LINK_INVALID(91001, "链接无效或已过期"),
    SHARE_PIN_REQUIRED(91002, "请输入 PIN"),
    SHARE_PIN_WRONG(91003, "PIN 不正确"),
    SHARE_TOO_MANY_ATTEMPTS(91004, "尝试次数过多，请稍后再试"),
    SHARE_NOT_FOUND(91005, "分享不存在"),

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
