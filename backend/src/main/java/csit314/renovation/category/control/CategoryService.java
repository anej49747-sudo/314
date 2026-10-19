package csit314.renovation.category.control;

import csit314.renovation.authentication.control.AuthenticationService;
import csit314.renovation.category.dto.CategoryResponse;
import csit314.renovation.category.dto.CreateCategoryRequest;
import csit314.renovation.category.entity.ProjectCategory;
import csit314.renovation.category.repository.ProjectCategoryRepository;
import java.text.Normalizer;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final ProjectCategoryRepository categories;
    private final AuthenticationService authenticationService;

    public CategoryService(ProjectCategoryRepository categories, AuthenticationService authenticationService) {
        this.categories = categories;
        this.authenticationService = authenticationService;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categories.findAllByOrderByNameAsc().stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request, Authentication authentication) {
        // Recheck the persisted role/status as well as Spring Security's session authorities.
        authenticationService.currentManager(authentication);
        String name = Normalizer.normalize(request.name().strip(), Normalizer.Form.NFKC).strip();
        if (name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("Category name must contain 1–100 characters");
        }
        if (categories.existsByNameIgnoreCase(name)) {
            throw new DuplicateCategoryException();
        }
        try {
            return CategoryResponse.from(categories.saveAndFlush(new ProjectCategory(name)));
        } catch (DataIntegrityViolationException duplicate) {
            // The unique index is the final guard against simultaneous submissions.
            throw new DuplicateCategoryException();
        }
    }
}
