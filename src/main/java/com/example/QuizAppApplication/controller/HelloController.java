package com.example.QuizAppApplication.controller;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HelloController {
    // Simple protected endpoint
    @GetMapping("/hello")
    public ResponseEntity<?> hello(HttpServletRequest request) {
        // JwtFilter sets attribute "username" when token is valid
        String username = (String) request.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok("Hello, " + username + "! This is a protected resource.");
    }
}


