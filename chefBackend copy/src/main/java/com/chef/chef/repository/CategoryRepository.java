package com.chef.chef.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chef.chef.model.Category;

public interface CategoryRepository extends MongoRepository<Category, String> {

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Category> findAllByOrderByNameAsc();
}