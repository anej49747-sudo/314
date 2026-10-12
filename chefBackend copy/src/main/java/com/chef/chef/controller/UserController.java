package com.chef.chef.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chef.chef.model.User;
import com.chef.chef.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    private Map<String, Object> toUserData(User user) {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        data.put("name", user.getName());

        return data;
    }

    private Map<String, Object> toDesignerData(User user) {
        Map<String, Object> data = toUserData(user);

        data.put("gender", user.getGender());
        data.put("email", user.getEmail());
        data.put("company", user.getCompany());
        data.put("bio", user.getBio());
        data.put(
                "designStyles",
                user.getDesignStyles() == null
                        ? List.of()
                        : user.getDesignStyles()
        );

        return data;
    }

    @PostMapping("/register")
    public Map<String, Object> register(
            @RequestBody Map<String, String> body
    ) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            String username = body.get("username");
            String password = body.get("password");

            User user = userService.register(username, password);

            response.put("status", "success");
            response.put("message", "Registration successful");
            response.put("userdata", toUserData(user));

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    @PostMapping("/login")
    public Map<String, Object> login(
            @RequestBody Map<String, String> body
    ) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            String username = body.get("username");
            String password = body.get("password");

            User user = userService.checkLogin(username, password);

            response.put("status", "success");
            response.put("message", "Login successful");
            response.put("userdata", toUserData(user));

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    @GetMapping("/getuser")
    public Map<String, Object> getUser(
            // 从/getuser取值
            @RequestParam("userId") String userId
    ) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            User user = userService.getUser(userId);

            response.put("status", "success");
            response.put("userdata", toUserData(user));

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    @GetMapping("/designer")
    public Map<String, Object> getDesigners() {
        List<Map<String, Object>> designers = userService.findDesigners()
                .stream()
                .map(this::toDesignerData)
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("designers", designers);

        return response;
    }

    @PostMapping("/admin/create-designer")
    public Map<String, Object> createDesigner(
            @RequestBody Map<String, String> body
    ) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            User user = userService.createDesigner(
                    body.get("username"),
                    body.get("password"),
                    body.get("name")
            );

            response.put("status", "success");
            response.put("message", "Designer created successfully");
            response.put("designer", toDesignerData(user));

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    @GetMapping("/designer/{id}")
    public Map<String, Object> getDesignerById(
            // 从/designer/{id}取值
            @PathVariable("id") String id
    ) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            User user = userService.getDesignerById(id);

            response.put("status", "success");
            response.put("designer", toDesignerData(user));

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    // 新增：删除指定的 Designer 账号
    @DeleteMapping("/designer/{id}")
    public Map<String, Object> deleteDesigner(
            @PathVariable("id") String id
    ) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            userService.deleteDesigner(id);

            response.put("status", "success");
            response.put("message", "Designer deleted successfully");

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }



@DeleteMapping("/user/{id}/account")
public Map<String, Object> deleteCustAccount(
        @PathVariable("id") String id
) {
    Map<String, Object> response = new LinkedHashMap<>();

    try {
        userService.deleteCustAccount(id);

        response.put("status", "success");
        response.put("message", "Account deleted successfully");

    } catch (IllegalArgumentException e) {
        response.put("status", "error");
        response.put("message", e.getMessage());
    }

    return response;
}



    @PutMapping("/designer/{id}/profile")
    public Map<String, Object> updateDesignerProfile(
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> body
    ) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            String name = getString(body, "name");
            String gender = getString(body, "gender");
            String email = getString(body, "email");
            String company = getString(body, "company");
            String bio = getString(body, "bio");

            List<String> designStyles = getStringList(body, "designStyles");

            User user = userService.updateDesignerProfile(
                    id,
                    name,
                    gender,
                    email,
                    company,
                    bio,
                    designStyles
            );

            response.put("status", "success");
            response.put("message", "Profile updated successfully");
            response.put("designer", toDesignerData(user));

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    @PutMapping("/user/{id}/profile")
    public Map<String, Object> updateUserProfile(
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> body
    ) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            String name = getString(body, "name");
            String gender = getString(body, "gender");
            String email = getString(body, "email");
            String bio = getString(body, "bio");

            User user = userService.updateUserProfile(
                    id,
                    name,
                    gender,
                    email,
                    bio
            );

            response.put("status", "success");
            response.put("message", "Profile updated successfully");
            response.put("userdata", toUserData(user));

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    private String getString(
            Map<String, Object> body,
            String key
    ) {
        Object value = body.get(key);
        return value == null ? "" : value.toString().trim();
    }

    private List<String> getStringList(
            Map<String, Object> body,
            String key
    ) {
        Object value = body.get(key);

        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        // stream流, 依次处理以下步骤
        return list.stream()
                // 筛选所有String
                .filter(item -> item instanceof String)
                // map转换认定String元素 trim去掉前后空格
                .map(item -> ((String) item).trim())
                // 只保留符合条件的元素
                .filter(item -> !item.isEmpty())
                .toList();
    }
}