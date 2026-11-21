package uk.gubin.budgetpilot.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 商户交易统计（用于列表展示和合并时挑选主商户）。
 */
@Data
public class MerchantUsageStatDTO {

    private Long merchantId;

    private Long txCount;

    private LocalDate lastDate;
}