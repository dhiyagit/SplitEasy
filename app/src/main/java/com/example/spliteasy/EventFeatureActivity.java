package com.example.spliteasy;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class EventFeatureActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_feature);

        String featureName = getIntent().getStringExtra("FEATURE_NAME");
        TextView featureTitleTextView = findViewById(R.id.featureTitleTextView);
        if (featureName != null) {
            featureTitleTextView.setText(featureName);
        }
    }
}
