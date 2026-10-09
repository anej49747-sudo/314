
package com.chef.chef.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    // =========================
    // Register
    // =========================
    @PostMapping(
        value = "/register",
        produces = "application/json;charset=UTF-8"
    )
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody Map<String, String> registerData) {

        Map<String, Object> response = new HashMap<>();

        String username = registerData.get("username");
        String password = registerData.get("password");

        try {
            User user = userService.register(username, password);

            response.put("status", "success");
            response.put("message", "Registration successful");
            response.put("id", user.getId());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());

            if ("Username already exists".equals(e.getMessage())) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(response);
            }

            return ResponseEntity
                    .badRequest()
                    .body(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Registration failed");

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    // =========================
    // Login
    // =========================
    @PostMapping(
        value = "/login",
        produces = "application/json;charset=UTF-8"
    )
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> loginData) {

        String username = loginData.get("username");
        String password = loginData.get("password");

        Map<String, Object> response = new HashMap<>();

        try {
            User user = userService.checkLogin(username, password);

            // Return public user information only
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("role", user.getRole());
            userData.put("name", user.getName());

            response.put("userdata", userData);
            response.put("message", username + "登录成功！");
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }
    }

    // =========================
    // Get one user by ID
    // =========================
    @GetMapping(
        value = "/getuser",
        produces = "application/json;charset=UTF-8"
    )
    public ResponseEntity<Map<String, Object>> getUser(
            @RequestParam("userId") String userId) {

        Map<String, Object> response = new HashMap<>();

        try {
            User user = userService.getUser(userId);

            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("role", user.getRole());
            userData.put("name", user.getName());

            response.put("userdata", userData);
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "User not found");

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(response);
        }
    }

    // =========================
    // Get all designers
    // =========================
    @GetMapping(
        value = "/designer",
        produces = "application/json;charset=UTF-8"
    )
    public ResponseEntity<?> getDesigners() {

        try {
            List<User> designers = userService.findDesigners();

            List<Map<String, Object>> result = designers.stream()
                    .map(user -> {
                        Map<String, Object> item = new HashMap<>();

                        item.put("id", user.getId());
                        item.put("username", user.getUsername());
                        item.put("name", user.getName());

                        return item;
                    })
                    .toList();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();

            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to retrieve designers");

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    // =========================
    // Get one designer by ID
    // =========================
    @GetMapping(
        value = "/designer/{id}",
        produces = "application/json;charset=UTF-8"
    )
    public ResponseEntity<Map<String, Object>> getDesignerById(
            @PathVariable String id) {

        Map<String, Object> response = new HashMap<>();

        try {
            User user = userService.getUser(id);

            // Check whether the user is a designer
            if (!"designer".equalsIgnoreCase(user.getRole())) {
                response.put("status", "error");
                response.put("message", "Designer not found");

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(response);
            }

            // Return public information only
            Map<String, Object> designerData = new HashMap<>();
            designerData.put("id", user.getId());
            designerData.put("username", user.getUsername());
            designerData.put("name", user.getName());
            designerData.put("role", user.getRole());

            response.put("designer", designerData);
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Designer not found");

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(response);
        }
    }
}
