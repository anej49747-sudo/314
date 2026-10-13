package com.chef.chef.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chef.chef.model.Favourite;

public interface FavouriteRepository extends MongoRepository<Favourite, String> {

    List<Favourite> findByCustomerId(String customerId);

    Optional<Favourite> findByCustomerIdAndIdpId(
            String customerId,
            String idpId);

    boolean existsByCustomerIdAndIdpId(
            String customerId,
            String idpId);

    void deleteByCustomerIdAndIdpId(
            String customerId,
            String idpId);

    long countByIdpId(String idpId);

    long countByCreateTimeGreaterThanEqualAndCreateTimeLessThan(
            LocalDateTime start,
            LocalDateTime end);
}