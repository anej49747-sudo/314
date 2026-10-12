package com.chef.chef.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user")
public class User {

    @Id
    private String id;

    private String username;
    private String password;
    private String role;
    private Boolean active;

    // Designer profile information
    private String name;
    private String gender;
    private String email;
    private String company;
    private String bio;

    // Multiple design styles
    private List<String> designStyles;
}