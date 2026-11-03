package com.example.spliteasy.api;

public class AnnouncementRequest {
    private Long userId;
    private String message;

    public AnnouncementRequest() {}

    public AnnouncementRequest(Long userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
