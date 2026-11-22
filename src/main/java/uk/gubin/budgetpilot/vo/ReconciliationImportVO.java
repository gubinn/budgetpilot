package uk.gubin.budgetpilot.vo;

import lombok.Data;

@Data
public class ReconciliationImportVO {
    private boolean duplicate;
    private String message;
    private ReconciliationBatchVO batch;
}