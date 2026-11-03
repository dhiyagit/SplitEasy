package com.example.spliteasy.api;

public class GroupSubscription {
    private Long id;
    private Long groupId;
    private String subscriptionName;
    private Double cost;
    private String renewalDate;
    private Long payerUserId;
    private String payerName;

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public String getSubscriptionName() { return subscriptionName; }
    public Double getCost() { return cost != null ? cost : 0.0; }
    public String getRenewalDate() { return renewalDate; }
    public Long getPayerUserId() { return payerUserId; }
    public String getPayerName() { return payerName; }
}
