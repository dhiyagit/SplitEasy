package com.example.spliteasy.api;

public class RemindRequest {
    private Long senderUserId;
    private Long recipientUserId;

    public RemindRequest(Long senderUserId, Long recipientUserId) {
        this.senderUserId = senderUserId;
        this.recipientUserId = recipientUserId;
    }

    public Long getSenderUserId() { return senderUserId; }
    public Long getRecipientUserId() { return recipientUserId; }
}
