package com.example.spliteasy.api;

public class Group {
    private Long id;
    private String groupName;
    private Long creatorUserId;
    private String createdAt;

    public Long getId() {
        return id;
    }

    public String getGroupName() {
        return groupName;
    }

    public Long getCreatorUserId() {
        return creatorUserId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return groupName != null ? groupName : "Group #" + id;
    }
}
