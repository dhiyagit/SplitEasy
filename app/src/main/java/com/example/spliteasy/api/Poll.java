package com.example.spliteasy.api;

import java.util.List;

public class Poll {
    private Long id;
    private Long groupId;
    private String question;
    private Long creatorUserId;
    private List<PollOption> options;
    private Long userVotedOptionId;

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public String getQuestion() { return question; }
    public Long getCreatorUserId() { return creatorUserId; }
    public List<PollOption> getOptions() { return options; }
    public Long getUserVotedOptionId() { return userVotedOptionId; }
}
