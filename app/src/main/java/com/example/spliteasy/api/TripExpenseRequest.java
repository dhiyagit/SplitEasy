package com.example.spliteasy.api;

public class TripExpenseRequest {
    private Long paidByUserId;
    private String title;
    private Double amount;
    private String category;
    private String expenseDate;

    public TripExpenseRequest() {}

    public TripExpenseRequest(Long paidByUserId, String title, Double amount, String category, String expenseDate) {
        this.paidByUserId = paidByUserId;
        this.title = title;
        this.amount = amount;
        this.category = (category == null || category.trim().isEmpty()) ? null : category.trim();
        this.expenseDate = (expenseDate == null || expenseDate.trim().isEmpty()) ? null : expenseDate.trim();
    }
}
