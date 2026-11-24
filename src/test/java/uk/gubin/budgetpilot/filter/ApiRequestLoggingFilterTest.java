package uk.gubin.budgetpilot.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ApiRequestLoggingFilterTest {

    @Test
    @DisplayName("写请求记录方法、路径、状态码和耗时")
    void logsWriteRequest() throws Exception {
        ApiRequestLoggingFilter filter = new ApiRequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transactions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        ListAppender<ILoggingEvent> appender = attach();
        try {
            filter.doFilter(request, response, (req, res) -> {
            });
        } finally {
            detach(appender);
        }

        assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage())
                    .contains("API POST /api/v1/transactions")
                    .contains("-> 200")
                    .contains("user=-")
                    .contains("share=-");
        });
    }

    @Test
    @DisplayName("HTTP 错误请求记录 WARN，包含状态码")
    void logsHttpError() throws Exception {
        ApiRequestLoggingFilter filter = new ApiRequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/transactions/123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        ListAppender<ILoggingEvent> appender = attach();
        try {
            filter.doFilter(request, response, (req, res) -> {
                ((MockHttpServletResponse) res).setStatus(403);
            });
        } finally {
            detach(appender);
        }

        assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage())
                    .contains("API PUT /api/v1/transactions/123")
                    .contains("-> 403");
        });
    }

    private ListAppender<ILoggingEvent> attach() {
        Logger logger = (Logger) LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detach(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
        logger.detachAppender(appender);
    }
}