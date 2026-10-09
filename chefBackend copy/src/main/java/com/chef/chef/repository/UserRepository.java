
package com.chef.chef.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chef.chef.model.User;

public interface UserRepository extends MongoRepository<User, String> {

    // Find a user by username
    Optional<User> findByUsername(String username);

    // Find a user by ID
    // MongoRepository already provides findById(String id),
    // so no need to declare it again.

    // Find all users with a specific role
    List<User> findByRole(String role);

    // Check whether a username already exists
    boolean existsByUsername(String username);
}
