package uk.gubin.budgetpilot.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.*;

/**
 * GlobalExceptionHandler 测试 —— 验证异常处理行为。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ============ BIZ EXCEPTION ============

    @Nested
    @DisplayName("业务异常处理")
    class BizExceptionTests {

        @Test
        @DisplayName("返回业务错误码和消息")
        void handleBizException() {
            BizException ex = new BizException(ErrorCode.ACCOUNT_NOT_FOUND);

            Result<?> result = handler.handleBizException(ex);

            assertThat(result.getCode()).isEqualTo(20001);
            assertThat(result.getMessage()).isNotNull();
        }

        @Test
        @DisplayName("带参数的错误消息格式化")
        void handleBizException_WithParams() {
            BizException ex = new BizException(ErrorCode.CURRENCY_UNSUPPORTED, "BTC");

            Result<?> result = handler.handleBizException(ex);

            assertThat(result.getMessage()).contains("BTC");
        }
    }

    // ============ VALIDATION EXCEPTION ============

    @Nested
    @DisplayName("校验异常处理")
    class ValidationExceptionTests {

        @Test
        @DisplayName("MethodArgumentNotValidException 返回 PARAM_ERROR")
        void handleMethodArgumentNotValid() {
            BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Object(), "target");
            errors.addError(new FieldError("target", "name", "不能为空"));
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, errors);

            Result<?> result = handler.handleValidationException(ex);

            assertThat(result.getCode()).isEqualTo(10001);
            assertThat(result.getMessage()).contains("不能为空");
        }

        @Test
        @DisplayName("BindException 返回 PARAM_ERROR")
        void handleBindException() {
            BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Object(), "target");
            errors.addError(new FieldError("target", "amount", "金额必须大于0"));
            BindException ex = new BindException(errors);

            Result<?> result = handler.handleBindException(ex);

            assertThat(result.getCode()).isEqualTo(10001);
            assertThat(result.getMessage()).contains("金额必须大于0");
        }

        @Test
        @DisplayName("多个字段错误合并为一条消息")
        void handleMultipleErrors() {
            BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Object(), "target");
            errors.addError(new FieldError("target", "name", "不能为空"));
            errors.addError(new FieldError("target", "amount", "必须大于0"));
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, errors);

            Result<?> result = handler.handleValidationException(ex);

            String msg = result.getMessage();
            assertThat(msg).contains("不能为空");
            assertThat(msg).contains("必须大于0");
        }
    }

    // ============ GENERIC EXCEPTION ============

    @Nested
    @DisplayName("兜底异常处理")
    class GenericExceptionTests {

        @Test
        @DisplayName("NullPointerException 返回 SYSTEM_ERROR，不暴露堆栈")
        void handleNullPointerException() {
            NullPointerException ex = new NullPointerException("something broke");

            Result<?> result = handler.handleException(ex);

            assertThat(result.getCode()).isEqualTo(10003);
            assertThat(result.getMessage()).isEqualTo("系统内部异常");
            assertThat(result.getMessage()).doesNotContain("NullPointerException");
            assertThat(result.getMessage()).doesNotContain("broke");
        }

        @Test
        @DisplayName("RuntimeException 返回 SYSTEM_ERROR")
        void handleRuntimeException() {
            RuntimeException ex = new RuntimeException("unexpected error");

            Result<?> result = handler.handleException(ex);

            assertThat(result.getCode()).isEqualTo(10003);
            assertThat(result.getMessage()).isEqualTo("系统内部异常");
            assertThat(result.getMessage()).doesNotContain("unexpected error");
        }

        @Test
        @DisplayName("任意 Exception 返回 SYSTEM_ERROR")
        void handleAnyException() {
            Exception ex = new Exception("test");

            Result<?> result = handler.handleException(ex);

            assertThat(result.getCode()).isEqualTo(10003);
            assertThat(result.getMessage()).isEqualTo("系统内部异常");
        }
    }
}
