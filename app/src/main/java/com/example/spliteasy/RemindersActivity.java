package com.example.spliteasy;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.PaymentReminder;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RemindersActivity extends AppCompatActivity {

    private Long userId;
    private ListView remindersListView;
    private ArrayAdapter<PaymentReminder> remindersAdapter;
    private List<PaymentReminder> reminderList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        userId = getIntent().getLongExtra("USER_ID", -1L);
        if (userId == -1L) {
            userId = SessionManager.getUserId(this);
        }

        remindersListView = findViewById(R.id.remindersListView);
        reminderList = new ArrayList<>();

        remindersAdapter = new ArrayAdapter<PaymentReminder>(this, android.R.layout.simple_list_item_1, reminderList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                PaymentReminder r = getItem(position);
                if (r != null) {
                    String text = "Group: " + r.getGroupName() + "\nPending Amount: " + CurrencyUtils.format(r.getAmount()) + "\nPlease settle when convenient.";
                    view.setText(text);
                }
                view.setTypeface(null, Typeface.BOLD);
                view.setTextColor(Color.BLACK);
                view.setTextSize(15);
                view.setPadding(12, 12, 12, 12);
                return view;
            }
        };

        remindersListView.setAdapter(remindersAdapter);

        remindersListView.setOnItemClickListener((parent, view, position, id) -> {
            PaymentReminder r = reminderList.get(position);
            // Mark viewed
            ApiClient.getApiService().markReminderViewed(r.getId()).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });

            Intent intent = new Intent(RemindersActivity.this, GroupDetailActivity.class);
            intent.putExtra("GROUP_ID", r.getGroupId());
            intent.putExtra("GROUP_NAME", r.getGroupName());
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        loadReminders();
    }

    private void loadReminders() {
        ApiClient.getApiService().getRemindersForUser(userId).enqueue(new Callback<List<PaymentReminder>>() {
            @Override
            public void onResponse(Call<List<PaymentReminder>> call, Response<List<PaymentReminder>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    reminderList.clear();
                    reminderList.addAll(response.body());
                    remindersAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(RemindersActivity.this, "Failed to load reminders", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<PaymentReminder>> call, Throwable t) {
                Toast.makeText(RemindersActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
