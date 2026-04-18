package uk.gubin.budgetpilot.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import uk.gubin.budgetpilot.entity.User;
import uk.gubin.budgetpilot.mapper.UserMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserMapper userMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void initDefaultAdmin() {
        long count = userMapper.selectCount(null);
        if (count == 0) {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String encoded = encoder.encode("admin123");

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(encoded);
            admin.setNickname("管理员");
            admin.setRole("ADMIN");
            admin.setIsActive(true);
            userMapper.insert(admin);

            log.warn("\n" +
                    "==========================================\n" +
                    "  [初始化] 已创建默认管理员账号\n" +
                    "  用户名: admin\n" +
                    "  密  码: admin123\n" +
                    "  请首次登录后修改密码！\n" +
                    "==========================================");
        }
    }
}
