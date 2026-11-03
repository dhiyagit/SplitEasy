package com.example.spliteasy.api;

public class BirthdayRequest {
    private Long userId;
    private String birthday;

    public BirthdayRequest(Long userId, String birthday) {
        this.userId = userId;
        this.birthday = birthday;
    }
}
