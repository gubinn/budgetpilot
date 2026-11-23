package uk.gubin.budgetpilot.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReadonlyFilterTest {

    @Test
    @DisplayName("只读会话允许读取业务数据")
    void allowsReadEndpoints() {
        assertThat(ReadonlyFilter.isAllowed("GET", "/api/v1/accounts")).isTrue();
        assertThat(ReadonlyFilter.isAllowed("GET", "/api/v1/transactions")).isTrue();
        assertThat(ReadonlyFilter.isAllowed("GET", "/api/v1/reports/overview")).isTrue();
        assertThat(ReadonlyFilter.isAllowed("GET", "/api/v1/prepaid-cards/summary")).isTrue();
        assertThat(ReadonlyFilter.isAllowed("HEAD", "/api/v1/accounts")).isTrue();
        assertThat(ReadonlyFilter.isAllowed("OPTIONS", "/api/v1/accounts")).isTrue();
    }

    @Test
    @DisplayName("只读会话拒绝所有写请求")
    void rejectsWrites() {
        assertThat(ReadonlyFilter.isAllowed("POST", "/api/v1/transactions")).isFalse();
        assertThat(ReadonlyFilter.isAllowed("PUT", "/api/v1/transactions/1")).isFalse();
        assertThat(ReadonlyFilter.isAllowed("PATCH", "/api/v1/accounts/1")).isFalse();
        assertThat(ReadonlyFilter.isAllowed("DELETE", "/api/v1/prepaid-cards/1")).isFalse();
        assertThat(ReadonlyFilter.isAllowed("POST", "/api/v1/reconciliation/import")).isFalse();
    }

    @Test
    @DisplayName("只读会话不能碰用户、分享、系统配置和 API Key")
    void rejectsSensitiveEndpoints() {
        assertThat(ReadonlyFilter.isAllowed("GET", "/api/v1/users/config")).isFalse();
        assertThat(ReadonlyFilter.isAllowed("GET", "/api/v1/shares")).isFalse();
        assertThat(ReadonlyFilter.isAllowed("GET", "/api/v1/shares/public/code")).isTrue();
        assertThat(ReadonlyFilter.isAllowed("POST", "/api/v1/shares/public/code/verify")).isTrue();
        assertThat(ReadonlyFilter.isAllowed("GET", "/api/v1/reconciliation/batches")).isFalse();
        assertThat(ReadonlyFilter.isAllowed("GET", "/api/v1/recurring-rules")).isFalse();
        assertThat(ReadonlyFilter.isAllowed("GET", "/api/v1/alert-rules")).isFalse();
        assertThat(ReadonlyFilter.isAllowed("GET", "/api/v1/merchants")).isFalse();
        assertThat(ReadonlyFilter.isAllowed("GET", "/api/v1/system/alerts")).isFalse();
        assertThat(ReadonlyFilter.isAllowed("GET", "/api/v1/system/config")).isFalse();
        assertThat(ReadonlyFilter.isAllowed("GET", "/api/v1/system/config/notify_channel")).isFalse();
        assertThat(ReadonlyFilter.isAllowed("GET", "/api/v1/auth/api-key")).isFalse();
        assertThat(ReadonlyFilter.isAllowed("POST", "/api/v1/auth/api-key/generate")).isFalse();
        assertThat(ReadonlyFilter.isAllowed("GET", "/api/v1/auth/ingress")).isFalse();
    }

    @Test
    @DisplayName("只读会话允许查自己和退出，不允许改密码")
    void allowsSelfInfoAndLogoutOnly() {
        assertThat(ReadonlyFilter.isAllowed("GET", "/api/v1/auth/info")).isTrue();
        assertThat(ReadonlyFilter.isAllowed("POST", "/api/v1/auth/logout")).isTrue();
        assertThat(ReadonlyFilter.isAllowed("POST", "/api/v1/auth/change-password")).isFalse();
        assertThat(ReadonlyFilter.isAllowed("POST", "/api/v1/auth/api-key/generate")).isFalse();
    }
}