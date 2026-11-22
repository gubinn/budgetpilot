package uk.gubin.budgetpilot.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gubin.budgetpilot.entity.Merchant;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.MerchantAliasMapper;
import uk.gubin.budgetpilot.mapper.RecurringRuleMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.common.MerchantNameNormalizer;
import uk.gubin.budgetpilot.vo.MerchantVO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MerchantServiceImplTest {

    @Test
    void convertsMerchantWithoutCategory() {
        MerchantAliasMapper aliasMapper = mock(MerchantAliasMapper.class);
        TransactionMapper transactionMapper = mock(TransactionMapper.class);
        when(aliasMapper.selectList(any())).thenReturn(List.of());
        when(transactionMapper.countByMerchantIds(any())).thenReturn(List.of());

        MerchantServiceImpl service = new MerchantServiceImpl(
                new ObjectMapper(),
                mock(CategoryMapper.class),
                transactionMapper,
                aliasMapper,
                mock(RecurringRuleMapper.class),
                mock(MerchantNameNormalizer.class));

        Merchant merchant = new Merchant();
        merchant.setId(1L);
        merchant.setName("示例商户");
        merchant.setCategoryId(null);
        merchant.setIsActive(true);

        @SuppressWarnings("unchecked")
        List<MerchantVO> result = (List<MerchantVO>) ReflectionTestUtils.invokeMethod(
                service, "toVOList", List.of(merchant));

        assertThat(result).singleElement().satisfies(vo -> {
            assertThat(vo.getName()).isEqualTo("示例商户");
            assertThat(vo.getCategoryId()).isNull();
        });
    }
}