package com.example.spliteasy;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.GroupSubscription;
import com.example.spliteasy.api.SubscriptionRequest;
import com.example.spliteasy.api.UserDto;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubscriptionsActivity extends AppCompatActivity {

    private Long groupId, userId;
    private Spinner subPayerSpinner;
    private ListView subscriptionsListView;
    private ArrayAdapter<GroupSubscription> adapter;
    private final List<GroupSubscription> subscriptionList = new ArrayList<>();
    private List<UserDto> memberList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscriptions);

        groupId = getIntent().getLongExtra("GROUP_ID", -1L);
        userId = getIntent().getLongExtra("USER_ID", -1L);

        EditText subNameInput = findViewById(R.id.subNameInput);
        EditText subCostInput = findViewById(R.id.subCostInput);
        EditText subRenewalInput = findViewById(R.id.subRenewalInput);
        subPayerSpinner = findViewById(R.id.subPayerSpinner);
        Button createSubButton = findViewById(R.id.createSubButton);
        subscriptionsListView = findViewById(R.id.subscriptionsListView);

        loadMembers();

        adapter = new ArrayAdapter<GroupSubscription>(this, 0, subscriptionList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item_list_delete, parent, false);
                }
                TextView view = convertView.findViewById(R.id.itemTextView);
                Button deleteBtn = convertView.findViewById(R.id.itemDeleteButton);

                GroupSubscription s = getItem(position);
                if (s != null) {
                    view.setText(s.getSubscriptionName() + " — " + CurrencyUtils.format(s.getCost()) + "\nRenewal: " + (s.getRenewalDate() != null ? s.getRenewalDate() : "N/A") + " | Paid by: " + s.getPayerName());
                    deleteBtn.setOnClickListener(v -> confirmAndDelete(s));
                }
                return convertView;
            }
        };
        subscriptionsListView.setAdapter(adapter);

        createSubButton.setOnClickListener(v -> {
            String name = subNameInput.getText().toString().trim();
            String costStr = subCostInput.getText().toString().trim();
            String rawRenewal = subRenewalInput.getText().toString().trim();

            if (name.isEmpty() || costStr.isEmpty()) {
                Toast.makeText(this, "Please enter subscription name and cost", Toast.LENGTH_SHORT).show();
                return;
            }

            double cost;
            try {
                cost = Double.parseDouble(costStr);
                if (cost <= 0) {
                    Toast.makeText(this, "Cost must be greater than zero", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid cost", Toast.LENGTH_SHORT).show();
                return;
            }

            String renewal = null;
            if (!rawRenewal.isEmpty()) {
                renewal = parseAndValidateDate(rawRenewal);
                if (renewal == null) {
                    Toast.makeText(this, "Invalid renewal date. Please use YYYY-MM-DD or YYYYMMDD.", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            int spinnerPos = subPayerSpinner.getSelectedItemPosition();
            if (memberList == null || memberList.isEmpty() || spinnerPos < 0 || spinnerPos >= memberList.size()) {
                Toast.makeText(this, "Select paying member", Toast.LENGTH_SHORT).show();
                return;
            }

            UserDto payer = memberList.get(spinnerPos);

            createSubButton.setEnabled(false);
            SubscriptionRequest req = new SubscriptionRequest(name, cost, renewal, payer.getId());

            ApiClient.getApiService().createSubscription(groupId, req).enqueue(new Callback<GroupSubscription>() {
                @Override
                public void onResponse(Call<GroupSubscription> call, Response<GroupSubscription> response) {
                    createSubButton.setEnabled(true);
                    if (response.isSuccessful()) {
                        subNameInput.setText("");
                        subCostInput.setText("");
                        subRenewalInput.setText("");
                        Toast.makeText(SubscriptionsActivity.this, "Subscription added!", Toast.LENGTH_SHORT).show();
                        loadSubscriptions();
                    } else {
                        String err = "Failed to add subscription (Code: " + response.code() + ")";
                        if (response.errorBody() != null) {
                            try (ResponseBody eb = response.errorBody()) {
                                err = "Failed to add subscription: " + eb.string() + " (Code: " + response.code() + ")";
                            } catch (Exception ignored) {}
                        }
                        Toast.makeText(SubscriptionsActivity.this, err, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<GroupSubscription> call, Throwable t) {
                    createSubButton.setEnabled(true);
                    Toast.makeText(SubscriptionsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        loadSubscriptions();
    }

    private void confirmAndDelete(GroupSubscription s) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Subscription")
                .setMessage("Are you sure you want to delete subscription '" + s.getSubscriptionName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    ApiClient.getApiService().deleteSubscription(s.getId(), userId).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(SubscriptionsActivity.this, "Subscription deleted successfully", Toast.LENGTH_SHORT).show();
                                loadSubscriptions();
                            } else {
                                String err = "Failed to delete subscription (Code: " + response.code() + ")";
                                if (response.errorBody() != null) {
                                    try (ResponseBody eb = response.errorBody()) {
                                        err = "Failed to delete subscription: " + eb.string() + " (Code: " + response.code() + ")";
                                    } catch (Exception ignored) {}
                                }
                                Toast.makeText(SubscriptionsActivity.this, err, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(SubscriptionsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadMembers() {
        ApiClient.getApiService().getGroupMembers(groupId).enqueue(new Callback<List<UserDto>>() {
            @Override
            public void onResponse(Call<List<UserDto>> call, Response<List<UserDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    memberList = response.body();
                    ArrayAdapter<UserDto> adapter = new ArrayAdapter<>(SubscriptionsActivity.this, android.R.layout.simple_spinner_item, memberList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    subPayerSpinner.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<UserDto>> call, Throwable t) {}
        });
    }

    private void loadSubscriptions() {
        ApiClient.getApiService().getSubscriptions(groupId, userId).enqueue(new Callback<List<GroupSubscription>>() {
            @Override
            public void onResponse(Call<List<GroupSubscription>> call, Response<List<GroupSubscription>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    subscriptionList.clear();
                    subscriptionList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<GroupSubscription>> call, Throwable t) {
                Toast.makeText(SubscriptionsActivity.this, "Failed to load subscriptions", Toast.LENGTH_SHORT).show();
            }
        });
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
}
