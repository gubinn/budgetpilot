package uk.gubin.budgetpilot.service;

import uk.gubin.budgetpilot.dto.ReadonlyShareCreateDTO;
import uk.gubin.budgetpilot.vo.ReadonlySessionVO;
import uk.gubin.budgetpilot.vo.ReadonlyShareCreatedVO;
import uk.gubin.budgetpilot.vo.ReadonlyShareVO;
import uk.gubin.budgetpilot.vo.ShareCheckVO;

import java.util.List;

/**
 * 只读共享账本链接。
 */
public interface ReadonlyShareService {

    List<ReadonlyShareVO> list();

    ReadonlyShareCreatedVO create(ReadonlyShareCreateDTO dto);

    void revoke(Long id);

    ShareCheckVO check(String code);

    ReadonlySessionVO verify(String code, String pin, String clientIp);
}