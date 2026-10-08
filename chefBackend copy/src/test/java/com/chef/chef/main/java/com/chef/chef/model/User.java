package com.chef.chef.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // 自动生成 Getter, Setter, toString, equals, hashCode
@NoArgsConstructor // 自动生成无参构造函数
@AllArgsConstructor
@Document(collection = "user") // 必须和 database里的名字一模一样
public class User {
    @Id
    private String id; // 对应数据库里的 _id

    // private String email; // 对应数据库里的 email
    // private String identity; // 对应数据库里的 identity
    // private String avatar;
    private String username;
    // @JsonIgnore
    private String password;
    private String role;
    private String name;
    // @Field("user_title")
    // private String userTitle;

    // @Field("loginStatus") // 对应数据库里的 loginStatus
    // private Boolean loginStatus;

}