package com.chef.chef.utils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtUtils {
    // 新版JWT要求密钥长度要更长，把密钥加长一点以确保安全
    private static final String SECRET_STRING = "UserTokenSecretKeyMustBeAtLeast32BytesLong!";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRATION_TIME = 86400000; // 24小时

    public static String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY) // 自动识别算法
                .compact();
    }

    // 🔑 统一解析 Token 的方法
    public static String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY) // 用相同的密钥解密
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}