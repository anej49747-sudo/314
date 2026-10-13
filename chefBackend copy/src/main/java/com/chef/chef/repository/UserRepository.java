package com.chef.chef.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chef.chef.model.User;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    List<User> findByRole(String role);

    boolean existsByUsername(String username);

    List<User> findByRoleAndUsernameContainingIgnoreCase(
            String role,
            String username);

    long countByRoleAndCreateTimeGreaterThanEqualAndCreateTimeLessThan(
            String role,
            LocalDateTime start,
            LocalDateTime end);
}