package com.example.QuizAppApplication.model;

public class AuthResponse {
    private String token;
    private long expiresAt; // epoch seconds

    public AuthResponse() {}

    public AuthResponse(String token, long expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
}
