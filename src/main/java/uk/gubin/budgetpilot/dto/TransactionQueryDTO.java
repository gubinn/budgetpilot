package uk.gubin.budgetpilot.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 交易查询条件 DTO
 */
@Data
public class TransactionQueryDTO {
    private Integer type;
    private Long accountId;
    private Long categoryId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String currency;
    private String keyword;
    private List<String> tags;
    private Boolean confirmed;
    /** 扩展字段查询：按 key-value 匹配 */
    private String extKey;
    private String extValue;
    private Integer page = 1;
    private Integer size = 20;
    private String sort = "transaction_date,desc";
}
