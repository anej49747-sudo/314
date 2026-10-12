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
        data.put("active", !Boolean.FALSE.equals(user.getActive()));

        data.put("name", user.getName());
        data.put("gender", user.getGender());
        data.put("email", user.getEmail());
        data.put("company", user.getCompany());
        data.put("bio", user.getBio());
        data.put(
                "designStyles",
                user.getDesignStyles() == null
                        ? List.of()
                        : user.getDesignStyles());

        return data;
    }

    private Map<String, Object> toDesignerData(User user) {
        return toUserData(user);
    }

    @PostMapping("/register")
    public Map<String, Object> register(
            @RequestBody Map<String, String> body) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            User user = userService.register(
                    body.get("username"),
                    body.get("password"));

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
            @RequestBody Map<String, String> body) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            User user = userService.checkLogin(
                    body.get("username"),
                    body.get("password"));

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
            @RequestParam("userId") String userId) {
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
            @RequestBody Map<String, String> body) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            User user = userService.createDesigner(
                    body.get("username"),
                    body.get("password"),
                    body.get("name"));

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
            @PathVariable("id") String id) {
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

    @DeleteMapping("/designer/{id}")
    public Map<String, Object> deleteDesigner(
            @PathVariable("id") String id) {
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
            @PathVariable("id") String id) {
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
            @RequestBody Map<String, Object> body) {
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
                    designStyles);

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
            @RequestBody Map<String, Object> body) {
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
                    bio);

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
            String key) {
        Object value = body.get(key);
        return value == null ? "" : value.toString().trim();
    }

    private List<String> getStringList(
            Map<String, Object> body,
            String key) {
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

    @GetMapping("/admin/customers")
    public Map<String, Object> getCustomers() {
        Map<String, Object> response = new LinkedHashMap<>();

        List<Map<String, Object>> customers = userService.findCustomers()
                .stream()
                .map(this::toUserData)
                .toList();

        response.put("status", "success");
        response.put("customers", customers);

        return response;
    }

    @GetMapping("/admin/customers/search")
    public Map<String, Object> searchCustomers(
            @RequestParam("username") String username) {
        Map<String, Object> response = new LinkedHashMap<>();

        List<Map<String, Object>> customers = userService.searchCustomers(username)
                .stream()
                .map(this::toUserData)
                .toList();

        response.put("status", "success");
        response.put("customers", customers);

        return response;
    }

    @PutMapping("/admin/customers/{id}/deactivate")
    public Map<String, Object> deactivateCustomer(
            @PathVariable("id") String id) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            User user = userService.deactivateCustomer(id);

            response.put("status", "success");
            response.put(
                    "message",
                    "Customer account deactivated successfully");
            response.put("customer", toUserData(user));

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    @PutMapping("/admin/customers/{id}/reactivate")
    public Map<String, Object> reactivateCustomer(
            @PathVariable("id") String id) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            User user = userService.reactivateCustomer(id);

            response.put("status", "success");
            response.put(
                    "message",
                    "Customer account reactivated successfully");
            response.put("customer", toUserData(user));

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }
}