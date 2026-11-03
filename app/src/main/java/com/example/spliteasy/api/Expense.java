package com.example.spliteasy.api;

public class Expense {
    private Long id;
    private Long groupId;
    private String title;
    private Double amount;
    private Long paidByUserId;
    private String description;
    private String expenseDate;

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public String getTitle() { return title; }
    public Double getAmount() { return amount; }
    public Long getPaidByUserId() { return paidByUserId; }
    public String getDescription() { return description; }
    public String getExpenseDate() { return expenseDate; }
}
