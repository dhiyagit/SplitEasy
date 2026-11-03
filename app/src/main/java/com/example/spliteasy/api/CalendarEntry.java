package com.example.spliteasy.api;

public class CalendarEntry {
    private Long id;
    private Long groupId;
    private String title;
    private String entryDate;
    private String description;
    private Long creatorUserId;

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public String getTitle() { return title; }
    public String getEntryDate() { return entryDate; }
    public String getDescription() { return description; }
    public Long getCreatorUserId() { return creatorUserId; }
}
