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
import com.example.spliteasy.api.ItineraryRequest;
import com.example.spliteasy.api.TripItinerary;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TripItineraryActivity extends AppCompatActivity {

    private Long tripId, groupId, userId;
    private String tripName;
    private ListView itineraryListView;
    private ArrayAdapter<TripItinerary> itineraryAdapter;
    private final List<TripItinerary> itineraryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_itinerary);

        tripId = getIntent().getLongExtra("TRIP_ID", -1L);
        tripName = getIntent().getStringExtra("TRIP_NAME");
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

        TextView itineraryTripTitle = findViewById(R.id.itineraryTripTitle);
        if (tripName != null) {
            itineraryTripTitle.setText("Itinerary: " + tripName);
        }

        EditText itineraryTitleInput = findViewById(R.id.itineraryTitleInput);
        EditText itineraryDescInput = findViewById(R.id.itineraryDescInput);
        EditText itineraryDateInput = findViewById(R.id.itineraryDateInput);
        Button addItineraryButton = findViewById(R.id.addItineraryButton);
        itineraryListView = findViewById(R.id.itineraryListView);

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
                startActivity(new Intent(TripItineraryActivity.this, HomeActivity.class));
                finish();
            });
        }

        if (navCreateGroup != null) {
            navCreateGroup.setOnClickListener(v -> {
                Intent intent = new Intent(TripItineraryActivity.this, CreateGroupActivity.class);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
                finish();
            });
        }

        if (navMoney != null) {
            navMoney.setOnClickListener(v -> {
                startActivity(new Intent(TripItineraryActivity.this, DashboardActivity.class));
                finish();
            });
        }

        itineraryAdapter = new ArrayAdapter<TripItinerary>(this, android.R.layout.simple_list_item_1, itineraryList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                TripItinerary item = getItem(position);
                if (item != null) {
                    view.setText("📍 " + item.getTitle() + "\n" + (item.getDescription() != null ? item.getDescription() + "\n" : "") + "Date: " + (item.getActivityDate() != null ? item.getActivityDate() : "TBD"));
                }
                view.setTypeface(null, Typeface.BOLD);
                view.setTextColor(Color.BLACK);
                view.setTextSize(14);
                view.setPadding(10, 10, 10, 10);
                return view;
            }
        };
        itineraryListView.setAdapter(itineraryAdapter);

        itineraryListView.setOnItemLongClickListener((parent, view, position, id) -> {
            TripItinerary item = itineraryList.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Delete Itinerary Item")
                    .setMessage("Are you sure you want to delete '" + item.getTitle() + "'?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        ApiClient.getApiService().deleteItinerary(groupId, item.getId(), userId).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(TripItineraryActivity.this, "Itinerary item deleted", Toast.LENGTH_SHORT).show();
                                    loadItinerary();
                                } else {
                                    Toast.makeText(TripItineraryActivity.this, "Failed to delete item", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Toast.makeText(TripItineraryActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        addItineraryButton.setOnClickListener(v -> {
            String title = itineraryTitleInput.getText().toString().trim();
            String desc = itineraryDescInput.getText().toString().trim();
            String date = itineraryDateInput.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter activity title", Toast.LENGTH_SHORT).show();
                return;
            }

            addItineraryButton.setEnabled(false);
            ItineraryRequest req = new ItineraryRequest(userId, title, desc.isEmpty() ? null : desc, date.isEmpty() ? null : date, null);

            ApiClient.getApiService().addItinerary(groupId, tripId, req).enqueue(new Callback<TripItinerary>() {
                @Override
                public void onResponse(Call<TripItinerary> call, Response<TripItinerary> response) {
                    addItineraryButton.setEnabled(true);
                    if (response.isSuccessful()) {
                        itineraryTitleInput.setText("");
                        itineraryDescInput.setText("");
                        itineraryDateInput.setText("");
                        Toast.makeText(TripItineraryActivity.this, "Itinerary added!", Toast.LENGTH_SHORT).show();
                        loadItinerary();
                    } else {
                        Toast.makeText(TripItineraryActivity.this, "Failed to add itinerary item", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<TripItinerary> call, Throwable t) {
                    addItineraryButton.setEnabled(true);
                    Toast.makeText(TripItineraryActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });

        loadItinerary();
    }

    private void loadItinerary() {
        ApiClient.getApiService().getItinerary(groupId, tripId, userId).enqueue(new Callback<List<TripItinerary>>() {
            @Override
            public void onResponse(Call<List<TripItinerary>> call, Response<List<TripItinerary>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    itineraryList.clear();
                    itineraryList.addAll(response.body());
                    itineraryAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<TripItinerary>> call, Throwable t) {
                Toast.makeText(TripItineraryActivity.this, "Failed to load itinerary", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SessionManager.needsPinUnlock(this)) {
            Intent intent = new Intent(TripItineraryActivity.this, PinLockActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
