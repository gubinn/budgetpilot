package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_account")
public class Account extends BaseEntity {
    private String name;
    private Integer type;
    private String icon;
    private String currency;
    private java.math.BigDecimal initialBalance;
    private java.math.BigDecimal currentBalance;
    private java.math.BigDecimal creditLimit;
    private Integer billingDay;
    private Integer paymentDay;
    private Integer sortOrder;
    private Boolean isActive;
    @TableField("ext_fields")
    private String extFields;
    private String metadata;
}
