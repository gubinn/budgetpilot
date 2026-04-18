package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user_config")
public class UserConfig extends BaseEntity {
    private Long userId;
    private String configKey;
    private String configValue;
}
