package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import uk.gubin.budgetpilot.common.PageResult;
import uk.gubin.budgetpilot.dto.MerchantCreateDTO;
import uk.gubin.budgetpilot.dto.MerchantQueryDTO;
import uk.gubin.budgetpilot.dto.MerchantUpdateDTO;
import uk.gubin.budgetpilot.entity.Merchant;
import uk.gubin.budgetpilot.vo.MerchantVO;

import java.util.List;
import java.util.Map;

/**
 * 商户服务接口
 */
public interface MerchantService extends IService<Merchant> {

    /**
     * 创建商户
     */
    MerchantVO create(MerchantCreateDTO dto);

    /**
     * 获取商户详情
     */
    MerchantVO getById(Long id);

    /**
     * 更新商户
     */
    MerchantVO update(Long id, MerchantUpdateDTO dto);

    /**
     * 删除商户
     */
    void delete(Long id);

    /**
     * 查询商户列表（保持旧接口：返回当前用户全部匹配商户，供采集端学习映射使用）
     */
    List<MerchantVO> query(MerchantQueryDTO dto);

    /**
     * 分页查询商户列表（管理页面使用）
     */
    PageResult<MerchantVO> queryPage(MerchantQueryDTO dto);

    /**
     * 合并重复商户：按归一化名称聚合，原始名称写入别名表。
     *
     * @return 合并统计
     */
    Map<String, Object> consolidate();

    /**
     * 商户模糊搜索（用于交易表单）
     * @param keyword 搜索关键字
     * @param limit 返回数量限制
     * @return 匹配的商户列表
     */
    List<MerchantVO> searchByName(String keyword, Integer limit);

    /**
     * 根据名称查找或创建商户
     * @param name 商户名称
     * @param categoryId 关联分类ID（可选）
     * @param autoCreate 是否自动创建新商户
     * @return 商户 VO
     */
    MerchantVO findOrCreate(String name, Long categoryId, boolean autoCreate);

    /**
     * 更新商户使用统计
     */
    void incrementUsage(Long merchantId);

    /**
     * 检查是否有关联交易
     */
    boolean hasTransactions(Long merchantId);
}