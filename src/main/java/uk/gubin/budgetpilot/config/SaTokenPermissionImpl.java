package uk.gubin.budgetpilot.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gubin.budgetpilot.entity.User;
import uk.gubin.budgetpilot.mapper.UserMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SaTokenPermissionImpl implements StpInterface {

    private final UserMapper userMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        try {
            Long userId = Long.parseLong(loginId.toString());
            User user = userMapper.selectById(userId);
            if (user != null && user.getIsActive()) {
                return List.of(user.getRole());
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }
}
