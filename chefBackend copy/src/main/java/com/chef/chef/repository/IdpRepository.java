package com.chef.chef.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chef.chef.model.posts;

public interface IdpRepository extends MongoRepository<posts, String> {

    // Find projects with the selected style
    List<posts> findByStyle(String style);
}