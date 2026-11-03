package com.example.spliteasy;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BirthdaysActivity extends AppCompatActivity {

    private Long groupId, userId;
    private ListView birthdaysListView;
    private ArrayAdapter<Map<String, Object>> adapter;
    private List<Map<String, Object>> birthdayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_birthdays);

        groupId = getIntent().getLongExtra("GROUP_ID", -1L);
        userId = getIntent().getLongExtra("USER_ID", -1L);

        birthdaysListView = findViewById(R.id.birthdaysListView);

        adapter = new ArrayAdapter<Map<String, Object>>(this, android.R.layout.simple_list_item_1, birthdayList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                Map<String, Object> map = getItem(position);
                if (map != null) {
                    String name = (String) map.get("fullName");
                    String bday = (String) map.get("birthday");
                    view.setText("🎂 " + name + " — " + bday);
                }
                view.setTypeface(null, Typeface.BOLD);
                view.setTextColor(Color.BLACK);
                view.setTextSize(16);
                view.setPadding(12, 12, 12, 12);
                return view;
            }
        };
        birthdaysListView.setAdapter(adapter);

        loadBirthdays();
    }

    private void loadBirthdays() {
        ApiClient.getApiService().getGroupBirthdays(groupId, userId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    birthdayList.clear();
                    birthdayList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    if (birthdayList.isEmpty()) {
                        Toast.makeText(BirthdaysActivity.this, "No birthdays added yet. Set your birthday in Profile.", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(BirthdaysActivity.this, "Failed to load birthdays", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
