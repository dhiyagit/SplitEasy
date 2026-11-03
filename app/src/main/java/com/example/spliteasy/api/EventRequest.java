package com.example.spliteasy.api;

public class EventRequest {
    private String title;
    private String description;
    private String eventDate;
    private String eventTime;
    private String venue;
    private Long creatorUserId;

    public EventRequest() {}

    public EventRequest(String title, String description, String eventDate, String eventTime, String venue, Long creatorUserId) {
        this.title = title;
        this.description = (description == null || description.trim().isEmpty()) ? null : description.trim();
        this.eventDate = (eventDate == null || eventDate.trim().isEmpty()) ? null : eventDate.trim();
        this.eventTime = (eventTime == null || eventTime.trim().isEmpty()) ? null : eventTime.trim();
        this.venue = (venue == null || venue.trim().isEmpty()) ? null : venue.trim();
        this.creatorUserId = creatorUserId;
    }
}
