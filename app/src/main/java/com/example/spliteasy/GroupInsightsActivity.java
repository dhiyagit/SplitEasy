package com.example.spliteasy;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.GroupInsightsResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupInsightsActivity extends AppCompatActivity {

    private Long groupId;
    private TextView totalSpendingTextView, highestCategoryTextView, monthlyComparisonTextView;
    private LinearLayout categoryBreakdownContainer, memberInsightsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_insights);

        groupId = getIntent().getLongExtra("GROUP_ID", -1L);

        totalSpendingTextView = findViewById(R.id.totalSpendingTextView);
        highestCategoryTextView = findViewById(R.id.highestCategoryTextView);
        monthlyComparisonTextView = findViewById(R.id.monthlyComparisonTextView);
        categoryBreakdownContainer = findViewById(R.id.categoryBreakdownContainer);
        memberInsightsContainer = findViewById(R.id.memberInsightsContainer);

        loadInsights();
    }

    private void loadInsights() {
        if (groupId == -1L) return;

        ApiClient.getApiService().getGroupInsights(groupId).enqueue(new Callback<GroupInsightsResponse>() {
            @Override
            public void onResponse(Call<GroupInsightsResponse> call, Response<GroupInsightsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GroupInsightsResponse data = response.body();

                    totalSpendingTextView.setText(CurrencyUtils.format(data.getTotalSpending()));
                    highestCategoryTextView.setText(data.getHighestCategory());
                    monthlyComparisonTextView.setText(data.getMonthlyComparisonText());

                    // Populate Category Breakdown
                    categoryBreakdownContainer.removeAllViews();
                    if (data.getCategoryBreakdown() != null) {
                        for (Map.Entry<String, Double> entry : data.getCategoryBreakdown().entrySet()) {
                            TextView tv = new TextView(GroupInsightsActivity.this);
                            tv.setText("• " + entry.getKey() + ": " + CurrencyUtils.format(entry.getValue()));
                            tv.setTextSize(15);
                            tv.setTypeface(null, android.graphics.Typeface.BOLD);
                            tv.setTextColor(getResources().getColor(android.R.color.black));
                            tv.setPadding(8, 8, 8, 8);
                            categoryBreakdownContainer.addView(tv);
                        }
                    }

                    // Populate Member Spending Summary
                    memberInsightsContainer.removeAllViews();
                    if (data.getMemberInsights() != null) {
                        for (GroupInsightsResponse.MemberInsight mi : data.getMemberInsights()) {
                            TextView tv = new TextView(GroupInsightsActivity.this);
                            tv.setText("• " + mi.getFullName() + "\n  Total Paid: " + CurrencyUtils.format(mi.getTotalPaid()) + " | Total Share: " + CurrencyUtils.format(mi.getTotalShare()));
                            tv.setTextSize(15);
                            tv.setTypeface(null, android.graphics.Typeface.BOLD);
                            tv.setTextColor(getResources().getColor(android.R.color.black));
                            tv.setPadding(8, 8, 8, 8);
                            memberInsightsContainer.addView(tv);
                        }
                    }

                } else {
                    Toast.makeText(GroupInsightsActivity.this, "Failed to load group insights", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GroupInsightsResponse> call, Throwable t) {
                Toast.makeText(GroupInsightsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
