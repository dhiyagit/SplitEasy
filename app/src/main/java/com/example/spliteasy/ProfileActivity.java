package com.example.spliteasy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.UpiUpdateRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private Long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userId = SessionManager.getUserId(this);

        EditText upiIdInput = findViewById(R.id.upiIdInput);
        Button saveUpiButton = findViewById(R.id.saveUpiButton);
        Button changePinButton = findViewById(R.id.changePinButton);

        saveUpiButton.setOnClickListener(v -> {
            String upiId = upiIdInput.getText().toString().trim();
            if (upiId.isEmpty()) {
                Toast.makeText(this, "Please enter a UPI ID", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!upiId.matches("[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}")) {
                Toast.makeText(this, "Invalid UPI ID format (e.g. name@okaxis)", Toast.LENGTH_SHORT).show();
                return;
            }

            saveUpiButton.setEnabled(false);
            UpiUpdateRequest request = new UpiUpdateRequest(userId, upiId);

            ApiClient.getApiService().updateUpiId(request).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    saveUpiButton.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "UPI ID saved successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Failed to save UPI ID", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    saveUpiButton.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });

        changePinButton.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, ChangePinActivity.class));
        });
    }
}
