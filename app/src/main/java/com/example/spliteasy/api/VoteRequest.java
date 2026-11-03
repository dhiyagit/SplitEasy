package com.example.spliteasy.api;

public class VoteRequest {
    private Long userId;
    private Long optionId;

    public VoteRequest() {}

    public VoteRequest(Long userId, Long optionId) {
        this.userId = userId;
        this.optionId = optionId;
    }
}
