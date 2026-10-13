package com.chef.chef.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.chef.chef.model.Category;
import com.chef.chef.repository.CategoryRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public Category getCategoryById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Category ID is required");
        }

        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    public Category createCategory(Category category) {
        validateCategory(category);

        String name = category.getName().trim();

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                    "A category with this name already exists");
        }

        category.setId(null);
        category.setName(name);
        category.setDescription(normalizeDescription(category.getDescription()));

        return categoryRepository.save(category);
    }

    public Category updateCategory(String id, Category updatedCategory) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Category ID is required");
        }

        validateCategory(updatedCategory);

        Category existingCategory = getCategoryById(id);
        String name = updatedCategory.getName().trim();

        Optional<Category> duplicate = categoryRepository.findByNameIgnoreCase(name);

        if (duplicate.isPresent()
                && !duplicate.get().getId().equals(id)) {
            throw new IllegalArgumentException(
                    "A category with this name already exists");
        }

        existingCategory.setName(name);
        existingCategory.setDescription(
                normalizeDescription(updatedCategory.getDescription()));

        return categoryRepository.save(existingCategory);
    }

    public void deleteCategory(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Category ID is required");
        }

        Category category = getCategoryById(id);
        categoryRepository.delete(category);
    }

    private void validateCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category data is required");
        }

        if (category.getName() == null
                || category.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Category name is required");
        }
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }
}