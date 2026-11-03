package com.example.spliteasy;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.Group;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private Long userId;
    private Spinner homeGroupSpinner;
    private List<Group> groupList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            View bottomNav = findViewById(R.id.bottomNavContainer);
            if (bottomNav != null) {
                android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) bottomNav.getLayoutParams();
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

        userId = SessionManager.getUserId(this);

        TextView homeWelcomeTextView = findViewById(R.id.homeWelcomeTextView);
        homeGroupSpinner = findViewById(R.id.homeGroupSpinner);

        String fullName = SessionManager.getFullName(this);
        if (fullName != null && !fullName.isEmpty()) {
            homeWelcomeTextView.setText("Welcome, " + fullName + "!");
        }

        Button cardAnnouncements = findViewById(R.id.cardAnnouncements);
        Button cardBirthdays = findViewById(R.id.cardBirthdays);
        Button cardDocuments = findViewById(R.id.cardDocuments);
        Button cardSubscriptions = findViewById(R.id.cardSubscriptions);

        cardAnnouncements.setOnClickListener(v -> openFeature(AnnouncementsActivity.class));
        cardBirthdays.setOnClickListener(v -> openFeature(BirthdaysActivity.class));
        cardDocuments.setOnClickListener(v -> openFeature(ShoppingListActivity.class));
        cardSubscriptions.setOnClickListener(v -> openFeature(SubscriptionsActivity.class));

        // Bottom Navigation setup (Highlight Home)
        Button navHome = findViewById(R.id.navHomeButton);
        Button navCreateGroup = findViewById(R.id.navCreateGroupButton);
        Button navEvent = findViewById(R.id.navEventButton);
        Button navMoney = findViewById(R.id.navMoneyButton);

        if (navHome != null) {
            navHome.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
            navHome.setOnClickListener(v -> {}); // already home
        }

        if (navCreateGroup != null) {
            navCreateGroup.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, CreateGroupActivity.class);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
                finish();
            });
        }

        if (navEvent != null) {
            navEvent.setOnClickListener(v -> {
                startActivity(new Intent(HomeActivity.this, EventActivity.class));
                finish();
            });
        }

        if (navMoney != null) {
            navMoney.setOnClickListener(v -> {
                startActivity(new Intent(HomeActivity.this, DashboardActivity.class));
                finish();
            });
        }

        loadGroups();
    }

    private void openFeature(Class<?> targetActivity) {
        Group selectedGroup = getSelectedGroup();
        if (selectedGroup == null) {
            Toast.makeText(this, "Please select a group first", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(HomeActivity.this, targetActivity);
        intent.putExtra("GROUP_ID", selectedGroup.getId());
        intent.putExtra("GROUP_NAME", selectedGroup.getGroupName());
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
    }

    private Group getSelectedGroup() {
        int pos = homeGroupSpinner.getSelectedItemPosition();
        if (groupList != null && pos >= 0 && pos < groupList.size()) {
            return groupList.get(pos);
        }
        return null;
    }

    private void loadGroups() {
        ApiClient.getApiService().getGroupsForUser(userId).enqueue(new Callback<List<Group>>() {
            @Override
            public void onResponse(Call<List<Group>> call, Response<List<Group>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    groupList = response.body();
                    ArrayAdapter<Group> adapter = new ArrayAdapter<Group>(HomeActivity.this, R.layout.item_spinner, groupList) {
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
                    homeGroupSpinner.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<Group>> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Failed to load groups", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SessionManager.needsPinUnlock(this)) {
            Intent intent = new Intent(HomeActivity.this, PinLockActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
