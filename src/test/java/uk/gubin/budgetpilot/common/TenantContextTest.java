package uk.gubin.budgetpilot.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gubin.budgetpilot.config.AppConfig;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("租户上下文在当前线程可读写并清理")
    void setGetClear() {
        TenantContext.set(42L);
        assertThat(TenantContext.get()).isEqualTo(42L);
        TenantContext.clear();
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    @DisplayName("异步线程池会把提交线程的租户复制到执行线程")
    void asyncExecutorPropagatesTenant() throws Exception {
        Executor executor = new AppConfig().asyncExecutor();
        TenantContext.set(2L);

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Long> seen = new AtomicReference<>();
        executor.execute(() -> {
            seen.set(TenantContext.get());
            done.countDown();
        });

        assertThat(done.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(seen.get()).isEqualTo(2L);
        // 提交线程自身的上下文不受影响
        assertThat(TenantContext.get()).isEqualTo(2L);
    }
}