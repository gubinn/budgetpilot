package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_account_balance_snapshot")
public class AccountBalanceSnapshot extends BaseEntity {
    private Long userId;
    private Long accountId;
    /** YYYY-MM，表示该月月末余额 */
    private String snapshotMonth;
    private BigDecimal balance;
}