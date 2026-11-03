package com.example.spliteasy.api;

public class PaymentReminder {
    private Long id;
    private Long groupId;
    private Long senderUserId;
    private Long recipientUserId;
    private Double amount;
    private String groupName;
    private String reminderDate;
    private boolean viewed;
    private boolean resolved;

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public Long getSenderUserId() { return senderUserId; }
    public Long getRecipientUserId() { return recipientUserId; }
    public Double getAmount() { return amount; }
    public String getGroupName() { return groupName; }
    public String getReminderDate() { return reminderDate; }
    public boolean isViewed() { return viewed; }
    public boolean isResolved() { return resolved; }
}
