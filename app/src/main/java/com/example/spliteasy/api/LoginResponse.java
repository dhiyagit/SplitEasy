package com.example.spliteasy.api;

public class LoginResponse {
    private String message;
    private Long userId;
    private String fullName;
    private String email;

    public String getMessage() {
        return message;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }
}
