package com.chef.chef.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "posts")
public class posts {

    @Id
    private String id;

    private String title;

    private String description;

    private String imageUrl;

    private String style;
}