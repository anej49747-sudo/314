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

    // Login
    public User checkLogin(String username, String password) {
        System.out.println("正在查询用户名：" + username);

        Optional<User> userOpt = userRepository.findByUsername(username);

        System.out.println("是否找到用户：" + userOpt.isPresent());

        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在！");
        }

        User user = userOpt.get();

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("密码错误！");
        }

        System.out.println("密码验证通过");

        return userRepository.save(user);
    }

    // Get one user by ID
    public User getUser(String userid) {
        return userRepository.findById(userid)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Get all designers
    public List<User> findDesigners() {
        return userRepository.findByRole("designer");
    }
}