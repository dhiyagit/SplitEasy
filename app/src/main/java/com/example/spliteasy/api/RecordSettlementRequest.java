package com.example.spliteasy.api;

public class RecordSettlementRequest {
    private Long payerUserId;
    private Long payeeUserId;
    private Double amount;
    private String description;

    public RecordSettlementRequest(Long payerUserId, Long payeeUserId, Double amount, String description) {
        this.payerUserId = payerUserId;
        this.payeeUserId = payeeUserId;
        this.amount = amount;
        this.description = description;
    }

    public Long getPayerUserId() { return payerUserId; }
    public Long getPayeeUserId() { return payeeUserId; }
    public Double getAmount() { return amount; }
    public String getDescription() { return description; }
}
