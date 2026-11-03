package com.example.spliteasy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.BalanceResponse;
import com.example.spliteasy.api.Expense;
import com.example.spliteasy.api.Group;
import com.example.spliteasy.api.MemberBalance;
import com.example.spliteasy.api.PaymentReminder;
import com.example.spliteasy.api.RemindRequest;
import com.example.spliteasy.api.Settlement;
import com.example.spliteasy.api.SuggestedDebt;
import com.example.spliteasy.api.UserDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 112;
    private Long userId;
    private Spinner moneyGroupSpinner;
    private LinearLayout moneyContentLayout, balancesContainer, suggestedDebtsContainer, historyContainer;
    private List<Group> groupList = new ArrayList<>();
    private Group selectedGroup = null;

    private final Map<Long, String> memberNameMap = new HashMap<>();
    private final Map<Long, String> recentReminderMap = new HashMap<>();
    private final Set<Long> membersWhoOweUser = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

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

        // Check Android 13+ notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }

        TextView welcomeTextView = findViewById(R.id.welcomeTextView);
        moneyGroupSpinner = findViewById(R.id.moneyGroupSpinner);
        moneyContentLayout = findViewById(R.id.moneyContentLayout);
        balancesContainer = findViewById(R.id.balancesContainer);
        suggestedDebtsContainer = findViewById(R.id.suggestedDebtsContainer);
        historyContainer = findViewById(R.id.historyContainer);

        Button addExpenseButton = findViewById(R.id.addExpenseButton);
        Button recordSettlementButton = findViewById(R.id.recordSettlementButton);
        Button insightsButton = findViewById(R.id.insightsButton);
        Button profileButton = findViewById(R.id.profileButton);
        Button remindersButton = findViewById(R.id.remindersButton);
        Button logoutButton = findViewById(R.id.logoutButton);

        // Bottom Navigation setup (Highlight Money)
        Button navHome = findViewById(R.id.navHomeButton);
        Button navCreateGroup = findViewById(R.id.navCreateGroupButton);
        Button navEvent = findViewById(R.id.navEventButton);
        Button navMoney = findViewById(R.id.navMoneyButton);

        if (navMoney != null) {
            navMoney.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
            navMoney.setOnClickListener(v -> {}); // already money
        }

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, HomeActivity.class));
                finish();
            });
        }

        if (navCreateGroup != null) {
            navCreateGroup.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, CreateGroupActivity.class);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
                finish();
            });
        }

        if (navEvent != null) {
            navEvent.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, EventActivity.class));
                finish();
            });
        }

        String fullName = SessionManager.getFullName(this);
        userId = SessionManager.getUserId(this);

        if (fullName != null && !fullName.isEmpty()) {
            welcomeTextView.setText("Welcome, " + fullName + "!");
        } else {
            welcomeTextView.setText("Welcome!");
        }

        moneyGroupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (groupList != null && position >= 0 && position < groupList.size()) {
                    selectedGroup = groupList.get(position);
                    moneyContentLayout.setVisibility(View.VISIBLE);
                    loadGroupData();
                } else {
                    selectedGroup = null;
                    moneyContentLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedGroup = null;
                moneyContentLayout.setVisibility(View.GONE);
            }
        });

        addExpenseButton.setOnClickListener(v -> {
            if (selectedGroup == null) return;
            Intent intent = new Intent(DashboardActivity.this, AddExpenseActivity.class);
            intent.putExtra("GROUP_ID", selectedGroup.getId());
            startActivity(intent);
        });

        recordSettlementButton.setOnClickListener(v -> {
            if (selectedGroup == null) return;
            Intent intent = new Intent(DashboardActivity.this, RecordSettlementActivity.class);
            intent.putExtra("GROUP_ID", selectedGroup.getId());
            startActivity(intent);
        });

        insightsButton.setOnClickListener(v -> {
            if (selectedGroup == null) return;
            Intent intent = new Intent(DashboardActivity.this, GroupInsightsActivity.class);
            intent.putExtra("GROUP_ID", selectedGroup.getId());
            intent.putExtra("GROUP_NAME", selectedGroup.getGroupName());
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        remindersButton.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, RemindersActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                SessionManager.clearSession(this);
                Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SessionManager.needsPinUnlock(this)) {
            Intent intent = new Intent(DashboardActivity.this, PinLockActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        if (userId != null && userId != -1L) {
            loadUserGroups();
            checkForReminders();
        }
    }

    private void loadUserGroups() {
        ApiClient.getApiService().getGroupsForUser(userId).enqueue(new Callback<List<Group>>() {
            @Override
            public void onResponse(Call<List<Group>> call, Response<List<Group>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    groupList = response.body();
                    ArrayAdapter<Group> adapter = new ArrayAdapter<Group>(DashboardActivity.this, R.layout.item_spinner, groupList) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            TextView view = (TextView) super.getView(position, convertView, parent);
                            view.setTypeface(null, Typeface.BOLD);
                            view.setTextColor(Color.BLACK);
                            view.setBackgroundColor(Color.WHITE);
                            view.setTextSize(15);
                            return view;
                        }
                        @Override
                        public View getDropDownView(int position, View convertView, ViewGroup parent) {
                            TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                            view.setTypeface(null, Typeface.BOLD);
                            view.setTextColor(Color.BLACK);
                            view.setBackgroundColor(Color.WHITE);
                            view.setTextSize(15);
                            return view;
                        }
                    };
                    adapter.setDropDownViewResource(R.layout.item_spinner);
                    moneyGroupSpinner.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<Group>> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Cannot connect to server to load groups", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGroupData() {
        if (selectedGroup == null) return;
        Long groupId = selectedGroup.getId();

        ApiClient.getApiService().getGroupMembers(groupId).enqueue(new Callback<List<UserDto>>() {
            @Override
            public void onResponse(Call<List<UserDto>> call, Response<List<UserDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    memberNameMap.clear();
                    for (UserDto m : response.body()) {
                        memberNameMap.put(m.getId(), m.getFullName());
                    }
                    loadRemindersStatusAndBalances(groupId);
                }
            }

            @Override
            public void onFailure(Call<List<UserDto>> call, Throwable t) {}
        });
    }

    private void loadRemindersStatusAndBalances(Long groupId) {
        ApiClient.getApiService().getGroupRemindersStatus(groupId, userId).enqueue(new Callback<List<PaymentReminder>>() {
            @Override
            public void onResponse(Call<List<PaymentReminder>> call, Response<List<PaymentReminder>> response) {
                recentReminderMap.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (PaymentReminder r : response.body()) {
                        String timeStr = r.getReminderDate() != null ? r.getReminderDate().replace("T", " ") : "";
                        if (timeStr.length() > 16) timeStr = timeStr.substring(0, 16);
                        recentReminderMap.put(r.getRecipientUserId(), timeStr);
                    }
                }
                loadBalancesAndHistory(groupId);
            }

            @Override
            public void onFailure(Call<List<PaymentReminder>> call, Throwable t) {
                loadBalancesAndHistory(groupId);
            }
        });
    }

    private void loadBalancesAndHistory(Long groupId) {
        ApiClient.getApiService().getGroupBalances(groupId).enqueue(new Callback<BalanceResponse>() {
            @Override
            public void onResponse(Call<BalanceResponse> call, Response<BalanceResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BalanceResponse data = response.body();
                    balancesContainer.removeAllViews();
                    suggestedDebtsContainer.removeAllViews();

                    membersWhoOweUser.clear();
                    if (data.getSuggestedSettlements() != null) {
                        for (SuggestedDebt sd : data.getSuggestedSettlements()) {
                            if (sd.getCreditorUserId() != null && sd.getCreditorUserId().equals(userId) && sd.getDebtorUserId() != null) {
                                membersWhoOweUser.add(sd.getDebtorUserId());
                            }
                        }
                    }

                    if (data.getMemberBalances() != null && !data.getMemberBalances().isEmpty()) {
                        for (MemberBalance mb : data.getMemberBalances()) {
                            View cardView = LayoutInflater.from(DashboardActivity.this).inflate(R.layout.item_member_balance, balancesContainer, false);
                            TextView nameTv = cardView.findViewById(R.id.cardNameTextView);
                            TextView paidTv = cardView.findViewById(R.id.cardPaidTextView);
                            TextView shareTv = cardView.findViewById(R.id.cardShareTextView);
                            TextView netTv = cardView.findViewById(R.id.cardNetTextView);
                            Button remindButton = cardView.findViewById(R.id.remindButton);
                            TextView reminderStatusTextView = cardView.findViewById(R.id.reminderStatusTextView);

                            nameTv.setText(mb.getFullName());
                            paidTv.setText(CurrencyUtils.format(mb.getTotalPaid() != null ? mb.getTotalPaid() : 0.0));
                            shareTv.setText(CurrencyUtils.format(mb.getTotalShare() != null ? mb.getTotalShare() : 0.0));

                            double net = mb.getNetBalance() != null ? mb.getNetBalance() : 0.0;
                            if (Math.abs(net) < 0.01) {
                                netTv.setText("Settled");
                                netTv.setTextColor(Color.parseColor("#4A5568"));
                            } else if (net > 0) {
                                netTv.setText("Gets back " + CurrencyUtils.format(net));
                                netTv.setTextColor(Color.parseColor("#38A169"));
                            } else {
                                netTv.setText("Owes " + CurrencyUtils.format(Math.abs(net)));
                                netTv.setTextColor(Color.parseColor("#E53E3E"));
                            }

                            boolean owesLoggedInUser = membersWhoOweUser.contains(mb.getUserId()) && !mb.getUserId().equals(userId);

                            if (owesLoggedInUser) {
                                if (recentReminderMap.containsKey(mb.getUserId())) {
                                    remindButton.setVisibility(View.GONE);
                                    reminderStatusTextView.setVisibility(View.VISIBLE);
                                    reminderStatusTextView.setText("Reminder Sent at " + recentReminderMap.get(mb.getUserId()));
                                } else {
                                    remindButton.setVisibility(View.VISIBLE);
                                    reminderStatusTextView.setVisibility(View.GONE);
                                    remindButton.setOnClickListener(v -> {
                                        remindButton.setEnabled(false);
                                        ApiClient.getApiService().sendReminder(groupId, new RemindRequest(userId, mb.getUserId()))
                                                .enqueue(new Callback<ResponseBody>() {
                                                    @Override
                                                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> resp) {
                                                        remindButton.setEnabled(true);
                                                        if (resp.isSuccessful()) {
                                                            Toast.makeText(DashboardActivity.this, "Payment reminder sent successfully!", Toast.LENGTH_SHORT).show();
                                                            remindButton.setVisibility(View.GONE);
                                                            reminderStatusTextView.setVisibility(View.VISIBLE);
                                                            reminderStatusTextView.setText("Reminder Sent");
                                                            recentReminderMap.put(mb.getUserId(), "just now");
                                                        } else {
                                                            Toast.makeText(DashboardActivity.this, "Cannot send reminder", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                                                        remindButton.setEnabled(true);
                                                    }
                                                });
                                    });
                                }
                            } else {
                                remindButton.setVisibility(View.GONE);
                                reminderStatusTextView.setVisibility(View.GONE);
                            }

                            balancesContainer.addView(cardView);
                        }
                    } else {
                        addTextView(balancesContainer, "No balances to display.");
                    }

                    if (data.getSuggestedSettlements() != null && !data.getSuggestedSettlements().isEmpty()) {
                        for (SuggestedDebt sd : data.getSuggestedSettlements()) {
                            addTextView(suggestedDebtsContainer, "• " + sd.getFormattedText(userId));
                        }
                    } else {
                        addTextView(suggestedDebtsContainer, "All members are settled up!");
                    }
                }
            }

            @Override
            public void onFailure(Call<BalanceResponse> call, Throwable t) {}
        });

        loadTransactionHistory(groupId);
    }

    private void loadTransactionHistory(Long groupId) {
        historyContainer.removeAllViews();

        ApiClient.getApiService().getGroupExpenses(groupId).enqueue(new Callback<List<Expense>>() {
            @Override
            public void onResponse(Call<List<Expense>> call, Response<List<Expense>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Expense> expenses = response.body();
                    if (!expenses.isEmpty()) {
                        for (Expense e : expenses) {
                            String payerName = memberNameMap.containsKey(e.getPaidByUserId()) ? memberNameMap.get(e.getPaidByUserId()) : ("Member #" + e.getPaidByUserId());
                            if (e.getPaidByUserId().equals(userId)) {
                                payerName = "You";
                            }
                            addTextView(historyContainer, "[Expense] " + e.getTitle() + ": " + CurrencyUtils.format(e.getAmount()) + "\n(Paid by " + payerName + ")");
                            addDivider(historyContainer);
                        }
                    } else {
                        addTextView(historyContainer, "No expenses recorded yet.");
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Expense>> call, Throwable t) {}
        });

        ApiClient.getApiService().getGroupSettlements(groupId).enqueue(new Callback<List<Settlement>>() {
            @Override
            public void onResponse(Call<List<Settlement>> call, Response<List<Settlement>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Settlement> settlements = response.body();
                    if (!settlements.isEmpty()) {
                        for (Settlement s : settlements) {
                            String payerName = memberNameMap.containsKey(s.getPayerUserId()) ? memberNameMap.get(s.getPayerUserId()) : ("Member #" + s.getPayerUserId());
                            if (s.getPayerUserId().equals(userId)) payerName = "You";

                            String payeeName = memberNameMap.containsKey(s.getPayeeUserId()) ? memberNameMap.get(s.getPayeeUserId()) : ("Member #" + s.getPayeeUserId());
                            if (s.getPayeeUserId().equals(userId)) payeeName = "You";

                            addTextView(historyContainer, "[Settlement] " + payerName + " paid " + payeeName + ": " + CurrencyUtils.format(s.getAmount()));
                            addDivider(historyContainer);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Settlement>> call, Throwable t) {}
        });
    }

    private void addTextView(LinearLayout container, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.BLACK);
        tv.setPadding(8, 8, 8, 8);
        container.addView(tv);
    }

    private void addDivider(LinearLayout container) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        v.setBackgroundColor(Color.parseColor("#E2E8F0"));
        container.addView(v);
    }

    private void checkForReminders() {
        ApiClient.getApiService().getRemindersForUser(userId).enqueue(new Callback<List<PaymentReminder>>() {
            @Override
            public void onResponse(Call<List<PaymentReminder>> call, Response<List<PaymentReminder>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (PaymentReminder r : response.body()) {
                        if (!r.isViewed()) {
                            NotificationHelper.showReminderNotification(DashboardActivity.this, r.getId(), r.getGroupId(), r.getGroupName(), r.getAmount(), userId);
                            ApiClient.getApiService().markReminderViewed(r.getId()).enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}
                                @Override
                                public void onFailure(Call<ResponseBody> call, Throwable t) {}
                            });
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<PaymentReminder>> call, Throwable t) {}
        });
    }
}
