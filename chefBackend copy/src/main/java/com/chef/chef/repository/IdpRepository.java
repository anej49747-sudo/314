package com.chef.chef.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.chef.chef.model.IDP;

public interface IdpRepository extends MongoRepository<IDP, String> {

    List<IDP> findByDesignerId(String designerId);

    List<IDP> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title,
            String description);

    List<IDP> findByCategoryId(String categoryId);

    long countByCategoryId(String categoryId);

    @Query("{ 'createTime': { '$gte': ?0, '$lt': ?1 } }")
    long countIdpsCreatedBetween(
            LocalDateTime start,
            LocalDateTime end);
}