package csit314.renovation.category.control;

import csit314.renovation.authentication.control.AuthenticationService;
import csit314.renovation.category.dto.CategoryResponse;
import csit314.renovation.category.dto.DeleteCategoryResponse;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
    private final InteriorDesignProjectRepository projects;

    public CategoryService(ProjectCategoryRepository categories, AuthenticationService authenticationService,
            InteriorDesignProjectRepository projects) {
        this.categories = categories;
        this.authenticationService = authenticationService;
        this.projects = projects;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categories.findAllByOrderByNameAsc().stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public DeleteCategoryResponse deleteCategory(Long categoryId, Authentication authentication) {
        authenticationService.currentManager(authentication);
        ProjectCategory category = categories.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        long affected = projects.countByCategory_Id(categoryId);
        // The V14 FK performs ON DELETE SET NULL inside the same transaction.
        // No project/image/favourite/view records are removed.
        categories.delete(category);
        categories.flush();
        return new DeleteCategoryResponse(categoryId, category.getName(), affected);
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
    @Transactional
    public CategoryResponse renameCategory(Long id, CreateCategoryRequest request, Authentication authentication) {
        authenticationService.currentManager(authentication);
        ProjectCategory existing=categories.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Category not found"));
        String name=Normalizer.normalize(request.name().strip(),Normalizer.Form.NFKC).strip();
        if(name.isBlank()||name.length()>100) throw new IllegalArgumentException("Category name must contain 1–100 characters");
        if(!existing.getName().equalsIgnoreCase(name) && categories.existsByNameIgnoreCase(name)) throw new DuplicateCategoryException();
        existing.rename(name);
        try { return CategoryResponse.from(categories.saveAndFlush(existing)); }
        catch(DataIntegrityViolationException e){ throw new DuplicateCategoryException(); }
    }

}
