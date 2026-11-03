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

public class EventActivity extends AppCompatActivity {

    private Long userId;
    private Spinner eventGroupSpinner;
    private List<Group> groupList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

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

        userId = SessionManager.getUserId(this);
        eventGroupSpinner = findViewById(R.id.eventGroupSpinner);

        Button cardTripPlanner = findViewById(R.id.cardTripPlanner);
        Button cardTripBudget = findViewById(R.id.cardTripBudget);
        Button cardPartyPlanner = findViewById(R.id.cardPartyPlanner);
        Button cardGuestList = findViewById(R.id.cardGuestList);
        Button cardCalendarPolls = findViewById(R.id.cardCalendarPolls);

        cardTripPlanner.setOnClickListener(v -> openFeature("Trip Planner"));
        cardTripBudget.setOnClickListener(v -> openFeature("Trip Budget"));
        cardPartyPlanner.setOnClickListener(v -> openFeature("Party & Event Planner"));
        cardGuestList.setOnClickListener(v -> openFeature("Guest List & RSVP"));
        cardCalendarPolls.setOnClickListener(v -> openFeature("Shared Calendar & Polls"));

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
                startActivity(new Intent(EventActivity.this, HomeActivity.class));
                finish();
            });
        }

        if (navCreateGroup != null) {
            navCreateGroup.setOnClickListener(v -> {
                Intent intent = new Intent(EventActivity.this, CreateGroupActivity.class);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
                finish();
            });
        }

        if (navMoney != null) {
            navMoney.setOnClickListener(v -> {
                startActivity(new Intent(EventActivity.this, DashboardActivity.class));
                finish();
            });
        }

        loadGroups();
    }

    private void openFeature(String featureName) {
        Group selectedGroup = getSelectedGroup();
        if (selectedGroup == null) {
            Toast.makeText(this, "Please select an active group first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (featureName.equals("Trip Planner")) {
            Intent intent = new Intent(EventActivity.this, TripPlannerActivity.class);
            intent.putExtra("GROUP_ID", selectedGroup.getId());
            intent.putExtra("GROUP_NAME", selectedGroup.getGroupName());
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        } else if (featureName.equals("Trip Budget")) {
            Intent intent = new Intent(EventActivity.this, TripBudgetListActivity.class);
            intent.putExtra("GROUP_ID", selectedGroup.getId());
            intent.putExtra("GROUP_NAME", selectedGroup.getGroupName());
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        } else if (featureName.equals("Party & Event Planner")) {
            Intent intent = new Intent(EventActivity.this, PartyEventPlannerActivity.class);
            intent.putExtra("GROUP_ID", selectedGroup.getId());
            intent.putExtra("GROUP_NAME", selectedGroup.getGroupName());
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        } else if (featureName.equals("Guest List & RSVP")) {
            Intent intent = new Intent(EventActivity.this, EventListForGuestsActivity.class);
            intent.putExtra("GROUP_ID", selectedGroup.getId());
            intent.putExtra("GROUP_NAME", selectedGroup.getGroupName());
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        } else if (featureName.equals("Shared Calendar & Polls")) {
            Intent intent = new Intent(EventActivity.this, CalendarPollsActivity.class);
            intent.putExtra("GROUP_ID", selectedGroup.getId());
            intent.putExtra("GROUP_NAME", selectedGroup.getGroupName());
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        } else {
            Intent intent = new Intent(EventActivity.this, EventFeatureActivity.class);
            intent.putExtra("FEATURE_NAME", featureName);
            intent.putExtra("GROUP_ID", selectedGroup.getId());
            intent.putExtra("GROUP_NAME", selectedGroup.getGroupName());
            startActivity(intent);
        }
    }

    private Group getSelectedGroup() {
        int pos = eventGroupSpinner.getSelectedItemPosition();
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
                    ArrayAdapter<Group> adapter = new ArrayAdapter<Group>(EventActivity.this, R.layout.item_spinner, groupList) {
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
                    eventGroupSpinner.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<Group>> call, Throwable t) {
                Toast.makeText(EventActivity.this, "Failed to load groups", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SessionManager.needsPinUnlock(this)) {
            Intent intent = new Intent(EventActivity.this, PinLockActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
