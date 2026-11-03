package com.example.spliteasy.api;

public class SubscriptionRequest {
    private String subscriptionName;
    private Double cost;
    private String renewalDate;
    private Long payerUserId;

    public SubscriptionRequest() {}

    public SubscriptionRequest(String subscriptionName, Double cost, String renewalDate, Long payerUserId) {
        this.subscriptionName = subscriptionName;
        this.cost = cost;
        this.renewalDate = (renewalDate == null || renewalDate.trim().isEmpty()) ? null : renewalDate.trim();
        this.payerUserId = payerUserId;
    }

    public String getSubscriptionName() { return subscriptionName; }
    public void setSubscriptionName(String subscriptionName) { this.subscriptionName = subscriptionName; }
    public Double getCost() { return cost; }
    public void setCost(Double cost) { this.cost = cost; }
    public String getRenewalDate() { return renewalDate; }
    public void setRenewalDate(String renewalDate) { this.renewalDate = renewalDate; }
    public Long getPayerUserId() { return payerUserId; }
    public void setPayerUserId(Long payerUserId) { this.payerUserId = payerUserId; }
}
