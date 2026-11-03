package com.example.spliteasy;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.GroupEvent;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EventListForGuestsActivity extends AppCompatActivity {

    private Long groupId, userId;
    private ListView eventsListView;
    private ArrayAdapter<GroupEvent> adapter;
    private final List<GroupEvent> eventList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list_for_guests);

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

        eventsListView = findViewById(R.id.eventsListView);

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
                startActivity(new Intent(EventListForGuestsActivity.this, HomeActivity.class));
                finish();
            });
        }

        if (navCreateGroup != null) {
            navCreateGroup.setOnClickListener(v -> {
                Intent intent = new Intent(EventListForGuestsActivity.this, CreateGroupActivity.class);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
                finish();
            });
        }

        if (navMoney != null) {
            navMoney.setOnClickListener(v -> {
                startActivity(new Intent(EventListForGuestsActivity.this, DashboardActivity.class));
                finish();
            });
        }

        adapter = new ArrayAdapter<GroupEvent>(this, android.R.layout.simple_list_item_1, eventList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                GroupEvent ev = getItem(position);
                if (ev != null) {
                    view.setText("🎉 " + ev.getTitle() + (ev.getVenue() != null && !ev.getVenue().isEmpty() ? " @ " + ev.getVenue() : "") + "\nDate: " + (ev.getEventDate() != null ? ev.getEventDate() : "TBD"));
                }
                view.setTypeface(null, Typeface.BOLD);
                view.setTextColor(Color.BLACK);
                view.setTextSize(15);
                view.setPadding(12, 12, 12, 12);
                return view;
            }
        };
        eventsListView.setAdapter(adapter);

        eventsListView.setOnItemClickListener((parent, view, position, id) -> {
            GroupEvent ev = eventList.get(position);
            Intent intent = new Intent(EventListForGuestsActivity.this, GuestListRsvpActivity.class);
            intent.putExtra("EVENT_ID", ev.getId());
            intent.putExtra("EVENT_TITLE", ev.getTitle());
            intent.putExtra("GROUP_ID", groupId);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        loadEvents();
    }

    private void loadEvents() {
        ApiClient.getApiService().getEvents(groupId, userId).enqueue(new Callback<List<GroupEvent>>() {
            @Override
            public void onResponse(Call<List<GroupEvent>> call, Response<List<GroupEvent>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    eventList.clear();
                    eventList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<GroupEvent>> call, Throwable t) {
                Toast.makeText(EventListForGuestsActivity.this, "Failed to load events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SessionManager.needsPinUnlock(this)) {
            Intent intent = new Intent(EventListForGuestsActivity.this, PinLockActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
