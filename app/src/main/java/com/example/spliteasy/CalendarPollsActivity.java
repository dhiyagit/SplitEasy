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
import com.example.spliteasy.api.CalendarEntry;
import com.example.spliteasy.api.CalendarRequest;
import com.example.spliteasy.api.Poll;
import com.example.spliteasy.api.PollOption;
import com.example.spliteasy.api.PollRequest;
import com.example.spliteasy.api.VoteRequest;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalendarPollsActivity extends AppCompatActivity {

    private Long groupId, userId;
    private ListView calendarListView, pollsListView;
    private ArrayAdapter<CalendarEntry> calendarAdapter;
    private ArrayAdapter<Poll> pollAdapter;
    private final List<CalendarEntry> calendarList = new ArrayList<>();
    private final List<Poll> pollList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_polls);

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

        EditText calendarTitleInput = findViewById(R.id.calendarTitleInput);
        EditText calendarDateInput = findViewById(R.id.calendarDateInput);
        EditText calendarDescInput = findViewById(R.id.calendarDescInput);
        Button addCalendarButton = findViewById(R.id.addCalendarButton);
        calendarListView = findViewById(R.id.calendarListView);

        EditText pollQuestionInput = findViewById(R.id.pollQuestionInput);
        EditText pollOpt1Input = findViewById(R.id.pollOpt1Input);
        EditText pollOpt2Input = findViewById(R.id.pollOpt2Input);
        EditText pollOpt3Input = findViewById(R.id.pollOpt3Input);
        Button createPollButton = findViewById(R.id.createPollButton);
        pollsListView = findViewById(R.id.pollsListView);

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
                startActivity(new Intent(CalendarPollsActivity.this, HomeActivity.class));
                finish();
            });
        }

        if (navCreateGroup != null) {
            navCreateGroup.setOnClickListener(v -> {
                Intent intent = new Intent(CalendarPollsActivity.this, CreateGroupActivity.class);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
                finish();
            });
        }

        if (navMoney != null) {
            navMoney.setOnClickListener(v -> {
                startActivity(new Intent(CalendarPollsActivity.this, DashboardActivity.class));
                finish();
            });
        }

        calendarAdapter = new ArrayAdapter<CalendarEntry>(this, android.R.layout.simple_list_item_1, calendarList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                CalendarEntry ce = getItem(position);
                if (ce != null) {
                    view.setText("📅 " + ce.getTitle() + "\nDate: " + (ce.getEntryDate() != null ? ce.getEntryDate() : "TBD") + (ce.getDescription() != null && !ce.getDescription().isEmpty() ? " | " + ce.getDescription() : ""));
                }
                view.setTypeface(null, Typeface.BOLD);
                view.setTextColor(Color.BLACK);
                view.setTextSize(14);
                view.setPadding(10, 10, 10, 10);
                return view;
            }
        };
        calendarListView.setAdapter(calendarAdapter);

        calendarListView.setOnItemLongClickListener((parent, view, position, id) -> {
            CalendarEntry ce = calendarList.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Delete Calendar Entry")
                    .setMessage("Are you sure you want to delete '" + ce.getTitle() + "'?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        ApiClient.getApiService().deleteCalendarEntry(groupId, ce.getId(), userId).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(CalendarPollsActivity.this, "Calendar entry deleted", Toast.LENGTH_SHORT).show();
                                    loadCalendar();
                                } else {
                                    Toast.makeText(CalendarPollsActivity.this, "Failed to delete entry", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Toast.makeText(CalendarPollsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        pollAdapter = new ArrayAdapter<Poll>(this, android.R.layout.simple_list_item_1, pollList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                Poll p = getItem(position);
                if (p != null) {
                    StringBuilder sb = new StringBuilder("📊 " + p.getQuestion() + "\n");
                    if (p.getOptions() != null) {
                        for (PollOption opt : p.getOptions()) {
                            String check = (p.getUserVotedOptionId() != null && p.getUserVotedOptionId().equals(opt.getId())) ? " ✔ [Your Vote]" : "";
                            sb.append("  • ").append(opt.getOptionText()).append(" (").append(opt.getVoteCount()).append(" votes)").append(check).append("\n");
                        }
                    }
                    view.setText(sb.toString().trim());
                }
                view.setTypeface(null, Typeface.BOLD);
                view.setTextColor(Color.BLACK);
                view.setTextSize(14);
                view.setPadding(10, 10, 10, 10);
                return view;
            }
        };
        pollsListView.setAdapter(pollAdapter);

        pollsListView.setOnItemClickListener((parent, view, position, id) -> {
            Poll p = pollList.get(position);
            if (p.getOptions() == null || p.getOptions().isEmpty()) return;

            List<String> optionTexts = new ArrayList<>();
            for (PollOption opt : p.getOptions()) {
                String check = (p.getUserVotedOptionId() != null && p.getUserVotedOptionId().equals(opt.getId())) ? " (Your Vote)" : "";
                optionTexts.add(opt.getOptionText() + " [" + opt.getVoteCount() + " votes]" + check);
            }

            CharSequence[] items = optionTexts.toArray(new CharSequence[0]);
            new AlertDialog.Builder(this)
                    .setTitle("Vote on Poll: " + p.getQuestion())
                    .setItems(items, (dialog, which) -> {
                        PollOption selectedOpt = p.getOptions().get(which);
                        VoteRequest req = new VoteRequest(userId, selectedOpt.getId());
                        ApiClient.getApiService().votePoll(groupId, p.getId(), req).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(CalendarPollsActivity.this, "Vote recorded!", Toast.LENGTH_SHORT).show();
                                    loadPolls();
                                } else {
                                    Toast.makeText(CalendarPollsActivity.this, "Failed to record vote", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Toast.makeText(CalendarPollsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        pollsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Poll p = pollList.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Delete Poll")
                    .setMessage("Are you sure you want to delete poll '" + p.getQuestion() + "'?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        ApiClient.getApiService().deletePoll(groupId, p.getId(), userId).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(CalendarPollsActivity.this, "Poll deleted successfully", Toast.LENGTH_SHORT).show();
                                    loadPolls();
                                } else {
                                    Toast.makeText(CalendarPollsActivity.this, "Failed to delete poll", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Toast.makeText(CalendarPollsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        addCalendarButton.setOnClickListener(v -> {
            String title = calendarTitleInput.getText().toString().trim();
            String rawDate = calendarDateInput.getText().toString().trim();
            String desc = calendarDescInput.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter event title", Toast.LENGTH_SHORT).show();
                return;
            }

            String entryDate = null;
            if (!rawDate.isEmpty()) {
                entryDate = parseAndValidateDate(rawDate);
                if (entryDate == null) {
                    Toast.makeText(this, "Invalid date. Please use YYYY-MM-DD or YYYYMMDD.", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            addCalendarButton.setEnabled(false);
            CalendarRequest req = new CalendarRequest(title, entryDate, desc.isEmpty() ? null : desc, userId);

            ApiClient.getApiService().createCalendarEntry(groupId, req).enqueue(new Callback<CalendarEntry>() {
                @Override
                public void onResponse(Call<CalendarEntry> call, Response<CalendarEntry> response) {
                    addCalendarButton.setEnabled(true);
                    if (response.isSuccessful()) {
                        calendarTitleInput.setText("");
                        calendarDateInput.setText("");
                        calendarDescInput.setText("");
                        Toast.makeText(CalendarPollsActivity.this, "Added to calendar!", Toast.LENGTH_SHORT).show();
                        loadCalendar();
                    } else {
                        Toast.makeText(CalendarPollsActivity.this, "Failed to add calendar entry", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<CalendarEntry> call, Throwable t) {
                    addCalendarButton.setEnabled(true);
                    Toast.makeText(CalendarPollsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });

        createPollButton.setOnClickListener(v -> {
            String question = pollQuestionInput.getText().toString().trim();
            String opt1 = pollOpt1Input.getText().toString().trim();
            String opt2 = pollOpt2Input.getText().toString().trim();
            String opt3 = pollOpt3Input.getText().toString().trim();

            if (question.isEmpty() || opt1.isEmpty() || opt2.isEmpty()) {
                Toast.makeText(this, "Please enter a question and at least 2 options", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> options = new ArrayList<>();
            options.add(opt1);
            options.add(opt2);
            if (!opt3.isEmpty()) {
                options.add(opt3);
            }

            createPollButton.setEnabled(false);
            PollRequest req = new PollRequest(question, options, userId);

            ApiClient.getApiService().createPoll(groupId, req).enqueue(new Callback<Poll>() {
                @Override
                public void onResponse(Call<Poll> call, Response<Poll> response) {
                    createPollButton.setEnabled(true);
                    if (response.isSuccessful()) {
                        pollQuestionInput.setText("");
                        pollOpt1Input.setText("");
                        pollOpt2Input.setText("");
                        pollOpt3Input.setText("");
                        Toast.makeText(CalendarPollsActivity.this, "Poll created successfully!", Toast.LENGTH_SHORT).show();
                        loadPolls();
                    } else {
                        Toast.makeText(CalendarPollsActivity.this, "Failed to create poll", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Poll> call, Throwable t) {
                    createPollButton.setEnabled(true);
                    Toast.makeText(CalendarPollsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });

        loadCalendar();
        loadPolls();
    }

    private void loadCalendar() {
        ApiClient.getApiService().getCalendarEntries(groupId, userId).enqueue(new Callback<List<CalendarEntry>>() {
            @Override
            public void onResponse(Call<List<CalendarEntry>> call, Response<List<CalendarEntry>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    calendarList.clear();
                    calendarList.addAll(response.body());
                    calendarAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<CalendarEntry>> call, Throwable t) {
                Toast.makeText(CalendarPollsActivity.this, "Failed to load calendar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPolls() {
        ApiClient.getApiService().getPolls(groupId, userId).enqueue(new Callback<List<Poll>>() {
            @Override
            public void onResponse(Call<List<Poll>> call, Response<List<Poll>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    pollList.clear();
                    pollList.addAll(response.body());
                    pollAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Poll>> call, Throwable t) {
                Toast.makeText(CalendarPollsActivity.this, "Failed to load polls", Toast.LENGTH_SHORT).show();
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
            Intent intent = new Intent(CalendarPollsActivity.this, PinLockActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
