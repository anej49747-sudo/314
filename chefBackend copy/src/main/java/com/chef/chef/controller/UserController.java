package com.chef.chef.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 统一生成用户公开资料
     * 不返回 password
     */
    private Map<String, Object> toUserData(User user) {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        data.put("name", user.getName());

        return data;
    }

    /**
     * 设计师公开资料
     */
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

    /**
     * 注册
     * POST /api/register
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
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

    /**
     * 登录
     * POST /api/login
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
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

    /**
     * 获取个人资料
     * GET /api/getuser?userId=用户ID
     */
    @GetMapping("/getuser")
    public Map<String, Object> getUser(
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

    /**
     * 获取设计师列表
     * GET /api/designer
     */
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

    /**
     * 获取单个设计师公开资料
     * GET /api/designer/{id}
     */
    @GetMapping("/designer/{id}")
    public Map<String, Object> getDesignerById(
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

    /**
     * 更新设计师个人资料
     * PUT /api/designer/{id}/profile
     */
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

    private String getString(Map<String, Object> body, String key) {
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

        return list.stream()
                .filter(item -> item instanceof String)
                .map(item -> ((String) item).trim())
                .filter(item -> !item.isEmpty())
                .toList();
    }
}