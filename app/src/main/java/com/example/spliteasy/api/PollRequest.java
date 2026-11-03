package com.example.spliteasy.api;

import java.util.List;

public class PollRequest {
    private String question;
    private List<String> options;
    private Long creatorUserId;

    public PollRequest() {}

    public PollRequest(String question, List<String> options, Long creatorUserId) {
        this.question = question;
        this.options = options;
        this.creatorUserId = creatorUserId;
    }
}
