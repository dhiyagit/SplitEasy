package com.example.spliteasy.api;

public class GroupEvent {
    private Long id;
    private Long groupId;
    private String title;
    private String description;
    private String eventDate;
    private String eventTime;
    private String venue;
    private Long creatorUserId;

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getEventDate() { return eventDate; }
    public String getEventTime() { return eventTime; }
    public String getVenue() { return venue; }
    public Long getCreatorUserId() { return creatorUserId; }

    @Override
    public String toString() {
        return title + (venue != null && !venue.isEmpty() ? " @ " + venue : "") + "\nDate: " + (eventDate != null ? eventDate : "TBD") + (eventTime != null && !eventTime.isEmpty() ? " " + eventTime : "");
    }
}
