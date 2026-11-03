package com.example.spliteasy.api;

public class ItineraryRequest {
    private Long userId;
    private String title;
    private String description;
    private String activityDate;
    private String location;

    public ItineraryRequest() {}

    public ItineraryRequest(Long userId, String title, String description, String activityDate, String location) {
        this.userId = userId;
        this.title = title;
        this.description = (description == null || description.trim().isEmpty()) ? null : description.trim();
        this.activityDate = (activityDate == null || activityDate.trim().isEmpty()) ? null : activityDate.trim();
        this.location = (location == null || location.trim().isEmpty()) ? null : location.trim();
    }
}
