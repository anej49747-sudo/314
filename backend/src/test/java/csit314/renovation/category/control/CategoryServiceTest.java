package csit314.renovation.category.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import csit314.renovation.authentication.control.AuthenticationService;
import csit314.renovation.category.dto.CreateCategoryRequest;
import csit314.renovation.category.entity.ProjectCategory;
import csit314.renovation.category.repository.ProjectCategoryRepository;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock ProjectCategoryRepository repository;
    @Mock InteriorDesignProjectRepository projects;
    @Mock AuthenticationService authenticationService;
    @Mock Authentication authentication;
    @InjectMocks CategoryService service;

    @Test
    void storesNormalizedNameWithoutDescription() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.createCategory(new CreateCategoryRequest("  Ｍodern  "), authentication);
        ArgumentCaptor<ProjectCategory> saved = ArgumentCaptor.forClass(ProjectCategory.class);
        verify(repository).saveAndFlush(saved.capture());
        assertEquals("Modern", saved.getValue().getName());
        verify(authenticationService).currentManager(authentication);
    }

    @Test
    void rejectsDuplicateWithoutSaving() {
        when(repository.existsByNameIgnoreCase("Modern")).thenReturn(true);
        assertThrows(DuplicateCategoryException.class,
                () -> service.createCategory(new CreateCategoryRequest(" Modern "), authentication));
        verify(repository, never()).saveAndFlush(any());
    }
}
