package com.example.spliteasy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PlansActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plans);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            View bottomNav = findViewById(R.id.bottomNavContainer);
            if (bottomNav != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomNav.getLayoutParams();
                params.bottomMargin = insets.bottom;
                bottomNav.setLayoutParams(params);
            }
            return windowInsets;
        });

        findViewById(R.id.navHomeButton).setOnClickListener(v -> {
            startActivity(new Intent(PlansActivity.this, HomeActivity.class));
            finish();
        });
        findViewById(R.id.navCreateGroupButton).setOnClickListener(v -> {
            Long userId = SessionManager.getUserId(PlansActivity.this);
            Intent intent = new Intent(PlansActivity.this, CreateGroupActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.navMoneyButton).setOnClickListener(v -> {
            startActivity(new Intent(PlansActivity.this, DashboardActivity.class));
            finish();
        });
        findViewById(R.id.navEventButton).setOnClickListener(v -> {
            startActivity(new Intent(PlansActivity.this, EventActivity.class));
            finish();
        });
    }
}
