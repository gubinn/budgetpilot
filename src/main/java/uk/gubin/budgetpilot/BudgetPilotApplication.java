package uk.gubin.budgetpilot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan("uk.gubin.budgetpilot.mapper")
public class BudgetPilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(BudgetPilotApplication.class, args);
    }
}
