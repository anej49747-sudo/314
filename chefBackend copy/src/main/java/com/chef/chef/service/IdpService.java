package com.chef.chef.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.chef.chef.model.Category;
import com.chef.chef.model.IDP;
import com.chef.chef.repository.CategoryRepository;
import com.chef.chef.repository.IdpRepository;

@Service
public class IdpService {

    private final IdpRepository idpRepository;
    private final CategoryRepository categoryRepository;

    public IdpService(
            IdpRepository idpRepository,
            CategoryRepository categoryRepository) {
        this.idpRepository = idpRepository;
        this.categoryRepository = categoryRepository;
    }

    public IDP createIdp(IDP idp) {
        validateIdp(idp);
        validateCategory(idp.getCategoryId());

        LocalDateTime now = LocalDateTime.now();

        idp.setId(null);
        idp.setCreateTime(now);
        idp.setUpdateTime(now);

        return idpRepository.save(idp);
    }

    public List<IDP> getIdpsByDesigner(String designerId) {
        if (designerId == null || designerId.isBlank()) {
            throw new IllegalArgumentException(
                    "Designer ID is required");
        }

        return idpRepository.findByDesignerId(designerId);
    }

    public IDP getIdpById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "IDP ID is required");
        }

        return idpRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("IDP not found"));
    }

    public IDP updateIdp(
            String id, String designerId, IDP updatedIdp) {

        IDP existingIdp = getIdpById(id);

        checkOwnership(existingIdp, designerId);
        validateIdp(updatedIdp);
        validateCategory(updatedIdp.getCategoryId());

        existingIdp.setTitle(updatedIdp.getTitle().trim());
        existingIdp.setDescription(
                updatedIdp.getDescription().trim());
        existingIdp.setImage(
                new ArrayList<>(updatedIdp.getImage()));
        existingIdp.setStatus(
                updatedIdp.getStatus().toLowerCase());
        existingIdp.setCategoryId(updatedIdp.getCategoryId());

        existingIdp.setUpdateTime(LocalDateTime.now());

        return idpRepository.save(existingIdp);
    }

    public void deleteIdp(String id, String designerId) {
        IDP existingIdp = getIdpById(id);

        checkOwnership(existingIdp, designerId);

        idpRepository.delete(existingIdp);
    }

    public List<IDP> searchIdps(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return idpRepository.findAll();
        }

        String searchTerm = keyword.trim();

        return idpRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        searchTerm,
                        searchTerm);
    }

    private void validateCategory(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException(
                    "Please select a category");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Selected category does not exist"));

        if (category.getId() == null) {
            throw new IllegalArgumentException(
                    "Selected category is invalid");
        }
    }

    private void checkOwnership(IDP idp, String designerId) {
        if (designerId == null
                || designerId.isBlank()
                || !Objects.equals(
                        idp.getDesignerId(), designerId)) {
            throw new IllegalArgumentException(
                    "You are not allowed to manage this IDP");
        }
    }

    private void validateIdp(IDP idp) {
        if (idp == null) {
            throw new IllegalArgumentException(
                    "IDP data is required");
        }

        if (idp.getDesignerId() == null
                || idp.getDesignerId().isBlank()) {
            throw new IllegalArgumentException(
                    "Designer ID is required");
        }

        if (idp.getTitle() == null
                || idp.getTitle().isBlank()) {
            throw new IllegalArgumentException(
                    "Project title is required");
        }

        if (idp.getDescription() == null
                || idp.getDescription().isBlank()) {
            throw new IllegalArgumentException(
                    "Project description is required");
        }

        if (idp.getStatus() == null
                || (!idp.getStatus().equalsIgnoreCase("current")
                        && !idp.getStatus().equalsIgnoreCase("completed"))) {
            throw new IllegalArgumentException(
                    "Status must be current or completed");
        }

        if (idp.getImage() == null) {
            idp.setImage(new ArrayList<>());
        }
    }
}