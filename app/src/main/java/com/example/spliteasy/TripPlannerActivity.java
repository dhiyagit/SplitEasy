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
import com.example.spliteasy.api.Trip;
import com.example.spliteasy.api.TripRequest;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TripPlannerActivity extends AppCompatActivity {

    private Long groupId, userId;
    private ListView tripsListView;
    private ArrayAdapter<Trip> adapter;
    private final List<Trip> tripList = new ArrayList<>();

    private EditText tripNameInput, destinationInput, startDateInput, endDateInput, totalBudgetInput;
    private Button saveTripButton, cancelEditButton;
    private TextView formTitleTextView;
    private Trip editingTrip = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_planner);

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

        tripNameInput = findViewById(R.id.tripNameInput);
        destinationInput = findViewById(R.id.destinationInput);
        startDateInput = findViewById(R.id.startDateInput);
        endDateInput = findViewById(R.id.endDateInput);
        totalBudgetInput = findViewById(R.id.totalBudgetInput);
        saveTripButton = findViewById(R.id.saveTripButton);
        cancelEditButton = findViewById(R.id.cancelEditButton);
        formTitleTextView = findViewById(R.id.formTitleTextView);
        tripsListView = findViewById(R.id.tripsListView);

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
                startActivity(new Intent(TripPlannerActivity.this, HomeActivity.class));
                finish();
            });
        }

        if (navCreateGroup != null) {
            navCreateGroup.setOnClickListener(v -> {
                Intent intent = new Intent(TripPlannerActivity.this, CreateGroupActivity.class);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
                finish();
            });
        }

        if (navMoney != null) {
            navMoney.setOnClickListener(v -> {
                startActivity(new Intent(TripPlannerActivity.this, DashboardActivity.class));
                finish();
            });
        }

        adapter = new ArrayAdapter<Trip>(this, android.R.layout.simple_list_item_1, tripList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                Trip t = getItem(position);
                if (t != null) {
                    view.setText("✈️ " + t.getTripName() + " (" + (t.getDestination() != null ? t.getDestination() : "No Destination") + ")\nDates: " + (t.getStartDate() != null ? t.getStartDate() : "TBD") + " to " + (t.getEndDate() != null ? t.getEndDate() : "TBD") + " | Budget: " + CurrencyUtils.format(t.getTotalBudget()));
                }
                view.setTypeface(null, Typeface.BOLD);
                view.setTextColor(Color.BLACK);
                view.setTextSize(15);
                view.setPadding(12, 12, 12, 12);
                return view;
            }
        };
        tripsListView.setAdapter(adapter);

        tripsListView.setOnItemClickListener((parent, view, position, id) -> {
            Trip t = tripList.get(position);
            Intent intent = new Intent(TripPlannerActivity.this, TripItineraryActivity.class);
            intent.putExtra("TRIP_ID", t.getId());
            intent.putExtra("TRIP_NAME", t.getTripName());
            intent.putExtra("GROUP_ID", groupId);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        tripsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Trip t = tripList.get(position);
            CharSequence[] options = {"Edit Trip", "Delete Trip"};
            new AlertDialog.Builder(this)
                    .setTitle("Manage Trip: " + t.getTripName())
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            startEditingTrip(t);
                        } else if (which == 1) {
                            confirmAndDeleteTrip(t);
                        }
                    })
                    .show();
            return true;
        });

        saveTripButton.setOnClickListener(v -> {
            String name = tripNameInput.getText().toString().trim();
            String dest = destinationInput.getText().toString().trim();
            String rawStart = startDateInput.getText().toString().trim();
            String rawEnd = endDateInput.getText().toString().trim();
            String budgetStr = totalBudgetInput.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter trip name", Toast.LENGTH_SHORT).show();
                return;
            }

            String start = null;
            if (!rawStart.isEmpty()) {
                start = parseAndValidateDate(rawStart);
                if (start == null) {
                    Toast.makeText(this, "Invalid start date. Use YYYY-MM-DD or YYYYMMDD.", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            String end = null;
            if (!rawEnd.isEmpty()) {
                end = parseAndValidateDate(rawEnd);
                if (end == null) {
                    Toast.makeText(this, "Invalid end date. Use YYYY-MM-DD or YYYYMMDD.", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            Double budget = 0.0;
            if (!budgetStr.isEmpty()) {
                try {
                    budget = Double.parseDouble(budgetStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid budget amount", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            saveTripButton.setEnabled(false);
            TripRequest req = new TripRequest(name, dest.isEmpty() ? null : dest, start, end, budget, userId);

            if (editingTrip == null) {
                ApiClient.getApiService().createTrip(groupId, req).enqueue(new Callback<Trip>() {
                    @Override
                    public void onResponse(Call<Trip> call, Response<Trip> response) {
                        saveTripButton.setEnabled(true);
                        if (response.isSuccessful()) {
                            clearForm();
                            Toast.makeText(TripPlannerActivity.this, "Trip created successfully!", Toast.LENGTH_SHORT).show();
                            loadTrips();
                        } else {
                            String err = "Failed to create trip (Code: " + response.code() + ")";
                            if (response.errorBody() != null) {
                                try (ResponseBody eb = response.errorBody()) {
                                    err = "Failed to create trip: " + eb.string() + " (Code: " + response.code() + ")";
                                } catch (Exception ignored) {}
                            }
                            Toast.makeText(TripPlannerActivity.this, err, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Trip> call, Throwable t) {
                        saveTripButton.setEnabled(true);
                        Toast.makeText(TripPlannerActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                ApiClient.getApiService().updateTrip(groupId, editingTrip.getId(), req).enqueue(new Callback<Trip>() {
                    @Override
                    public void onResponse(Call<Trip> call, Response<Trip> response) {
                        saveTripButton.setEnabled(true);
                        if (response.isSuccessful()) {
                            clearForm();
                            Toast.makeText(TripPlannerActivity.this, "Trip updated successfully!", Toast.LENGTH_SHORT).show();
                            loadTrips();
                        } else {
                            String err = "Failed to update trip (Code: " + response.code() + ")";
                            if (response.errorBody() != null) {
                                try (ResponseBody eb = response.errorBody()) {
                                    err = "Failed to update trip: " + eb.string() + " (Code: " + response.code() + ")";
                                } catch (Exception ignored) {}
                            }
                            Toast.makeText(TripPlannerActivity.this, err, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Trip> call, Throwable t) {
                        saveTripButton.setEnabled(true);
                        Toast.makeText(TripPlannerActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        cancelEditButton.setOnClickListener(v -> clearForm());

        loadTrips();
    }

    private void startEditingTrip(Trip t) {
        editingTrip = t;
        formTitleTextView.setText("Edit Trip: " + t.getTripName());
        tripNameInput.setText(t.getTripName());
        destinationInput.setText(t.getDestination() != null ? t.getDestination() : "");
        startDateInput.setText(t.getStartDate() != null ? t.getStartDate() : "");
        endDateInput.setText(t.getEndDate() != null ? t.getEndDate() : "");
        totalBudgetInput.setText(String.valueOf(t.getTotalBudget()));
        saveTripButton.setText("Update Trip");
        cancelEditButton.setVisibility(View.VISIBLE);
    }

    private void clearForm() {
        editingTrip = null;
        formTitleTextView.setText("Create New Trip");
        tripNameInput.setText("");
        destinationInput.setText("");
        startDateInput.setText("");
        endDateInput.setText("");
        totalBudgetInput.setText("");
        saveTripButton.setText("Create Trip");
        cancelEditButton.setVisibility(View.GONE);
    }

    private void confirmAndDeleteTrip(Trip t) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Trip")
                .setMessage("Are you sure you want to delete trip '" + t.getTripName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    ApiClient.getApiService().deleteTrip(groupId, t.getId(), userId).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(TripPlannerActivity.this, "Trip deleted successfully", Toast.LENGTH_SHORT).show();
                                if (editingTrip != null && editingTrip.getId().equals(t.getId())) {
                                    clearForm();
                                }
                                loadTrips();
                            } else {
                                String err = "Failed to delete trip (Code: " + response.code() + ")";
                                if (response.errorBody() != null) {
                                    try (ResponseBody eb = response.errorBody()) {
                                        err = "Failed to delete trip: " + eb.string() + " (Code: " + response.code() + ")";
                                    } catch (Exception ignored) {}
                                }
                                Toast.makeText(TripPlannerActivity.this, err, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(TripPlannerActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadTrips() {
        ApiClient.getApiService().getTrips(groupId, userId).enqueue(new Callback<List<Trip>>() {
            @Override
            public void onResponse(Call<List<Trip>> call, Response<List<Trip>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tripList.clear();
                    tripList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Trip>> call, Throwable t) {
                Toast.makeText(TripPlannerActivity.this, "Failed to load trips", Toast.LENGTH_SHORT).show();
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
            Intent intent = new Intent(TripPlannerActivity.this, PinLockActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
