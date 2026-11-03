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
import com.example.spliteasy.api.EventRequest;
import com.example.spliteasy.api.GroupEvent;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PartyEventPlannerActivity extends AppCompatActivity {

    private Long groupId, userId;
    private ListView eventsListView;
    private ArrayAdapter<GroupEvent> adapter;
    private final List<GroupEvent> eventList = new ArrayList<>();

    private EditText eventTitleInput, eventDescInput, eventDateInput, eventTimeInput, venueInput;
    private Button saveEventButton, cancelEditButton;
    private TextView formTitleTextView;
    private GroupEvent editingEvent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party_event_planner);

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

        eventTitleInput = findViewById(R.id.eventTitleInput);
        eventDescInput = findViewById(R.id.eventDescInput);
        eventDateInput = findViewById(R.id.eventDateInput);
        eventTimeInput = findViewById(R.id.eventTimeInput);
        venueInput = findViewById(R.id.venueInput);
        saveEventButton = findViewById(R.id.saveEventButton);
        cancelEditButton = findViewById(R.id.cancelEditButton);
        formTitleTextView = findViewById(R.id.formTitleTextView);
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
                startActivity(new Intent(PartyEventPlannerActivity.this, HomeActivity.class));
                finish();
            });
        }

        if (navCreateGroup != null) {
            navCreateGroup.setOnClickListener(v -> {
                Intent intent = new Intent(PartyEventPlannerActivity.this, CreateGroupActivity.class);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
                finish();
            });
        }

        if (navMoney != null) {
            navMoney.setOnClickListener(v -> {
                startActivity(new Intent(PartyEventPlannerActivity.this, DashboardActivity.class));
                finish();
            });
        }

        adapter = new ArrayAdapter<GroupEvent>(this, android.R.layout.simple_list_item_1, eventList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                GroupEvent ev = getItem(position);
                if (ev != null) {
                    view.setText("🎉 " + ev.getTitle() + (ev.getVenue() != null && !ev.getVenue().isEmpty() ? " @ " + ev.getVenue() : "") + "\nDate: " + (ev.getEventDate() != null ? ev.getEventDate() : "TBD") + (ev.getEventTime() != null && !ev.getEventTime().isEmpty() ? " " + ev.getEventTime() : "") + (ev.getDescription() != null && !ev.getDescription().isEmpty() ? "\nNotes: " + ev.getDescription() : ""));
                }
                view.setTypeface(null, Typeface.BOLD);
                view.setTextColor(Color.BLACK);
                view.setTextSize(15);
                view.setPadding(12, 12, 12, 12);
                return view;
            }
        };
        eventsListView.setAdapter(adapter);

        eventsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            GroupEvent ev = eventList.get(position);
            CharSequence[] options = {"Edit Event", "Delete Event"};
            new AlertDialog.Builder(this)
                    .setTitle("Manage Event: " + ev.getTitle())
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            startEditingEvent(ev);
                        } else if (which == 1) {
                            confirmAndDeleteEvent(ev);
                        }
                    })
                    .show();
            return true;
        });

        saveEventButton.setOnClickListener(v -> {
            String title = eventTitleInput.getText().toString().trim();
            String desc = eventDescInput.getText().toString().trim();
            String rawDate = eventDateInput.getText().toString().trim();
            String time = eventTimeInput.getText().toString().trim();
            String venue = venueInput.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter event title", Toast.LENGTH_SHORT).show();
                return;
            }

            String eventDate = null;
            if (!rawDate.isEmpty()) {
                eventDate = parseAndValidateDate(rawDate);
                if (eventDate == null) {
                    Toast.makeText(this, "Invalid date. Please use YYYY-MM-DD or YYYYMMDD.", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            saveEventButton.setEnabled(false);
            EventRequest req = new EventRequest(title, desc.isEmpty() ? null : desc, eventDate, time.isEmpty() ? null : time, venue.isEmpty() ? null : venue, userId);

            if (editingEvent == null) {
                ApiClient.getApiService().createEvent(groupId, req).enqueue(new Callback<GroupEvent>() {
                    @Override
                    public void onResponse(Call<GroupEvent> call, Response<GroupEvent> response) {
                        saveEventButton.setEnabled(true);
                        if (response.isSuccessful()) {
                            clearForm();
                            Toast.makeText(PartyEventPlannerActivity.this, "Event created successfully!", Toast.LENGTH_SHORT).show();
                            loadEvents();
                        } else {
                            String err = "Failed to create event (Code: " + response.code() + ")";
                            if (response.errorBody() != null) {
                                try (ResponseBody eb = response.errorBody()) {
                                    err = "Failed to create event: " + eb.string() + " (Code: " + response.code() + ")";
                                } catch (Exception ignored) {}
                            }
                            Toast.makeText(PartyEventPlannerActivity.this, err, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<GroupEvent> call, Throwable t) {
                        saveEventButton.setEnabled(true);
                        Toast.makeText(PartyEventPlannerActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                ApiClient.getApiService().updateEvent(groupId, editingEvent.getId(), req).enqueue(new Callback<GroupEvent>() {
                    @Override
                    public void onResponse(Call<GroupEvent> call, Response<GroupEvent> response) {
                        saveEventButton.setEnabled(true);
                        if (response.isSuccessful()) {
                            clearForm();
                            Toast.makeText(PartyEventPlannerActivity.this, "Event updated successfully!", Toast.LENGTH_SHORT).show();
                            loadEvents();
                        } else {
                            String err = "Failed to update event (Code: " + response.code() + ")";
                            if (response.errorBody() != null) {
                                try (ResponseBody eb = response.errorBody()) {
                                    err = "Failed to update event: " + eb.string() + " (Code: " + response.code() + ")";
                                } catch (Exception ignored) {}
                            }
                            Toast.makeText(PartyEventPlannerActivity.this, err, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<GroupEvent> call, Throwable t) {
                        saveEventButton.setEnabled(true);
                        Toast.makeText(PartyEventPlannerActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        cancelEditButton.setOnClickListener(v -> clearForm());

        loadEvents();
    }

    private void startEditingEvent(GroupEvent ev) {
        editingEvent = ev;
        formTitleTextView.setText("Edit Event: " + ev.getTitle());
        eventTitleInput.setText(ev.getTitle());
        eventDescInput.setText(ev.getDescription() != null ? ev.getDescription() : "");
        eventDateInput.setText(ev.getEventDate() != null ? ev.getEventDate() : "");
        eventTimeInput.setText(ev.getEventTime() != null ? ev.getEventTime() : "");
        venueInput.setText(ev.getVenue() != null ? ev.getVenue() : "");
        saveEventButton.setText("Update Event");
        cancelEditButton.setVisibility(View.VISIBLE);
    }

    private void clearForm() {
        editingEvent = null;
        formTitleTextView.setText("Create New Event");
        eventTitleInput.setText("");
        eventDescInput.setText("");
        eventDateInput.setText("");
        eventTimeInput.setText("");
        venueInput.setText("");
        saveEventButton.setText("Create Event");
        cancelEditButton.setVisibility(View.GONE);
    }

    private void confirmAndDeleteEvent(GroupEvent ev) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete event '" + ev.getTitle() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    ApiClient.getApiService().deleteEvent(groupId, ev.getId(), userId).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(PartyEventPlannerActivity.this, "Event deleted successfully", Toast.LENGTH_SHORT).show();
                                if (editingEvent != null && editingEvent.getId().equals(ev.getId())) {
                                    clearForm();
                                }
                                loadEvents();
                            } else {
                                String err = "Failed to delete event (Code: " + response.code() + ")";
                                if (response.errorBody() != null) {
                                    try (ResponseBody eb = response.errorBody()) {
                                        err = "Failed to delete event: " + eb.string() + " (Code: " + response.code() + ")";
                                    } catch (Exception ignored) {}
                                }
                                Toast.makeText(PartyEventPlannerActivity.this, err, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(PartyEventPlannerActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
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
                Toast.makeText(PartyEventPlannerActivity.this, "Failed to load events", Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onResume() {
        super.onResume();
        if (SessionManager.needsPinUnlock(this)) {
            Intent intent = new Intent(PartyEventPlannerActivity.this, PinLockActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
