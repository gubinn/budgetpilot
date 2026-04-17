package uk.gubin.budgetpilot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.dto.CategoryCreateDTO;
import uk.gubin.budgetpilot.service.CategoryService;
import uk.gubin.budgetpilot.vo.CategoryVO;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Result<CategoryVO> create(@Valid @RequestBody CategoryCreateDTO dto) {
        return Result.ok(categoryService.create(dto));
    }

    @GetMapping("/tree")
    public Result<List<CategoryVO>> tree(
            @RequestParam(required = false) Integer type) {
        return Result.ok(categoryService.getTree(type));
    }

    @GetMapping("/{id}")
    public Result<CategoryVO> detail(@PathVariable Long id) {
        return Result.ok(categoryService.getById(id));
    }

    @PutMapping("/{id}")
    public Result<CategoryVO> update(@PathVariable Long id, @Valid @RequestBody CategoryCreateDTO dto) {
        return Result.ok(categoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}
