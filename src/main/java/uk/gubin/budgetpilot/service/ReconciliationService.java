package uk.gubin.budgetpilot.service;

import org.springframework.web.multipart.MultipartFile;
import uk.gubin.budgetpilot.common.PageResult;
import uk.gubin.budgetpilot.dto.ReconciliationCreateTxDTO;
import uk.gubin.budgetpilot.vo.MatchedTransactionVO;
import uk.gubin.budgetpilot.vo.ReconciliationBatchVO;
import uk.gubin.budgetpilot.vo.ReconciliationImportVO;
import uk.gubin.budgetpilot.vo.ReconciliationItemVO;

import java.util.List;

public interface ReconciliationService {
    ReconciliationImportVO importBill(MultipartFile file, String channel, Long accountId);

    List<ReconciliationBatchVO> listBatches();

    ReconciliationBatchVO getBatch(Long batchId);

    PageResult<ReconciliationItemVO> listItems(Long batchId, String status, int page, int size);

    List<MatchedTransactionVO> listUnmatchedTransactions(Long batchId);

    List<MatchedTransactionVO> listCandidates(Long itemId);

    ReconciliationItemVO match(Long itemId, Long transactionId);

    ReconciliationItemVO unmatch(Long itemId);

    ReconciliationItemVO ignore(Long itemId, String reason);

    ReconciliationItemVO createTransaction(Long itemId, ReconciliationCreateTxDTO dto);

    ReconciliationBatchVO rematch(Long batchId);

    void deleteBatch(Long batchId);
}