package com.example.QuizAppApplication.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.QuizAppApplication.model.AuthRequest;
import com.example.QuizAppApplication.model.AuthResponse;
import com.example.QuizAppApplication.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    // Accept any username/password and return a signed JWT
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String token = authService.authenticateAndCreateToken(req.getUsername());
        // parse expiry from token to return expiresAt - but easier: JwtUtil encodes exp; let's compute epoch now + 3600 here
        long expiresAt = java.time.Instant.now().getEpochSecond() + 3600L;
        return ResponseEntity.ok(new AuthResponse(token, expiresAt));
    }
}

