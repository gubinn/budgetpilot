package uk.gubin.budgetpilot.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String detailMessage;

    public BizException(ErrorCode errorCode, Object... args) {
        super(errorCode.format(args));
        this.errorCode = errorCode;
        this.detailMessage = errorCode.format(args);
    }

    public BizException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
        this.detailMessage = detail;
    }
}
