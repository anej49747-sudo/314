package com.chef.chef.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chef.chef.model.User;

public interface UserRepository extends MongoRepository<User, String> {
    // 只要写出方法名，Spring 会自动帮你实现查询逻辑
    Optional<User> findByUsername(String username);

    Optional<User> findById(String id);
}
