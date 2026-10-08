package com.chef.chef.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chef.chef.model.User;
import com.chef.chef.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController // 告诉 Spring 这是一个 API 控制器
@RequestMapping("/api") // 这一组接口的前缀
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private static final String JWT_SECRET = "UserTokenSecretKeyMustBeAtLeast32BytesLong!";

    @PostMapping(value = "/login", produces = "application/json;charset=UTF-8")

    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        Map<String, Object> response = new HashMap<>();

        System.out.println(loginData);
        try {
            User user = userService.checkLogin(username, password);

            // if (user != null) {
            System.out.println(user);
            response.put("userdata", user);
            response.put("message", username + "登录成功！");
            response.put("status", "success");
            // response.put("token", token);
            // }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

        }

    }

    @GetMapping(value = "/getuser", produces = "application/json;charset=UTF-8")
    public ResponseEntity<Map<String, Object>> getUser(
            @RequestParam("userId") String userId

    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            User user = userService.getUser(userId);
            if (user != null) {
                response.put("userdata", user);
                response.put("status", "success");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
