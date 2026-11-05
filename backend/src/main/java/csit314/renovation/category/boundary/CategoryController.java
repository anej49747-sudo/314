package csit314.renovation.category.boundary;

import csit314.renovation.category.control.CategoryService;
import csit314.renovation.category.dto.CategoryResponse;
import csit314.renovation.category.dto.DeleteCategoryResponse;
import csit314.renovation.category.dto.CreateCategoryRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.listCategories();
    }

    @DeleteMapping("/{categoryId}")
    public DeleteCategoryResponse delete(@PathVariable Long categoryId, Authentication authentication) {
        return categoryService.deleteCategory(categoryId, authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CreateCategoryRequest request, Authentication authentication) {
        return categoryService.createCategory(request, authentication);
    }
    @PutMapping("/{categoryId}")
    public CategoryResponse rename(@PathVariable Long categoryId, @Valid @RequestBody CreateCategoryRequest request, Authentication authentication){
        return categoryService.renameCategory(categoryId,request,authentication);
    }

}
