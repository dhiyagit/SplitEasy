package com.example.spliteasy.api;

import java.util.List;
import java.util.Map;

public class GroupInsightsResponse {
    private Double totalSpending;
    private String highestCategory;
    private Map<String, Double> categoryBreakdown;
    private Map<String, Double> monthlySummary;
    private Double currentMonthSpending;
    private Double previousMonthSpending;
    private String monthlyComparisonText;
    private List<MemberInsight> memberInsights;

    public static class MemberInsight {
        private Long userId;
        private String fullName;
        private Double totalPaid;
        private Double totalShare;

        public Long getUserId() { return userId; }
        public String getFullName() { return fullName; }
        public Double getTotalPaid() { return totalPaid != null ? totalPaid : 0.0; }
        public Double getTotalShare() { return totalShare != null ? totalShare : 0.0; }
    }

    public Double getTotalSpending() { return totalSpending != null ? totalSpending : 0.0; }
    public String getHighestCategory() { return highestCategory != null ? highestCategory : "None"; }
    public Map<String, Double> getCategoryBreakdown() { return categoryBreakdown; }
    public Map<String, Double> getMonthlySummary() { return monthlySummary; }
    public Double getCurrentMonthSpending() { return currentMonthSpending != null ? currentMonthSpending : 0.0; }
    public Double getPreviousMonthSpending() { return previousMonthSpending != null ? previousMonthSpending : 0.0; }
    public String getMonthlyComparisonText() { return monthlyComparisonText != null ? monthlyComparisonText : ""; }
    public List<MemberInsight> getMemberInsights() { return memberInsights; }
}
