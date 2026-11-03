package com.example.spliteasy;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.TripExpense;
import com.example.spliteasy.api.TripExpenseRequest;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TripBudgetDetailActivity extends AppCompatActivity {

    private Long tripId, groupId, userId;
    private String tripName;
    private Double totalBudget = 0.0;
    private TextView tripTitleTextView, tripSummaryTextView;
    private ListView expensesListView;
    private ArrayAdapter<TripExpense> expenseAdapter;
    private final List<TripExpense> expenseList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_budget);

        tripId = getIntent().getLongExtra("TRIP_ID", -1L);
        tripName = getIntent().getStringExtra("TRIP_NAME");
        totalBudget = getIntent().getDoubleExtra("TOTAL_BUDGET", 0.0);
        groupId = getIntent().getLongExtra("GROUP_ID", -1L);
        userId = getIntent().getLongExtra("USER_ID", -1L);
        if (userId == -1L) {
            userId = SessionManager.getUserId(this);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            View bottomNav = findViewById(R.id.bottomNavContainer);
            if (bottomNav != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomNav.getLayoutParams();
                params.bottomMargin = insets.bottom;
                bottomNav.setLayoutParams(params);
            }
            View scrollView = findViewById(R.id.mainScrollView);
            if (scrollView != null) {
                float density = getResources().getDisplayMetrics().density;
                int defaultBottomPaddingPx = (int) (64 * density);
                scrollView.setPadding(
                        scrollView.getPaddingLeft(),
                        scrollView.getPaddingTop(),
                        scrollView.getPaddingRight(),
                        defaultBottomPaddingPx + insets.bottom
                );
            }
            return windowInsets;
        });

        tripTitleTextView = findViewById(R.id.tripTitleTextView);
        tripSummaryTextView = findViewById(R.id.tripSummaryTextView);
        if (tripName != null) {
            tripTitleTextView.setText("Budget: " + tripName);
        }

        EditText expenseTitleInput = findViewById(R.id.expenseTitleInput);
        EditText expenseAmountInput = findViewById(R.id.expenseAmountInput);
        EditText expenseCategoryInput = findViewById(R.id.expenseCategoryInput);
        EditText expenseDateInput = findViewById(R.id.expenseDateInput);
        Button addExpenseButton = findViewById(R.id.addExpenseButton);
        expensesListView = findViewById(R.id.expensesListView);

        // Bottom Navigation setup (Highlight Event)
        Button navHome = findViewById(R.id.navHomeButton);
        Button navCreateGroup = findViewById(R.id.navCreateGroupButton);
        Button navEvent = findViewById(R.id.navEventButton);
        Button navMoney = findViewById(R.id.navMoneyButton);

        if (navEvent != null) {
            navEvent.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
            navEvent.setOnClickListener(v -> {}); // already event
        }

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                startActivity(new Intent(TripBudgetDetailActivity.this, HomeActivity.class));
                finish();
            });
        }

        if (navCreateGroup != null) {
            navCreateGroup.setOnClickListener(v -> {
                Intent intent = new Intent(TripBudgetDetailActivity.this, CreateGroupActivity.class);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
                finish();
            });
        }

        if (navMoney != null) {
            navMoney.setOnClickListener(v -> {
                startActivity(new Intent(TripBudgetDetailActivity.this, DashboardActivity.class));
                finish();
            });
        }

        expenseAdapter = new ArrayAdapter<TripExpense>(this, 0, expenseList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item_list_delete, parent, false);
                }
                TextView view = convertView.findViewById(R.id.itemTextView);
                Button deleteBtn = convertView.findViewById(R.id.itemDeleteButton);

                TripExpense e = getItem(position);
                if (e != null) {
                    view.setText(e.getTitle() + " — " + CurrencyUtils.format(e.getAmount()) + "\nCategory: " + (e.getCategory() != null ? e.getCategory() : "General") + " | Paid by: " + e.getPaidByName() + " on " + (e.getExpenseDate() != null ? e.getExpenseDate() : "N/A"));
                    deleteBtn.setOnClickListener(v -> confirmAndDeleteExpense(e));
                }
                return convertView;
            }
        };
        expensesListView.setAdapter(expenseAdapter);

        addExpenseButton.setOnClickListener(v -> {
            String title = expenseTitleInput.getText().toString().trim();
            String amountStr = expenseAmountInput.getText().toString().trim();
            String cat = expenseCategoryInput.getText().toString().trim();
            String rawDate = expenseDateInput.getText().toString().trim();

            if (title.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter title and amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show();
                return;
            }

            String expenseDate = null;
            if (!rawDate.isEmpty()) {
                expenseDate = parseAndValidateDate(rawDate);
                if (expenseDate == null) {
                    Toast.makeText(this, "Invalid date. Please use YYYY-MM-DD or YYYYMMDD.", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            addExpenseButton.setEnabled(false);
            TripExpenseRequest req = new TripExpenseRequest(userId, title, amount, cat.isEmpty() ? null : cat, expenseDate);

            ApiClient.getApiService().addTripExpense(groupId, tripId, req).enqueue(new Callback<TripExpense>() {
                @Override
                public void onResponse(Call<TripExpense> call, Response<TripExpense> response) {
                    addExpenseButton.setEnabled(true);
                    if (response.isSuccessful()) {
                        expenseTitleInput.setText("");
                        expenseAmountInput.setText("");
                        expenseCategoryInput.setText("");
                        expenseDateInput.setText("");
                        Toast.makeText(TripBudgetDetailActivity.this, "Expense added!", Toast.LENGTH_SHORT).show();
                        loadExpenses();
                    } else {
                        String err = "Failed to add expense (Code: " + response.code() + ")";
                        if (response.errorBody() != null) {
                            try (ResponseBody eb = response.errorBody()) {
                                err = "Failed to add expense: " + eb.string() + " (Code: " + response.code() + ")";
                            } catch (Exception ignored) {}
                        }
                        Toast.makeText(TripBudgetDetailActivity.this, err, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<TripExpense> call, Throwable t) {
                    addExpenseButton.setEnabled(true);
                    Toast.makeText(TripBudgetDetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        loadExpenses();
    }

    private void confirmAndDeleteExpense(TripExpense e) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete expense '" + e.getTitle() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    ApiClient.getApiService().deleteTripExpense(groupId, e.getId(), userId).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(TripBudgetDetailActivity.this, "Expense deleted successfully", Toast.LENGTH_SHORT).show();
                                loadExpenses();
                            } else {
                                String err = "Failed to delete expense (Code: " + response.code() + ")";
                                if (response.errorBody() != null) {
                                    try (ResponseBody eb = response.errorBody()) {
                                        err = "Failed to delete expense: " + eb.string() + " (Code: " + response.code() + ")";
                                    } catch (Exception ignored) {}
                                }
                                Toast.makeText(TripBudgetDetailActivity.this, err, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(TripBudgetDetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadExpenses() {
        ApiClient.getApiService().getTripExpenses(groupId, tripId, userId).enqueue(new Callback<List<TripExpense>>() {
            @Override
            public void onResponse(Call<List<TripExpense>> call, Response<List<TripExpense>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    expenseList.clear();
                    expenseList.addAll(response.body());
                    expenseAdapter.notifyDataSetChanged();
                    updateBudgetSummary();
                }
            }

            @Override
            public void onFailure(Call<List<TripExpense>> call, Throwable t) {
                Toast.makeText(TripBudgetDetailActivity.this, "Failed to load expenses", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateBudgetSummary() {
        double totalSpent = 0.0;
        for (TripExpense e : expenseList) {
            totalSpent += e.getAmount();
        }
        double remaining = totalBudget - totalSpent;
        tripSummaryTextView.setText("Budget: " + CurrencyUtils.format(totalBudget) + " | Spent: " + CurrencyUtils.format(totalSpent) + " | Remaining: " + CurrencyUtils.format(remaining));
    }

    private String parseAndValidateDate(String input) {
        if (input == null) return null;
        String dateStr = input.trim();

        if (dateStr.matches("^\\d{8}$")) {
            String year = dateStr.substring(0, 4);
            String month = dateStr.substring(4, 6);
            String day = dateStr.substring(6, 8);
            dateStr = year + "-" + month + "-" + day;
        }

        if (!dateStr.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            return null;
        }

        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            sdf.setLenient(false);
            java.util.Date date = sdf.parse(dateStr);
            if (date == null) return null;

            String formatted = sdf.format(date);
            if (!formatted.equals(dateStr)) {
                return null;
            }
            return dateStr;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SessionManager.needsPinUnlock(this)) {
            Intent intent = new Intent(TripBudgetDetailActivity.this, PinLockActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
