package com.chef.chef.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.chef.chef.model.User;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    List<User> findByRole(String role);

    boolean existsByUsername(String username);

    List<User> findByRoleAndUsernameContainingIgnoreCase(
            String role,
            String username);

    @Query("{ 'role': ?0, 'createTime': { '$gte': ?1, '$lt': ?2 } }")
    long countCustomersCreatedBetween(
            String role,
            LocalDateTime start,
            LocalDateTime end);
}