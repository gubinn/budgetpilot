package uk.gubin.budgetpilot.vo;

import lombok.Data;
import uk.gubin.budgetpilot.entity.RecurringRule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RecurringRuleVO {
    private Long id;
    private String name;
    private Integer type;
    private BigDecimal amount;
    private String currency;
    private Long accountId;
    private String accountName;
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private Long merchantId;
    private String merchantName;
    private String frequency;
    private Integer executeDay;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate lastExecuted;
    private LocalDate nextExecute;
    private Boolean autoConfirm;
    private Boolean isActive;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RecurringRuleVO fromEntity(RecurringRule entity) {
        if (entity == null) return null;
        RecurringRuleVO vo = new RecurringRuleVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setType(entity.getType());
        vo.setAmount(entity.getAmount());
        vo.setCurrency(entity.getCurrency());
        vo.setAccountId(entity.getAccountId());
        vo.setCategoryId(entity.getCategoryId());
        vo.setMerchantId(entity.getMerchantId());
        vo.setFrequency(entity.getFrequency());
        vo.setExecuteDay(entity.getExecuteDay());
        vo.setStartDate(entity.getStartDate());
        vo.setEndDate(entity.getEndDate());
        vo.setLastExecuted(entity.getLastExecuted());
        vo.setNextExecute(entity.getNextExecute());
        vo.setAutoConfirm(entity.getAutoConfirm());
        vo.setIsActive(entity.getIsActive());
        vo.setNote(entity.getNote());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}