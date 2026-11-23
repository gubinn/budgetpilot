package uk.gubin.budgetpilot.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 净资产月度快照：某个月末每个账户的余额，以及相对上个月的变化。
 */
@Data
public class NetWorthSnapshotVO {

    private String month;
    private String label;
    private boolean currentMonth;
    private BigDecimal netWorth = BigDecimal.ZERO;
    private BigDecimal prevNetWorth = BigDecimal.ZERO;
    private BigDecimal change = BigDecimal.ZERO;
    private List<MonthOption> months;
    private List<Row> accounts;

    @Data
    public static class MonthOption {
        private String month;
        private String label;
        private BigDecimal netWorth = BigDecimal.ZERO;
        private boolean currentMonth;
    }

    @Data
    public static class Row {
        private Long accountId;
        private String name;
        private String type;
        private String currency;
        private BigDecimal balance = BigDecimal.ZERO;
        private BigDecimal prevBalance = BigDecimal.ZERO;
        private BigDecimal change = BigDecimal.ZERO;
    }
}