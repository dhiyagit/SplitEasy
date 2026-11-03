package com.example.spliteasy;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.PinSetRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePinActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_pin);

        EditText newPinInput = findViewById(R.id.newPinInput);
        EditText confirmNewPinInput = findViewById(R.id.confirmNewPinInput);
        Button changePinButton = findViewById(R.id.changePinButton);

        Long userId = SessionManager.getUserId(this);

        changePinButton.setOnClickListener(v -> {
            String newPin = newPinInput.getText().toString().trim();
            String confirm = confirmNewPinInput.getText().toString().trim();

            if (newPin.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPin.length() != 4 || !newPin.matches("\\d{4}")) {
                Toast.makeText(this, "PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPin.equals(confirm)) {
                Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            changePinButton.setEnabled(false);
            PinSetRequest request = new PinSetRequest(userId, newPin);

            ApiClient.getApiService().setPin(request).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    changePinButton.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(ChangePinActivity.this, "PIN updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ChangePinActivity.this, "Failed to update PIN", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    changePinButton.setEnabled(true);
                    Toast.makeText(ChangePinActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
