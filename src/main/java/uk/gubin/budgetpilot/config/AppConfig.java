package uk.gubin.budgetpilot.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AppConfig implements MetaObjectHandler {

    /**
     * 需要忽略租户过滤的全局表
     */
    private static final java.util.Set<String> IGNORE_TENANT_TABLES = java.util.Set.of(
            "t_user", "t_user_config", "t_currency_rate", "t_config"
    );

    /**
     * MyBatis-Plus 分页 + 租户隔离插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 用户数据隔离：自动为 SQL 注入 user_id 条件
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor(
                new TenantLineHandler() {
                    @Override
                    public Expression getTenantId() {
                        try {
                            long userId = StpUtil.getLoginIdAsLong();
                            return new LongValue(userId);
                        } catch (Exception e) {
                            // 未登录时不注入 user_id 条件（由 SaInterceptor 拦截请求）
                            return null;
                        }
                    }

                    @Override
                    public String getTenantIdColumn() {
                        return "user_id";
                    }

                    @Override
                    public boolean ignoreTable(String tableName) {
                        return IGNORE_TENANT_TABLES.contains(tableName);
                    }
                });

        interceptor.addInnerInterceptor(tenantInterceptor);
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(200L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }

    /**
     * 自动填充 createdAt / updatedAt / userId
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        // 自动填充当前用户的 userId（仅对存在 userId 字段的实体生效）
        if (metaObject.hasSetter("userId")) {
            try {
                this.strictInsertFill(metaObject, "userId", Long.class, StpUtil.getLoginIdAsLong());
            } catch (Exception e) {
                // 未登录时不填充（由 SaInterceptor 拦截请求，或 DataInitializer 等内部调用）
            }
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 密码加密器
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 异步任务线程池
     */
    @Bean("asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("budget-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
