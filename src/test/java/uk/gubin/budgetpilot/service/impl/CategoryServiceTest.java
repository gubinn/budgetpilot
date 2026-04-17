package uk.gubin.budgetpilot.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.dto.CategoryCreateDTO;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.vo.CategoryVO;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryMapper categoryMapper;
    @Mock private TransactionMapper transactionMapper;
    @InjectMocks private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        doAnswer(inv -> {
            Category cat = inv.getArgument(0);
            cat.setId(1L);
            return 1;
        }).when(categoryMapper).insert(any(Category.class));
    }

    // ============ CREATE ============

    @Nested
    @DisplayName("创建分类")
    class CreateTests {

        @Test
        @DisplayName("正常创建一级分类")
        void create_RootCategory() {
            CategoryCreateDTO dto = buildDTO("餐饮", 1);
            dto.setParentId(0L);

            CategoryVO vo = categoryService.create(dto);

            assertThat(vo.getName()).isEqualTo("餐饮");
            assertThat(vo.getType()).isEqualTo(1);
            assertThat(vo.getIsSystem()).isFalse();
        }

        @Test
        @DisplayName("正常创建子分类")
        void create_SubCategory() {
            CategoryCreateDTO dto = buildDTO("外卖", 1);
            dto.setParentId(10L);

            categoryService.create(dto);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryMapper).insert(captor.capture());
            assertThat(captor.getValue().getParentId()).isEqualTo(10L);
        }
    }

    // ============ TREE + CIRCULAR DETECTION ============

    @Nested
    @DisplayName("分类树构建")
    class TreeTests {

        @Test
        @DisplayName("正常构建两级分类树")
        void getTree_NormalTwoLevel() {
            Category root = cat(1L, 0L, "餐饮", 1);
            Category child = cat(2L, 1L, "外卖", 1);
            when(categoryMapper.selectList(any())).thenReturn(List.of(root, child));

            List<CategoryVO> tree = categoryService.getTree(1);

            assertThat(tree).hasSize(1);
            assertThat(tree.get(0).getChildren()).hasSize(1);
            assertThat(tree.get(0).getChildren().get(0).getName()).isEqualTo("外卖");
        }

        @Test
        @DisplayName("检测循环引用 A→B→A 并抛异常")
        void getTree_DetectCircular_AtoBtoA() {
            Category a = cat(1L, 2L, "A", 1);
            Category b = cat(2L, 1L, "B", 1);
            when(categoryMapper.selectList(any())).thenReturn(List.of(a, b));

            assertThatThrownBy(() -> categoryService.getTree(1))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("循环引用");
        }

        @Test
        @DisplayName("检测循环引用 A→B→C→A 并抛异常")
        void getTree_DetectCircular_AtoBtoCtoA() {
            Category a = cat(1L, 3L, "A", 1);
            Category b = cat(2L, 1L, "B", 1);
            Category c = cat(3L, 2L, "C", 1);
            when(categoryMapper.selectList(any())).thenReturn(List.of(a, b, c));

            assertThatThrownBy(() -> categoryService.getTree(1))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("循环引用");
        }

        @Test
        @DisplayName("正常自引用不视为循环（parentId=0）")
        void getTree_SelfReference_Root() {
            Category root = cat(1L, 0L, "根", 1);
            when(categoryMapper.selectList(any())).thenReturn(List.of(root));

            List<CategoryVO> tree = categoryService.getTree(1);

            assertThat(tree).hasSize(1);
        }
    }

    // ============ DELETE ============

    @Nested
    @DisplayName("删除分类")
    class DeleteTests {

        @Test
        @DisplayName("系统分类拒绝删除")
        void delete_SystemCategory() {
            Category cat = cat(1L, 0L, "餐饮", 1);
            cat.setIsSystem(true);
            when(categoryMapper.selectById(1L)).thenReturn(cat);

            assertThatThrownBy(() -> categoryService.delete(1L))
                    .isInstanceOf(BizException.class);
        }

        @Test
        @DisplayName("有子分类拒绝删除")
        void delete_HasChildren() {
            Category cat = cat(1L, 0L, "餐饮", 1);
            cat.setIsSystem(false);
            when(categoryMapper.selectById(1L)).thenReturn(cat);
            when(categoryMapper.selectCount(any())).thenReturn(1L);

            assertThatThrownBy(() -> categoryService.delete(1L))
                    .isInstanceOf(BizException.class);
        }

        @Test
        @DisplayName("有交易记录拒绝删除")
        void delete_HasTransactions() {
            Category cat = cat(1L, 0L, "餐饮", 1);
            cat.setIsSystem(false);
            when(categoryMapper.selectById(1L)).thenReturn(cat);
            // hasChildren → 0, hasTransactions → 1
            when(categoryMapper.selectCount(any()))
                    .thenReturn(0L)  // hasChildren
                    .thenReturn(1L); // hasTransactions
            when(transactionMapper.selectCount(any())).thenReturn(5L);

            assertThatThrownBy(() -> categoryService.delete(1L))
                    .isInstanceOf(BizException.class);
        }

        @Test
        @DisplayName("干净分类正常逻辑删除")
        void delete_CleanCategory() {
            Category cat = cat(1L, 0L, "餐饮", 1);
            cat.setIsSystem(false);
            when(categoryMapper.selectById(1L)).thenReturn(cat);
            when(categoryMapper.selectCount(any())).thenReturn(0L);
            when(transactionMapper.selectCount(any())).thenReturn(0L);

            categoryService.delete(1L);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryMapper).updateById(captor.capture());
            assertThat(captor.getValue().getIsActive()).isFalse();
        }
    }

    // ============ HELPERS ============

    private CategoryCreateDTO buildDTO(String name, Integer type) {
        CategoryCreateDTO dto = new CategoryCreateDTO();
        dto.setName(name);
        dto.setType(type);
        return dto;
    }

    private Category cat(Long id, Long parentId, String name, Integer type) {
        Category c = new Category();
        c.setId(id);
        c.setParentId(parentId);
        c.setName(name);
        c.setType(type);
        c.setIsActive(true);
        c.setIsSystem(false);
        return c;
    }
}
