package com.example.spliteasy.api;

public class TripRequest {
    private String tripName;
    private String destination;
    private String startDate;
    private String endDate;
    private Double totalBudget;
    private Long creatorUserId;

    public TripRequest() {}

    public TripRequest(String tripName, String destination, String startDate, String endDate, Double totalBudget, Long creatorUserId) {
        this.tripName = tripName;
        this.destination = (destination == null || destination.trim().isEmpty()) ? null : destination.trim();
        this.startDate = (startDate == null || startDate.trim().isEmpty()) ? null : startDate.trim();
        this.endDate = (endDate == null || endDate.trim().isEmpty()) ? null : endDate.trim();
        this.totalBudget = totalBudget != null ? totalBudget : 0.0;
        this.creatorUserId = creatorUserId;
    }
}
