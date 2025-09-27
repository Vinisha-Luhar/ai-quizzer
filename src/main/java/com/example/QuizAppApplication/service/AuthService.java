package com.example.QuizAppApplication.service;

import org.springframework.stereotype.Service;
import com.example.QuizAppApplication.util.JwtUtil;

@Service
public class AuthService {
    // mock auth: accept any username/password
    // returns JWT token string
    public String authenticateAndCreateToken(String username) {
        // token valid for 1 hour (3600 seconds)
        long ttlSeconds = 3600L;
        return JwtUtil.createToken(username, ttlSeconds);
    }
}
