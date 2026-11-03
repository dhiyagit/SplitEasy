package com.example.spliteasy.api;

public class RegisterRequest {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String password;
    private String pin;

    public RegisterRequest(String fullName, String email, String phoneNumber, String password, String pin) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.pin = pin;
    }
}
