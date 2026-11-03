package com.example.spliteasy.api;

public class TripExpense {
    private Long id;
    private Long tripId;
    private String title;
    private Double amount;
    private String category;
    private String expenseDate;
    private Long paidByUserId;
    private String paidByName;

    public Long getId() { return id; }
    public Long getTripId() { return tripId; }
    public String getTitle() { return title; }
    public Double getAmount() { return amount != null ? amount : 0.0; }
    public String getCategory() { return category; }
    public String getExpenseDate() { return expenseDate; }
    public Long getPaidByUserId() { return paidByUserId; }
    public String getPaidByName() { return paidByName; }
}
