package com.chef.chef.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.chef.chef.model.User;
import com.chef.chef.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 用户登录
     */
    public User checkLogin(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User does not exist");
        }

        User user = userOpt.get();

        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("Incorrect password");
        }

        return user;
    }

    /**
     * 用户注册
     */
    public User register(String username, String password) {
        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Username and password are required"
            );
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole("customer");

        return userRepository.save(user);
    }

    /**
     * 根据 ID 获取用户
     */
    public User getUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID is required");
        }

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }

    /**
     * 获取所有设计师
     */
    public List<User> findDesigners() {
        return userRepository.findByRole("designer");
    }

    /**
     * 根据 ID 获取设计师公开资料
     */
    public User getDesignerById(String id) {
        User user = getUser(id);

        if (!"designer".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("Designer not found");
        }

        return user;
    }

    /**
     * 更新设计师个人资料
     */
    public User updateDesignerProfile(
            String userId,
            String name,
            String gender,
            String email,
            String company,
            String bio,
            List<String> designStyles
    ) {
        User user = getDesignerById(userId);

        user.setName(name);
        user.setGender(gender);
        user.setEmail(email);
        user.setCompany(company);
        user.setBio(bio);
        user.setDesignStyles(
                designStyles == null ? List.of() : designStyles
        );

        return userRepository.save(user);
    }
}