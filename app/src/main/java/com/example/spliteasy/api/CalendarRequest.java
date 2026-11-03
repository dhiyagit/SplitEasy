package com.example.spliteasy.api;

public class CalendarRequest {
    private String title;
    private String entryDate;
    private String description;
    private Long creatorUserId;

    public CalendarRequest() {}

    public CalendarRequest(String title, String entryDate, String description, Long creatorUserId) {
        this.title = title;
        this.entryDate = (entryDate == null || entryDate.trim().isEmpty()) ? null : entryDate.trim();
        this.description = (description == null || description.trim().isEmpty()) ? null : description.trim();
        this.creatorUserId = creatorUserId;
    }
}
