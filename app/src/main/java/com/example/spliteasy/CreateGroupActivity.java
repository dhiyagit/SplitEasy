package com.example.spliteasy;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.AddMemberRequest;
import com.example.spliteasy.api.BirthdayRequest;
import com.example.spliteasy.api.CreateGroupRequest;
import com.example.spliteasy.api.Group;
import com.example.spliteasy.api.UserDto;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateGroupActivity extends AppCompatActivity {

    private Long userId;
    private Spinner manageGroupSpinner;
    private LinearLayout groupManagementLayout, membersContainer;
    private EditText groupNameInput, memberEmailInput;
    private Button createGroupButton, addMemberButton, deleteGroupButton;
    private List<Group> groupList = new ArrayList<>();
    private Group selectedGroup = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

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

        userId = getIntent().getLongExtra("USER_ID", -1L);
        if (userId == -1L) {
            userId = SessionManager.getUserId(this);
        }

        groupNameInput = findViewById(R.id.groupNameInput);
        createGroupButton = findViewById(R.id.createGroupButton);
        manageGroupSpinner = findViewById(R.id.manageGroupSpinner);
        groupManagementLayout = findViewById(R.id.groupManagementLayout);
        memberEmailInput = findViewById(R.id.memberEmailInput);
        addMemberButton = findViewById(R.id.addMemberButton);
        membersContainer = findViewById(R.id.membersContainer);
        deleteGroupButton = findViewById(R.id.deleteGroupButton);

        // Bottom Navigation setup (Highlight Create Group)
        Button navHome = findViewById(R.id.navHomeButton);
        Button navCreateGroup = findViewById(R.id.navCreateGroupButton);
        Button navEvent = findViewById(R.id.navEventButton);
        Button navMoney = findViewById(R.id.navMoneyButton);

        if (navCreateGroup != null) {
            navCreateGroup.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
            navCreateGroup.setOnClickListener(v -> {}); // already create group
        }

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                startActivity(new Intent(CreateGroupActivity.this, HomeActivity.class));
                finish();
            });
        }

        if (navEvent != null) {
            navEvent.setOnClickListener(v -> {
                startActivity(new Intent(CreateGroupActivity.this, EventActivity.class));
                finish();
            });
        }

        if (navMoney != null) {
            navMoney.setOnClickListener(v -> {
                startActivity(new Intent(CreateGroupActivity.this, DashboardActivity.class));
                finish();
            });
        }

        createGroupButton.setOnClickListener(v -> {
            String groupName = groupNameInput.getText().toString().trim();

            if (groupName.isEmpty()) {
                Toast.makeText(this, "Please enter a group name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userId == -1L) {
                Toast.makeText(this, "Error: User ID not found", Toast.LENGTH_SHORT).show();
                return;
            }

            createGroupButton.setEnabled(false);

            CreateGroupRequest request = new CreateGroupRequest(groupName, userId, null);

            ApiClient.getApiService().createGroup(request).enqueue(new Callback<Group>() {
                @Override
                public void onResponse(Call<Group> call, Response<Group> response) {
                    createGroupButton.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(CreateGroupActivity.this, "Group created successfully!", Toast.LENGTH_SHORT).show();
                        groupNameInput.setText("");
                        loadUserGroups();
                    } else {
                        String errorMsg = "Failed to create group";
                        if (response.errorBody() != null) {
                            try {
                                errorMsg = response.errorBody().string();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        Toast.makeText(CreateGroupActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Group> call, Throwable t) {
                    createGroupButton.setEnabled(true);
                    Toast.makeText(CreateGroupActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        EditText birthdayInput = findViewById(R.id.birthdayInput);
        Button saveBirthdayButton = findViewById(R.id.saveBirthdayButton);

        saveBirthdayButton.setOnClickListener(v -> {
            String rawBday = birthdayInput.getText().toString().trim();
            if (rawBday.isEmpty()) {
                Toast.makeText(this, "Please enter your birthday (YYYY-MM-DD or YYYYMMDD)", Toast.LENGTH_SHORT).show();
                return;
            }

            String validatedBday = parseAndValidateDate(rawBday);
            if (validatedBday == null) {
                Toast.makeText(this, "Invalid date. Please enter a real calendar date in YYYY-MM-DD or YYYYMMDD format.", Toast.LENGTH_LONG).show();
                return;
            }

            Long currentUserId = SessionManager.getUserId(this);
            if (currentUserId == -1L) {
                Toast.makeText(this, "Session error. Please login again.", Toast.LENGTH_SHORT).show();
                return;
            }

            saveBirthdayButton.setEnabled(false);
            BirthdayRequest request = new BirthdayRequest(currentUserId, validatedBday);

            ApiClient.getApiService().updateBirthday(request).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    saveBirthdayButton.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(CreateGroupActivity.this, "Birthday saved successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        String errMsg = "Failed to save birthday";
                        if (response.errorBody() != null) {
                            try {
                                errMsg = response.errorBody().string();
                            } catch (Exception ignored) {}
                        }
                        Toast.makeText(CreateGroupActivity.this, errMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    saveBirthdayButton.setEnabled(true);
                    Toast.makeText(CreateGroupActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        manageGroupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (groupList != null && position >= 0 && position < groupList.size()) {
                    selectedGroup = groupList.get(position);
                    groupManagementLayout.setVisibility(View.VISIBLE);
                    loadGroupMembers();
                } else {
                    selectedGroup = null;
                    groupManagementLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedGroup = null;
                groupManagementLayout.setVisibility(View.GONE);
            }
        });

        addMemberButton.setOnClickListener(v -> {
            if (selectedGroup == null) return;
            String email = memberEmailInput.getText().toString().trim().toLowerCase();
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter member email", Toast.LENGTH_SHORT).show();
                return;
            }

            addMemberButton.setEnabled(false);
            ApiClient.getApiService().addMemberByEmail(selectedGroup.getId(), new AddMemberRequest(email)).enqueue(new Callback<UserDto>() {
                @Override
                public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                    addMemberButton.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(CreateGroupActivity.this, "Member added successfully!", Toast.LENGTH_SHORT).show();
                        memberEmailInput.setText("");
                        loadGroupMembers();
                    } else {
                        String errMsg = "Failed to add member";
                        if (response.code() == 409) errMsg = "User is already a member";
                        else if (response.code() == 404) errMsg = "User not found with this email";
                        Toast.makeText(CreateGroupActivity.this, errMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<UserDto> call, Throwable t) {
                    addMemberButton.setEnabled(true);
                    Toast.makeText(CreateGroupActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });

        deleteGroupButton.setOnClickListener(v -> {
            if (selectedGroup == null) return;
            new AlertDialog.Builder(this)
                    .setTitle("Delete Group")
                    .setMessage("Are you sure you want to delete '" + selectedGroup.getGroupName() + "' permanently? All associated expenses and settlements will be removed.")
                    .setPositiveButton("Delete", (dialog, which) -> deleteGroup())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SessionManager.needsPinUnlock(this)) {
            Intent intent = new Intent(CreateGroupActivity.this, PinLockActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        loadUserGroups();
    }

    private void loadUserGroups() {
        if (userId == -1L) return;
        ApiClient.getApiService().getGroupsForUser(userId).enqueue(new Callback<List<Group>>() {
            @Override
            public void onResponse(Call<List<Group>> call, Response<List<Group>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    groupList = response.body();
                    ArrayAdapter<Group> adapter = new ArrayAdapter<>(CreateGroupActivity.this, android.R.layout.simple_spinner_item, groupList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    manageGroupSpinner.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<Group>> call, Throwable t) {
                Toast.makeText(CreateGroupActivity.this, "Failed to load groups", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGroupMembers() {
        if (selectedGroup == null) return;
        ApiClient.getApiService().getGroupMembers(selectedGroup.getId()).enqueue(new Callback<List<UserDto>>() {
            @Override
            public void onResponse(Call<List<UserDto>> call, Response<List<UserDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    membersContainer.removeAllViews();
                    List<UserDto> members = response.body();
                    if (members.isEmpty()) {
                        addTextView(membersContainer, "No members in this group yet.");
                    } else {
                        for (UserDto m : members) {
                            TextView tv = new TextView(CreateGroupActivity.this);
                            tv.setText("• " + m.getFullName() + " (" + m.getEmail() + ")");
                            tv.setTextSize(15);
                            tv.setTypeface(null, Typeface.BOLD);
                            tv.setTextColor(Color.BLACK);
                            tv.setPadding(8, 8, 8, 8);
                            tv.setOnClickListener(v -> {
                                new AlertDialog.Builder(CreateGroupActivity.this)
                                        .setTitle("Remove Member")
                                        .setMessage("Are you sure you want to remove " + m.getFullName() + " from this group?")
                                        .setPositiveButton("Remove", (dialog, which) -> removeMember(m.getId()))
                                        .setNegativeButton("Cancel", null)
                                        .show();
                            });
                            membersContainer.addView(tv);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<UserDto>> call, Throwable t) {}
        });
    }

    private void removeMember(Long memberUserId) {
        if (selectedGroup == null) return;
        ApiClient.getApiService().removeMember(selectedGroup.getId(), memberUserId, userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CreateGroupActivity.this, "Member removed successfully", Toast.LENGTH_SHORT).show();
                    loadGroupMembers();
                } else {
                    String err = "Cannot remove member";
                    if (response.errorBody() != null) {
                        try {
                            err = response.errorBody().string();
                        } catch (Exception ignored) {}
                    }
                    Toast.makeText(CreateGroupActivity.this, err, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(CreateGroupActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteGroup() {
        if (selectedGroup == null) return;
        ApiClient.getApiService().deleteGroup(selectedGroup.getId(), userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CreateGroupActivity.this, "Group deleted successfully", Toast.LENGTH_SHORT).show();
                    loadUserGroups();
                } else {
                    String err = "Failed to delete group";
                    if (response.errorBody() != null) {
                        try {
                            err = response.errorBody().string();
                        } catch (Exception ignored) {}
                    }
                    Toast.makeText(CreateGroupActivity.this, err, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(CreateGroupActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTextView(LinearLayout container, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.BLACK);
        tv.setPadding(8, 8, 8, 8);
        container.addView(tv);
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
}
