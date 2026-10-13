package com.chef.chef.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chef.chef.model.IDP;

public interface IdpRepository extends MongoRepository<IDP, String> {

    List<IDP> findByDesignerId(String designerId);
}