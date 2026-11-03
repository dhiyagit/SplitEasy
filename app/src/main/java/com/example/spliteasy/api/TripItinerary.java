package com.example.spliteasy.api;

public class TripItinerary {
    private Long id;
    private Long tripId;
    private String title;
    private String description;
    private String activityDate;
    private String location;

    public Long getId() { return id; }
    public Long getTripId() { return tripId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getActivityDate() { return activityDate; }
    public String getLocation() { return location; }
}
