package com.example.spliteasy.api;

public class UpiUpdateRequest {
    private Long userId;
    private String upiId;

    public UpiUpdateRequest(Long userId, String upiId) {
        this.userId = userId;
        this.upiId = upiId;
    }

    public Long getUserId() { return userId; }
    public String getUpiId() { return upiId; }
}
