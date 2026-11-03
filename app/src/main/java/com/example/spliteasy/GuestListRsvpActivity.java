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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.EventGuest;
import com.example.spliteasy.api.GuestRequest;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GuestListRsvpActivity extends AppCompatActivity {

    private Long eventId, groupId, userId;
    private String eventTitle;
    private TextView eventTitleTextView, rsvpSummaryTextView;
    private ListView guestsListView;
    private ArrayAdapter<EventGuest> guestAdapter;
    private final List<EventGuest> guestList = new ArrayList<>();
    private Spinner rsvpStatusSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_list_rsvp);

        eventId = getIntent().getLongExtra("EVENT_ID", -1L);
        eventTitle = getIntent().getStringExtra("EVENT_TITLE");
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

        eventTitleTextView = findViewById(R.id.eventTitleTextView);
        rsvpSummaryTextView = findViewById(R.id.rsvpSummaryTextView);
        if (eventTitle != null) {
            eventTitleTextView.setText("Guests: " + eventTitle);
        }

        EditText guestNameInput = findViewById(R.id.guestNameInput);
        EditText guestEmailInput = findViewById(R.id.guestEmailInput);
        rsvpStatusSpinner = findViewById(R.id.rsvpStatusSpinner);
        Button addGuestButton = findViewById(R.id.addGuestButton);
        guestsListView = findViewById(R.id.guestsListView);

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"PENDING", "ACCEPTED", "DECLINED"});
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rsvpStatusSpinner.setAdapter(statusAdapter);

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
                startActivity(new Intent(GuestListRsvpActivity.this, HomeActivity.class));
                finish();
            });
        }

        if (navCreateGroup != null) {
            navCreateGroup.setOnClickListener(v -> {
                Intent intent = new Intent(GuestListRsvpActivity.this, CreateGroupActivity.class);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
                finish();
            });
        }

        if (navMoney != null) {
            navMoney.setOnClickListener(v -> {
                startActivity(new Intent(GuestListRsvpActivity.this, DashboardActivity.class));
                finish();
            });
        }

        guestAdapter = new ArrayAdapter<EventGuest>(this, android.R.layout.simple_list_item_1, guestList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                EventGuest g = getItem(position);
                if (g != null) {
                    String statusEmoji = "⏳";
                    if ("ACCEPTED".equalsIgnoreCase(g.getRsvpStatus())) statusEmoji = "✅";
                    else if ("DECLINED".equalsIgnoreCase(g.getRsvpStatus())) statusEmoji = "❌";

                    view.setText("👤 " + g.getGuestName() + (g.getEmail() != null && !g.getEmail().isEmpty() ? " (" + g.getEmail() + ")" : "") + "\nRSVP: " + statusEmoji + " " + g.getRsvpStatus());
                }
                view.setTypeface(null, Typeface.BOLD);
                view.setTextColor(Color.BLACK);
                view.setTextSize(14);
                view.setPadding(10, 10, 10, 10);
                return view;
            }
        };
        guestsListView.setAdapter(guestAdapter);

        guestsListView.setOnItemClickListener((parent, view, position, id) -> {
            EventGuest g = guestList.get(position);
            CharSequence[] statuses = {"PENDING", "ACCEPTED", "DECLINED"};
            new AlertDialog.Builder(this)
                    .setTitle("Update RSVP for " + g.getGuestName())
                    .setItems(statuses, (dialog, which) -> {
                        String newStatus = (String) statuses[which];
                        GuestRequest req = new GuestRequest(userId, g.getGuestName(), g.getEmail(), newStatus);
                        ApiClient.getApiService().updateRsvpStatus(groupId, eventId, g.getId(), req).enqueue(new Callback<EventGuest>() {
                            @Override
                            public void onResponse(Call<EventGuest> call, Response<EventGuest> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(GuestListRsvpActivity.this, "RSVP updated!", Toast.LENGTH_SHORT).show();
                                    loadGuests();
                                } else {
                                    Toast.makeText(GuestListRsvpActivity.this, "Failed to update RSVP", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<EventGuest> call, Throwable t) {
                                Toast.makeText(GuestListRsvpActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .show();
        });

        guestsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            EventGuest g = guestList.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Remove Guest")
                    .setMessage("Are you sure you want to remove '" + g.getGuestName() + "'?")
                    .setPositiveButton("Remove", (dialog, which) -> {
                        ApiClient.getApiService().deleteGuest(groupId, eventId, g.getId(), userId).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(GuestListRsvpActivity.this, "Guest removed", Toast.LENGTH_SHORT).show();
                                    loadGuests();
                                } else {
                                    Toast.makeText(GuestListRsvpActivity.this, "Failed to remove guest", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Toast.makeText(GuestListRsvpActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        addGuestButton.setOnClickListener(v -> {
            String name = guestNameInput.getText().toString().trim();
            String email = guestEmailInput.getText().toString().trim();
            String status = rsvpStatusSpinner.getSelectedItem().toString();

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter guest name", Toast.LENGTH_SHORT).show();
                return;
            }

            addGuestButton.setEnabled(false);
            GuestRequest req = new GuestRequest(userId, name, email.isEmpty() ? null : email, status);

            ApiClient.getApiService().addGuest(groupId, eventId, req).enqueue(new Callback<EventGuest>() {
                @Override
                public void onResponse(Call<EventGuest> call, Response<EventGuest> response) {
                    addGuestButton.setEnabled(true);
                    if (response.isSuccessful()) {
                        guestNameInput.setText("");
                        guestEmailInput.setText("");
                        rsvpStatusSpinner.setSelection(0);
                        Toast.makeText(GuestListRsvpActivity.this, "Guest added successfully!", Toast.LENGTH_SHORT).show();
                        loadGuests();
                    } else {
                        String err = "Failed to add guest (Code: " + response.code() + ")";
                        if (response.errorBody() != null) {
                            try (ResponseBody eb = response.errorBody()) {
                                err = "Failed to add guest: " + eb.string() + " (Code: " + response.code() + ")";
                            } catch (Exception ignored) {}
                        }
                        Toast.makeText(GuestListRsvpActivity.this, err, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<EventGuest> call, Throwable t) {
                    addGuestButton.setEnabled(true);
                    Toast.makeText(GuestListRsvpActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        loadGuests();
    }

    private void loadGuests() {
        ApiClient.getApiService().getGuests(groupId, eventId, userId).enqueue(new Callback<List<EventGuest>>() {
            @Override
            public void onResponse(Call<List<EventGuest>> call, Response<List<EventGuest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    guestList.clear();
                    guestList.addAll(response.body());
                    guestAdapter.notifyDataSetChanged();
                    updateRsvpSummary();
                }
            }

            @Override
            public void onFailure(Call<List<EventGuest>> call, Throwable t) {
                Toast.makeText(GuestListRsvpActivity.this, "Failed to load guests", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRsvpSummary() {
        int total = guestList.size();
        int accepted = 0, pending = 0, declined = 0;
        for (EventGuest g : guestList) {
            String s = g.getRsvpStatus();
            if ("ACCEPTED".equalsIgnoreCase(s)) accepted++;
            else if ("DECLINED".equalsIgnoreCase(s)) declined++;
            else pending++;
        }
        rsvpSummaryTextView.setText("Total: " + total + " | Accepted: " + accepted + " ✅ | Pending: " + pending + " ⏳ | Declined: " + declined + " ❌");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SessionManager.needsPinUnlock(this)) {
            Intent intent = new Intent(GuestListRsvpActivity.this, PinLockActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
