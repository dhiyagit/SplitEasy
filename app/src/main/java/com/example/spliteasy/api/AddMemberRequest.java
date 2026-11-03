package com.example.spliteasy.api;

public class AddMemberRequest {
    private String email;

    public AddMemberRequest(String email) {
        this.email = email;
    }

    public String getEmail() { return email; }
}
