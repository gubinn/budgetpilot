package uk.gubin.budgetpilot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.service.ReceivableService;
import uk.gubin.budgetpilot.vo.ReceivableOverviewVO;
import uk.gubin.budgetpilot.vo.ReceivableSummaryVO;

/**
 * 往来款（应收/应付）只读接口。
 */
@RestController
@RequestMapping("/api/v1/receivables")
@RequiredArgsConstructor
public class ReceivableController {

    private final ReceivableService receivableService;

    @GetMapping("/overview")
    public Result<ReceivableOverviewVO> overview(
            @RequestParam(defaultValue = "false") boolean includeUnconfirmed) {
        return Result.ok(receivableService.overview(includeUnconfirmed));
    }

    @GetMapping("/summary")
    public Result<ReceivableSummaryVO> summary() {
        return Result.ok(receivableService.summary());
    }
}