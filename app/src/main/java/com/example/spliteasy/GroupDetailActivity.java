package com.example.spliteasy;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.AddMemberRequest;
import com.example.spliteasy.api.BalanceResponse;
import com.example.spliteasy.api.Expense;
import com.example.spliteasy.api.MemberBalance;
import com.example.spliteasy.api.PaymentReminder;
import com.example.spliteasy.api.RemindRequest;
import com.example.spliteasy.api.Settlement;
import com.example.spliteasy.api.SuggestedDebt;
import com.example.spliteasy.api.UserDto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupDetailActivity extends AppCompatActivity {

    private Long groupId;
    private String groupName;
    private Long userId;

    private LinearLayout membersContainer, balancesContainer, suggestedDebtsContainer, historyContainer;
    private final Map<Long, String> memberNameMap = new HashMap<>();
    private final Map<Long, String> recentReminderMap = new HashMap<>(); // recipientUserId -> formatted reminder time
    private final Set<Long> membersWhoOweUser = new HashSet<>(); // recipientUserId who owe money directly to logged-in user

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        groupId = getIntent().getLongExtra("GROUP_ID", -1L);
        groupName = getIntent().getStringExtra("GROUP_NAME");
        userId = getIntent().getLongExtra("USER_ID", -1L);

        TextView groupTitleTextView = findViewById(R.id.groupTitleTextView);
        if (groupName != null) {
            groupTitleTextView.setText(groupName);
        }

        EditText memberEmailInput = findViewById(R.id.memberEmailInput);
        Button addMemberButton = findViewById(R.id.addMemberButton);
        Button addExpenseButton = findViewById(R.id.addExpenseButton);
        Button recordSettlementButton = findViewById(R.id.recordSettlementButton);
        Button insightsButton = findViewById(R.id.insightsButton);
        Button deleteGroupButton = findViewById(R.id.deleteGroupButton);

        membersContainer = findViewById(R.id.membersContainer);
        balancesContainer = findViewById(R.id.balancesContainer);
        suggestedDebtsContainer = findViewById(R.id.suggestedDebtsContainer);
        historyContainer = findViewById(R.id.historyContainer);

        setupInnerScrollListener(findViewById(R.id.membersScrollView));
        setupInnerScrollListener(findViewById(R.id.balancesScrollView));
        setupInnerScrollListener(findViewById(R.id.suggestedDebtsScrollView));
        setupInnerScrollListener(findViewById(R.id.historyScrollView));

        addMemberButton.setOnClickListener(v -> {
            String email = memberEmailInput.getText().toString().trim().toLowerCase();
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter member email", Toast.LENGTH_SHORT).show();
                return;
            }

            addMemberButton.setEnabled(false);
            ApiClient.getApiService().addMemberByEmail(groupId, new AddMemberRequest(email)).enqueue(new Callback<UserDto>() {
                @Override
                public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                    addMemberButton.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(GroupDetailActivity.this, "Member added successfully!", Toast.LENGTH_SHORT).show();
                        memberEmailInput.setText("");
                        loadGroupData();
                    } else {
                        String errMsg = "Failed to add member";
                        if (response.code() == 409) errMsg = "User is already a member";
                        else if (response.code() == 404) errMsg = "User not found with this email";
                        Toast.makeText(GroupDetailActivity.this, errMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<UserDto> call, Throwable t) {
                    addMemberButton.setEnabled(true);
                    Toast.makeText(GroupDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });

        addExpenseButton.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDetailActivity.this, AddExpenseActivity.class);
            intent.putExtra("GROUP_ID", groupId);
            startActivity(intent);
        });

        recordSettlementButton.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDetailActivity.this, RecordSettlementActivity.class);
            intent.putExtra("GROUP_ID", groupId);
            startActivity(intent);
        });

        insightsButton.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDetailActivity.this, GroupInsightsActivity.class);
            intent.putExtra("GROUP_ID", groupId);
            intent.putExtra("GROUP_NAME", groupName);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        deleteGroupButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Group")
                    .setMessage("Are you sure you want to delete this group permanently? All associated expenses and settlements will be removed.")
                    .setPositiveButton("Delete", (dialog, which) -> deleteGroup())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void setupInnerScrollListener(View scrollView) {
        if (scrollView != null) {
            scrollView.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                if ((event.getAction() & android.view.MotionEvent.ACTION_MASK) == android.view.MotionEvent.ACTION_UP) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return false;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGroupData();
    }

    private void loadGroupData() {
        if (groupId == -1L) return;

        ApiClient.getApiService().getGroupMembers(groupId).enqueue(new Callback<List<UserDto>>() {
            @Override
            public void onResponse(Call<List<UserDto>> call, Response<List<UserDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    membersContainer.removeAllViews();
                    memberNameMap.clear();
                    List<UserDto> members = response.body();
                    if (members.isEmpty()) {
                        addTextView(membersContainer, "No members in this group yet.");
                    } else {
                        for (UserDto m : members) {
                            memberNameMap.put(m.getId(), m.getFullName());
                            TextView tv = new TextView(GroupDetailActivity.this);
                            tv.setText("• " + m.getFullName() + " (" + m.getEmail() + ")");
                            tv.setTextSize(15);
                            tv.setTypeface(null, Typeface.BOLD);
                            tv.setTextColor(Color.BLACK);
                            tv.setPadding(8, 8, 8, 8);
                            tv.setOnClickListener(v -> {
                                new AlertDialog.Builder(GroupDetailActivity.this)
                                        .setTitle("Remove Member")
                                        .setMessage("Are you sure you want to remove " + m.getFullName() + " from this group?")
                                        .setPositiveButton("Remove", (dialog, which) -> removeMember(m.getId()))
                                        .setNegativeButton("Cancel", null)
                                        .show();
                            });
                            membersContainer.addView(tv);
                        }
                    }
                    loadRemindersStatusAndBalances();
                }
            }

            @Override
            public void onFailure(Call<List<UserDto>> call, Throwable t) {}
        });
    }

    private void loadRemindersStatusAndBalances() {
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
                loadBalancesAndHistory();
            }

            @Override
            public void onFailure(Call<List<PaymentReminder>> call, Throwable t) {
                loadBalancesAndHistory();
            }
        });
    }

    private void loadBalancesAndHistory() {
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
                            View cardView = LayoutInflater.from(GroupDetailActivity.this).inflate(R.layout.item_member_balance, balancesContainer, false);
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
                                                            Toast.makeText(GroupDetailActivity.this, "Payment reminder sent successfully!", Toast.LENGTH_SHORT).show();
                                                            remindButton.setVisibility(View.GONE);
                                                            reminderStatusTextView.setVisibility(View.VISIBLE);
                                                            reminderStatusTextView.setText("Reminder Sent (Available after 24 hours)");
                                                            recentReminderMap.put(mb.getUserId(), "just now");
                                                        } else {
                                                            String err = "Cooldown active or cannot send reminder.";
                                                            if (resp.errorBody() != null) {
                                                                try {
                                                                    err = resp.errorBody().string();
                                                                } catch (Exception ignored) {}
                                                            }
                                                            Toast.makeText(GroupDetailActivity.this, err, Toast.LENGTH_LONG).show();
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                                                        remindButton.setEnabled(true);
                                                        Toast.makeText(GroupDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
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

        loadTransactionHistory();
    }

    private void loadTransactionHistory() {
        historyContainer.removeAllViews();

        ApiClient.getApiService().getGroupExpenses(groupId).enqueue(new Callback<List<Expense>>() {
            @Override
            public void onResponse(Call<List<Expense>> call, Response<List<Expense>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Expense> expenses = response.body();
                    if (!expenses.isEmpty()) {
                        for (Expense e : expenses) {
                            String payerName = memberNameMap.getOrDefault(e.getPaidByUserId(), "Member #" + e.getPaidByUserId());
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
                            String payerName = memberNameMap.getOrDefault(s.getPayerUserId(), "Member #" + s.getPayerUserId());
                            if (s.getPayerUserId().equals(userId)) payerName = "You";

                            String payeeName = memberNameMap.getOrDefault(s.getPayeeUserId(), "Member #" + s.getPayeeUserId());
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

    private void removeMember(Long memberUserId) {
        ApiClient.getApiService().removeMember(groupId, memberUserId, userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(GroupDetailActivity.this, "Member removed successfully", Toast.LENGTH_SHORT).show();
                    loadGroupData();
                } else {
                    String err = "Cannot remove member";
                    if (response.errorBody() != null) {
                        try {
                            err = response.errorBody().string();
                        } catch (Exception ignored) {}
                    }
                    Toast.makeText(GroupDetailActivity.this, err, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(GroupDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteGroup() {
        ApiClient.getApiService().deleteGroup(groupId, userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(GroupDetailActivity.this, "Group deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String err = "Failed to delete group";
                    if (response.errorBody() != null) {
                        try {
                            err = response.errorBody().string();
                        } catch (Exception ignored) {}
                    }
                    Toast.makeText(GroupDetailActivity.this, "Failed to delete group", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(GroupDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
