package com.example.spliteasy.api;

public class Settlement {
    private Long id;
    private Long groupId;
    private Long payerUserId;
    private Long payeeUserId;
    private Double amount;
    private String description;
    private String settlementDate;

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public Long getPayerUserId() { return payerUserId; }
    public Long getPayeeUserId() { return payeeUserId; }
    public Double getAmount() { return amount; }
    public String getDescription() { return description; }
    public String getSettlementDate() { return settlementDate; }
}
