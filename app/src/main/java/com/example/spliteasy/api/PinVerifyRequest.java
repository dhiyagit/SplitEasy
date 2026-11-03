package com.example.spliteasy.api;

public class PinVerifyRequest {
    private Long userId;
    private String pin;

    public PinVerifyRequest(Long userId, String pin) {
        this.userId = userId;
        this.pin = pin;
    }
}
