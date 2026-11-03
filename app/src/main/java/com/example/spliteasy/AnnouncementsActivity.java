package com.example.spliteasy;

import android.app.AlertDialog;
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

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.AnnouncementRequest;
import com.example.spliteasy.api.GroupAnnouncement;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnnouncementsActivity extends AppCompatActivity {

    private Long groupId, userId;
    private ListView announcementsListView;
    private ArrayAdapter<GroupAnnouncement> adapter;
    private List<GroupAnnouncement> announcementList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcements);

        groupId = getIntent().getLongExtra("GROUP_ID", -1L);
        userId = getIntent().getLongExtra("USER_ID", -1L);

        EditText announcementInput = findViewById(R.id.announcementInput);
        Button postAnnouncementButton = findViewById(R.id.postAnnouncementButton);
        announcementsListView = findViewById(R.id.announcementsListView);

        adapter = new ArrayAdapter<GroupAnnouncement>(this, 0, announcementList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item_list_delete, parent, false);
                }
                TextView view = convertView.findViewById(R.id.itemTextView);
                Button deleteBtn = convertView.findViewById(R.id.itemDeleteButton);

                GroupAnnouncement ann = getItem(position);
                if (ann != null) {
                    view.setText(ann.getAuthorName() + " (" + ann.getCreatedAt() + "):\n" + ann.getMessage());
                    deleteBtn.setOnClickListener(v -> confirmAndDelete(ann));
                }
                return convertView;
            }
        };
        announcementsListView.setAdapter(adapter);

        postAnnouncementButton.setOnClickListener(v -> {
            String msg = announcementInput.getText().toString().trim();
            if (msg.isEmpty()) {
                Toast.makeText(this, "Please enter an announcement", Toast.LENGTH_SHORT).show();
                return;
            }

            postAnnouncementButton.setEnabled(false);
            AnnouncementRequest req = new AnnouncementRequest(userId, msg);

            ApiClient.getApiService().createAnnouncement(groupId, req).enqueue(new Callback<GroupAnnouncement>() {
                @Override
                public void onResponse(Call<GroupAnnouncement> call, Response<GroupAnnouncement> response) {
                    postAnnouncementButton.setEnabled(true);
                    if (response.isSuccessful()) {
                        announcementInput.setText("");
                        Toast.makeText(AnnouncementsActivity.this, "Posted!", Toast.LENGTH_SHORT).show();
                        loadAnnouncements();
                    } else {
                        String err = "Failed to post announcement (Code: " + response.code() + ")";
                        if (response.errorBody() != null) {
                            try (ResponseBody eb = response.errorBody()) {
                                err = "Failed to post announcement: " + eb.string() + " (Code: " + response.code() + ")";
                            } catch (Exception ignored) {}
                        }
                        Toast.makeText(AnnouncementsActivity.this, err, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<GroupAnnouncement> call, Throwable t) {
                    postAnnouncementButton.setEnabled(true);
                    Toast.makeText(AnnouncementsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        loadAnnouncements();
    }

    private void confirmAndDelete(GroupAnnouncement ann) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Announcement")
                .setMessage("Are you sure you want to delete this announcement?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    ApiClient.getApiService().deleteAnnouncement(ann.getId(), userId).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(AnnouncementsActivity.this, "Announcement deleted successfully", Toast.LENGTH_SHORT).show();
                                loadAnnouncements();
                            } else {
                                String err = "Failed to delete announcement (Code: " + response.code() + ")";
                                if (response.errorBody() != null) {
                                    try (ResponseBody eb = response.errorBody()) {
                                        err = "Failed to delete announcement: " + eb.string() + " (Code: " + response.code() + ")";
                                    } catch (Exception ignored) {}
                                }
                                Toast.makeText(AnnouncementsActivity.this, err, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(AnnouncementsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadAnnouncements() {
        ApiClient.getApiService().getAnnouncements(groupId, userId).enqueue(new Callback<List<GroupAnnouncement>>() {
            @Override
            public void onResponse(Call<List<GroupAnnouncement>> call, Response<List<GroupAnnouncement>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    announcementList.clear();
                    announcementList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<GroupAnnouncement>> call, Throwable t) {
                Toast.makeText(AnnouncementsActivity.this, "Failed to load announcements", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
