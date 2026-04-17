package uk.gubin.budgetpilot;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * 测试用 Spring Boot 配置 —— 不启动真实 DB/Redis，
 * 所有 Mapper 由 Mockito mock，避免集成测试依赖。
 */
@SpringBootTest(
        classes = BudgetPilotApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/test",
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        "spring.data.redis.host=localhost",
        "sa-token.token-name=Authorization"
})
public class BaseTest {
}
