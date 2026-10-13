package com.chef.chef.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "idp")
public class IDP {

    @Id
    private String id;

    private String designerId;

    private String title;

    private String description;

    private String categoryId;

    private List<String> image = new ArrayList<>();

    private String status = "current";

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}