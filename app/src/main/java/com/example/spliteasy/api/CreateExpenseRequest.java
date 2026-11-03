package com.example.spliteasy.api;

import java.util.Map;

public class CreateExpenseRequest {
    private String title;
    private Double amount;
    private Long paidByUserId;
    private String description;
    private String splitType;
    private Map<Long, Double> shares;
    private Map<Long, Double> exactAmounts;
    private Map<Long, Double> percentages;
    private String receiptImageBase64;
    private String category;

    public CreateExpenseRequest(String title, Double amount, Long paidByUserId, String description,
                                String splitType, Map<Long, Double> shares,
                                Map<Long, Double> exactAmounts, Map<Long, Double> percentages,
                                String receiptImageBase64, String category) {
        this.title = title;
        this.amount = amount;
        this.paidByUserId = paidByUserId;
        this.description = description;
        this.splitType = splitType;
        this.shares = shares;
        this.exactAmounts = exactAmounts;
        this.percentages = percentages;
        this.receiptImageBase64 = receiptImageBase64;
        this.category = category;
    }

    public String getTitle() { return title; }
    public Double getAmount() { return amount; }
    public Long getPaidByUserId() { return paidByUserId; }
    public String getDescription() { return description; }
    public String getSplitType() { return splitType; }
    public Map<Long, Double> getShares() { return shares; }
    public Map<Long, Double> getExactAmounts() { return exactAmounts; }
    public Map<Long, Double> getPercentages() { return percentages; }
    public String getReceiptImageBase64() { return receiptImageBase64; }
    public String getCategory() { return category; }
}
