package com.example.spliteasy.api;

public class GuestRequest {
    private Long userId;
    private String guestName;
    private String email;
    private String rsvpStatus;

    public GuestRequest() {}

    public GuestRequest(Long userId, String guestName, String email, String rsvpStatus) {
        this.userId = userId;
        this.guestName = (guestName == null || guestName.trim().isEmpty()) ? null : guestName.trim();
        this.email = (email == null || email.trim().isEmpty()) ? null : email.trim();
        this.rsvpStatus = (rsvpStatus == null || rsvpStatus.trim().isEmpty()) ? "PENDING" : rsvpStatus.trim().toUpperCase();
    }
}
