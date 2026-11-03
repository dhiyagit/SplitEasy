package com.example.spliteasy.api;

public class PinSetRequest {
    private Long userId;
    private String newPin;

    public PinSetRequest(Long userId, String newPin) {
        this.userId = userId;
        this.newPin = newPin;
    }
}
