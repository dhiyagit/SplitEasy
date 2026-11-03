package com.example.spliteasy.api;

public class Trip {
    private Long id;
    private Long groupId;
    private String tripName;
    private String destination;
    private String startDate;
    private String endDate;
    private Double totalBudget;
    private Long creatorUserId;

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public String getTripName() { return tripName; }
    public String getDestination() { return destination; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public Double getTotalBudget() { return totalBudget != null ? totalBudget : 0.0; }
    public Long getCreatorUserId() { return creatorUserId; }

    @Override
    public String toString() {
        return tripName + (destination != null && !destination.isEmpty() ? " (" + destination + ")" : "");
    }
}
