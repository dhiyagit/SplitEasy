package com.example.spliteasy.api;

public class PollOption {
    private Long id;
    private Long pollId;
    private String optionText;
    private long voteCount;

    public Long getId() { return id; }
    public Long getPollId() { return pollId; }
    public String getOptionText() { return optionText; }
    public long getVoteCount() { return voteCount; }
}
