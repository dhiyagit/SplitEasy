package com.example.spliteasy.api;

public class GroupAnnouncement {
    private Long id;
    private Long groupId;
    private Long userId;
    private String authorName;
    private String message;
    private String createdAt;

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public Long getUserId() { return userId; }
    public String getAuthorName() { return authorName; }
    public String getMessage() { return message; }
    public String getCreatedAt() { return createdAt != null ? createdAt.replace("T", " ") : ""; }
}
