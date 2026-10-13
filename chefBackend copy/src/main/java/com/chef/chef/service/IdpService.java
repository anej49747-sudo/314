package com.chef.chef.service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.chef.chef.model.IDP;
import com.chef.chef.repository.IdpRepository;

@Service
public class IdpService {

    private final IdpRepository idpRepository;

    public IdpService(IdpRepository idpRepository) {
        this.idpRepository = idpRepository;
    }

    public IDP createIdp(IDP idp) {
        validateIdp(idp);

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

        existingIdp.setTitle(updatedIdp.getTitle().trim());
        existingIdp.setDescription(
                updatedIdp.getDescription().trim());
        existingIdp.setImage(
                new ArrayList<>(updatedIdp.getImage()));
        existingIdp.setStatus(
                updatedIdp.getStatus().toLowerCase());

        existingIdp.setUpdateTime(LocalDateTime.now());

        return idpRepository.save(existingIdp);
    }

    public void deleteIdp(String id, String designerId) {
        IDP existingIdp = getIdpById(id);

        checkOwnership(existingIdp, designerId);

        idpRepository.delete(existingIdp);
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

        for (String imageUrl : idp.getImage()) {
            if (imageUrl == null || imageUrl.isBlank()) {
                throw new IllegalArgumentException(
                        "Image URL cannot be empty");
            }

            try {
                URI uri = URI.create(imageUrl.trim());

                if (uri.getHost() == null
                        || (!"http".equalsIgnoreCase(uri.getScheme())
                                && !"https".equalsIgnoreCase(uri.getScheme()))) {
                    throw new IllegalArgumentException(
                            "Image URL must use HTTP or HTTPS");
                }
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Invalid image URL: " + imageUrl);
            }
        }
    }
}