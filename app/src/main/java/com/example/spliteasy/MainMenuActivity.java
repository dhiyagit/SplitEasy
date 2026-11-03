package com.example.spliteasy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        Button menuHomeButton = findViewById(R.id.menuHomeButton);
        Button menuGroupsButton = findViewById(R.id.menuGroupsButton);
        Button menuEventButton = findViewById(R.id.menuEventButton);
        Button menuMoneyButton = findViewById(R.id.menuMoneyButton);

        menuHomeButton.setOnClickListener(v -> {
            startActivity(new Intent(MainMenuActivity.this, HomeActivity.class));
        });

        menuGroupsButton.setOnClickListener(v -> {
            startActivity(new Intent(MainMenuActivity.this, CreateGroupActivity.class));
        });

        menuEventButton.setOnClickListener(v -> {
            startActivity(new Intent(MainMenuActivity.this, EventActivity.class));
        });

        menuMoneyButton.setOnClickListener(v -> {
            startActivity(new Intent(MainMenuActivity.this, DashboardActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SessionManager.needsPinUnlock(this)) {
            Intent intent = new Intent(MainMenuActivity.this, PinLockActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
