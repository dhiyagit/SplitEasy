package com.example.spliteasy.api;

public class EventGuest {
    private Long id;
    private Long eventId;
    private String guestName;
    private String email;
    private String rsvpStatus;

    public Long getId() { return id; }
    public Long getEventId() { return eventId; }
    public String getGuestName() { return guestName; }
    public String getEmail() { return email; }
    public String getRsvpStatus() { return rsvpStatus != null ? rsvpStatus : "PENDING"; }
}
