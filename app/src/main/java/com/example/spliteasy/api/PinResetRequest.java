package com.example.spliteasy.api;

public class PinResetRequest {
    private String identifier;
    private String otp;
    private String newPin;

    public PinResetRequest(String identifier, String otp, String newPin) {
        this.identifier = identifier;
        this.otp = otp;
        this.newPin = newPin;
    }
}
