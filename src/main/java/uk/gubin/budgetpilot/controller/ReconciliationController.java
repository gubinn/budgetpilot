package uk.gubin.budgetpilot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uk.gubin.budgetpilot.common.PageResult;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.dto.ReconciliationCreateTxDTO;
import uk.gubin.budgetpilot.dto.ReconciliationIgnoreDTO;
import uk.gubin.budgetpilot.dto.ReconciliationMatchDTO;
import uk.gubin.budgetpilot.service.ReconciliationService;
import uk.gubin.budgetpilot.vo.MatchedTransactionVO;
import uk.gubin.budgetpilot.vo.ReconciliationBatchVO;
import uk.gubin.budgetpilot.vo.ReconciliationImportVO;
import uk.gubin.budgetpilot.vo.ReconciliationItemVO;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ReconciliationImportVO> importBill(
            @RequestPart("file") MultipartFile file,
            @RequestParam("channel") String channel,
            @RequestParam(value = "accountId", required = false) Long accountId) {
        return Result.ok(reconciliationService.importBill(file, channel, accountId));
    }

    @GetMapping("/batches")
    public Result<List<ReconciliationBatchVO>> batches() {
        return Result.ok(reconciliationService.listBatches());
    }

    @GetMapping("/batches/{id}")
    public Result<ReconciliationBatchVO> batch(@PathVariable Long id) {
        return Result.ok(reconciliationService.getBatch(id));
    }

    @GetMapping("/batches/{id}/items")
    public Result<PageResult<ReconciliationItemVO>> items(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(reconciliationService.listItems(id, status, page, size));
    }

    @GetMapping("/batches/{id}/transactions")
    public Result<List<MatchedTransactionVO>> unmatchedTransactions(@PathVariable Long id) {
        return Result.ok(reconciliationService.listUnmatchedTransactions(id));
    }

    @PostMapping("/batches/{id}/rematch")
    public Result<ReconciliationBatchVO> rematch(@PathVariable Long id) {
        return Result.ok(reconciliationService.rematch(id));
    }

    @DeleteMapping("/batches/{id}")
    public Result<Void> deleteBatch(@PathVariable Long id) {
        reconciliationService.deleteBatch(id);
        return Result.ok();
    }

    @GetMapping("/items/{id}/candidates")
    public Result<List<MatchedTransactionVO>> candidates(@PathVariable Long id) {
        return Result.ok(reconciliationService.listCandidates(id));
    }

    @PostMapping("/items/{id}/match")
    public Result<ReconciliationItemVO> match(@PathVariable Long id,
                                              @Valid @RequestBody ReconciliationMatchDTO dto) {
        return Result.ok(reconciliationService.match(id, dto.getTransactionId()));
    }

    @PostMapping("/items/{id}/unmatch")
    public Result<ReconciliationItemVO> unmatch(@PathVariable Long id) {
        return Result.ok(reconciliationService.unmatch(id));
    }

    @PostMapping("/items/{id}/ignore")
    public Result<ReconciliationItemVO> ignore(@PathVariable Long id,
                                               @RequestBody(required = false) ReconciliationIgnoreDTO dto) {
        return Result.ok(reconciliationService.ignore(id, dto == null ? null : dto.getReason()));
    }

    @PostMapping("/items/{id}/create-transaction")
    public Result<ReconciliationItemVO> createTransaction(
            @PathVariable Long id, @Valid @RequestBody ReconciliationCreateTxDTO dto) {
        return Result.ok(reconciliationService.createTransaction(id, dto));
    }
}