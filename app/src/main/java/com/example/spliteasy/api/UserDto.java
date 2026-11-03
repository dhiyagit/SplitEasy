package com.example.spliteasy.api;

public class UserDto {
    private Long id;
    private String fullName;
    private String email;
    private String upiId;

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getUpiId() { return upiId; }

    @Override
    public String toString() {
        return fullName + " (" + email + ")";
    }
}
