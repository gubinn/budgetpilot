package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import uk.gubin.budgetpilot.dto.AccountCreateDTO;
import uk.gubin.budgetpilot.dto.AccountUpdateDTO;
import uk.gubin.budgetpilot.entity.Account;
import uk.gubin.budgetpilot.vo.AccountVO;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService extends IService<Account> {
    AccountVO create(AccountCreateDTO dto);
    List<AccountVO> listActive(Boolean activeOnly);
    AccountVO getById(Long id);
    AccountVO update(Long id, AccountUpdateDTO dto);
    void delete(Long id);
    void adjustBalance(Long accountId, BigDecimal amount);
    void adjustBalanceWithRecord(Long accountId, BigDecimal newBalance, String reason);
}
