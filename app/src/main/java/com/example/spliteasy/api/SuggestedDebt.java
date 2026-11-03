package com.example.spliteasy.api;

import com.example.spliteasy.CurrencyUtils;

public class SuggestedDebt {
    private Long debtorUserId;
    private String debtorName;
    private Long creditorUserId;
    private String creditorName;
    private Double amount;

    public Long getDebtorUserId() { return debtorUserId; }
    public String getDebtorName() { return debtorName; }
    public Long getCreditorUserId() { return creditorUserId; }
    public String getCreditorName() { return creditorName; }
    public Double getAmount() { return amount; }

    public String getFormattedText(Long currentUserId) {
        String dStr = (debtorUserId != null && debtorUserId.equals(currentUserId)) ? "You" : (debtorName != null ? debtorName : "Member");
        String cStr = (creditorUserId != null && creditorUserId.equals(currentUserId)) ? "You" : (creditorName != null ? creditorName : "Member");
        double amt = amount != null ? amount : 0.0;
        return dStr + " owes " + cStr + " " + CurrencyUtils.format(amt);
    }

    @Override
    public String toString() {
        double amt = amount != null ? amount : 0.0;
        return (debtorName != null ? debtorName : "Member") + " owes " + (creditorName != null ? creditorName : "Member") + " " + CurrencyUtils.format(amt);
    }
}
