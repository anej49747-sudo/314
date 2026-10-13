package com.chef.chef.model;

import java.time.LocalDateTime;
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

    private String name;
    private String gender;
    private String email;
    private String company;
    private String bio;
    private List<String> designStyles;

    private String companyDescription;
    private String companyContact;
    private String companyAddress;

    private LocalDateTime createTime;
}