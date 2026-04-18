package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import uk.gubin.budgetpilot.dto.CategoryCreateDTO;
import uk.gubin.budgetpilot.dto.CategoryUpdateDTO;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.vo.CategoryVO;

import java.util.List;

public interface CategoryService extends IService<Category> {
    CategoryVO create(CategoryCreateDTO dto);
    List<CategoryVO> getTree(Integer type);
    CategoryVO getById(Long id);
    CategoryVO update(Long id, CategoryUpdateDTO dto);
    void delete(Long id);
    List<Category> getChildren(Long parentId);
    boolean hasChildren(Long id);
    boolean hasTransactions(Long id);
}
