package com.chef.chef.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chef.chef.model.Category;
import com.chef.chef.service.CategoryService;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<?> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(
            @PathVariable String id) {
        try {
            return ResponseEntity.ok(
                    categoryService.getCategoryById(id));
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    public ResponseEntity<?> createCategory(
            @RequestBody Category category) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(categoryService.createCategory(category));
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable String id,
            @RequestBody Category category) {
        try {
            return ResponseEntity.ok(
                    categoryService.updateCategory(id, category));
        } catch (IllegalArgumentException e) {
            HttpStatus status = "Category not found".equals(e.getMessage())
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;

            return errorResponse(e.getMessage(), status);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(
            @PathVariable String id) {
        try {
            categoryService.deleteCategory(id);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "Category deleted successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    private ResponseEntity<Map<String, String>> errorResponse(
            String message,
            HttpStatus status) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("message", message);

        return ResponseEntity.status(status).body(response);
    }
}