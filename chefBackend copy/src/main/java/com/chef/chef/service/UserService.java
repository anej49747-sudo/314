package com.chef.chef.service;

import java.util.ArrayList;
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

    public User checkLogin(String username, String password) {
        Optional<User> userOpt =
                userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "User does not exist"
            );
        }

        User user = userOpt.get();

// Reject deactivated accounts. Existing accounts without an active field remain enabled.
if (Boolean.FALSE.equals(user.getActive())) {
    throw new IllegalArgumentException(
            "Your account has been deactivated"
    );
}

        if (user.getPassword() == null
                || !user.getPassword().equals(password)) {
            throw new IllegalArgumentException(
                    "Incorrect password"
            );
        }

        return user;
    }

    public User register(String username, String password) {
        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Username and password are required"
            );
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                    "Username already exists"
            );
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole("customer");
        user.setActive(true);

        return userRepository.save(user);
    }

    public User getUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "User ID is required"
            );
        }

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );
    }

    public List<User> findDesigners() {
        return userRepository.findByRole("designer");
    }

    public User createDesigner(
            String username,
            String password,
            String name) {

        if (username == null || username.isBlank()
                || password == null || password.isBlank()
                || name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Username, password and full name are required"
            );
        }

        username = username.trim();
        name = name.trim();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                    "Username already exists"
            );
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setName(name);
        user.setRole("designer");

        return userRepository.save(user);
    }

    public User getDesignerById(String id) {
        User user = getUser(id);

        if (user.getRole() == null
                || !user.getRole().equalsIgnoreCase("designer")) {
            throw new IllegalArgumentException(
                    "Designer not found"
            );
        }

        return user;
    }

    // Delete designer account
    public void deleteDesigner(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Designer ID is required"
            );
        }

        User designer = getDesignerById(id);

        userRepository.deleteById(designer.getId());
    }

    public User updateDesignerProfile(
            String userId,
            String name,
            String gender,
            String email,
            String company,
            String bio,
            List<String> designStyles) {

        User user = getDesignerById(userId);

        user.setName(name);
        user.setGender(gender);
        user.setEmail(email);
        user.setCompany(company);
        user.setBio(bio);

        List<String> styles = new ArrayList<>();

        if (designStyles != null) {
            for (String style : designStyles) {
                if (style != null && !style.isBlank()) {
                    styles.add(style.trim());
                }
            }
        }

        user.setDesignStyles(styles);

        return userRepository.save(user);
    }

    public User updateUserProfile(
            String userId,
            String name,
            String gender,
            String email,
            String bio) {

        User user = getUser(userId);

        user.setName(name);
        user.setGender(gender);
        user.setEmail(email);
        user.setBio(bio);

        return userRepository.save(user);
    }

    // Delete the customer's own account
    public void deleteCustAccount(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "User ID is required"
            );
        }

        User user = getUser(userId);

        String role = user.getRole() == null
                ? ""
                : user.getRole().trim().toLowerCase();

        if ("admin".equals(role) || "manager".equals(role)) {
            throw new IllegalArgumentException(
                    "Admin and Manager cannot delete their own accounts"
            );
        }

        if (!"customer".equals(role) && !"designer".equals(role)) {
            throw new IllegalArgumentException(
                    "This role is not allowed to delete its account"
            );
        }

        userRepository.deleteById(user.getId());
    }
}
