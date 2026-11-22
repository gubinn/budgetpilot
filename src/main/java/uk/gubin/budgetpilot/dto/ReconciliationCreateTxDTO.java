package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 账单有、流水无时手工补录交易。
 * 普通收支必须选择账户和分类；转账需要同时选择转出/转入账户，不需要分类。
 * 历史漏记的转账默认不计入余额，避免再次影响已经与银行对齐的当前余额。
 */
@Data
public class ReconciliationCreateTxDTO {
    @NotNull(message = "请选择入账账户")
    private Long accountId;

    /** 普通收支必填；转账不需要 */
    private Long categoryId;

    /** 转账的对方账户：转入账单填转出账户，转出账单填转入账户 */
    private Long targetAccountId;

    /**
     * 可选，覆盖按摘要推断的交易类型。
     * 「转账汇款」既可能是账户间转账，也可能是付给外部个人/公司的费用，前端可让用户改选支出/收入。
     */
    private Integer type;

    private Boolean confirmed;
}