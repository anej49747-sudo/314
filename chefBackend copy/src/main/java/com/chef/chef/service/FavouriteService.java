package com.chef.chef.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.chef.chef.model.Favourite;
import com.chef.chef.model.IDP;
import com.chef.chef.repository.FavouriteRepository;
import com.chef.chef.repository.IdpRepository;

@Service
public class FavouriteService {

    private final FavouriteRepository favouriteRepository;
    private final IdpRepository idpRepository;

    public FavouriteService(
            FavouriteRepository favouriteRepository,
            IdpRepository idpRepository) {
        this.favouriteRepository = favouriteRepository;
        this.idpRepository = idpRepository;
    }

    public Favourite addFavourite(String customerId, String idpId) {
        validateIds(customerId, idpId);

        if (!idpRepository.existsById(idpId)) {
            throw new IllegalArgumentException("IDP not found");
        }

        if (favouriteRepository.existsByCustomerIdAndIdpId(
                customerId, idpId)) {
            throw new IllegalArgumentException(
                    "IDP is already in favourites");
        }

        Favourite favourite = new Favourite();
        favourite.setCustomerId(customerId);
        favourite.setIdpId(idpId);
        favourite.setCreateTime(LocalDateTime.now());

        return favouriteRepository.save(favourite);
    }

    public List<IDP> getFavourites(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException(
                    "Customer ID is required");
        }

        List<Favourite> favourites = favouriteRepository.findByCustomerId(customerId);

        return favourites.stream()
                .map(favourite -> idpRepository
                        .findById(favourite.getIdpId())
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    public void removeFavourite(String customerId, String idpId) {
        validateIds(customerId, idpId);

        Optional<Favourite> favourite = favouriteRepository.findByCustomerIdAndIdpId(
                customerId, idpId);

        if (favourite.isEmpty()) {
            throw new IllegalArgumentException(
                    "Favourite not found");
        }

        favouriteRepository.delete(favourite.get());
    }

    public long getFavouriteCount(String idpId) {
        if (idpId == null || idpId.isBlank()) {
            throw new IllegalArgumentException("IDP ID is required");
        }

        return favouriteRepository.countByIdpId(idpId);
    }

    public List<IDP> getPopularIdps(String designerId) {
        if (designerId == null || designerId.isBlank()) {
            throw new IllegalArgumentException(
                    "Designer ID is required");
        }

        return idpRepository.findByDesignerId(designerId)
                .stream()
                .sorted(
                        Comparator.comparingLong(
                                (IDP idp) -> getFavouriteCount(idp.getId()))
                                .reversed())
                .toList();
    }

    private void validateIds(String customerId, String idpId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException(
                    "Customer ID is required");
        }

        if (idpId == null || idpId.isBlank()) {
            throw new IllegalArgumentException(
                    "IDP ID is required");
        }
    }
}