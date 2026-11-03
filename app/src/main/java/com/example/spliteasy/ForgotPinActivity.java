package com.example.spliteasy;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.PinResetRequest;
import com.example.spliteasy.api.RecoveryRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPinActivity extends AppCompatActivity {

    private EditText identifierInput, otpInput, newPinInput;
    private Button requestOtpButton, resetPinButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_pin);

        identifierInput = findViewById(R.id.identifierInput);
        otpInput = findViewById(R.id.otpInput);
        newPinInput = findViewById(R.id.newPinInput);
        requestOtpButton = findViewById(R.id.requestOtpButton);
        resetPinButton = findViewById(R.id.resetPinButton);

        requestOtpButton.setOnClickListener(v -> {
            String identifier = identifierInput.getText().toString().trim();
            if (identifier.isEmpty()) {
                Toast.makeText(this, "Please enter your registered email", Toast.LENGTH_SHORT).show();
                return;
            }

            requestOtpButton.setEnabled(false);
            RecoveryRequest request = new RecoveryRequest(identifier);

            ApiClient.getApiService().requestOtp(request).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    requestOtpButton.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(ForgotPinActivity.this, "OTP sent successfully to your email!", Toast.LENGTH_LONG).show();
                        otpInput.setVisibility(View.VISIBLE);
                        newPinInput.setVisibility(View.VISIBLE);
                        resetPinButton.setVisibility(View.VISIBLE);
                        identifierInput.setEnabled(false);
                        requestOtpButton.setVisibility(View.GONE);
                    } else {
                        String errMsg = "Failed to send email OTP.";
                        if (response.errorBody() != null) {
                            try {
                                errMsg = response.errorBody().string();
                            } catch (Exception ignored) {}
                        }
                        Toast.makeText(ForgotPinActivity.this, errMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    requestOtpButton.setEnabled(true);
                    Toast.makeText(ForgotPinActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });

        resetPinButton.setOnClickListener(v -> {
            String identifier = identifierInput.getText().toString().trim();
            String otp = otpInput.getText().toString().trim();
            String newPin = newPinInput.getText().toString().trim();

            if (otp.isEmpty() || newPin.isEmpty()) {
                Toast.makeText(this, "Please enter OTP and new PIN", Toast.LENGTH_SHORT).show();
                return;
            }

            if (otp.length() != 6 || !otp.matches("\\d{6}")) {
                Toast.makeText(this, "OTP must be exactly 6 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPin.length() != 4 || !newPin.matches("\\d{4}")) {
                Toast.makeText(this, "New PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            resetPinButton.setEnabled(false);
            PinResetRequest request = new PinResetRequest(identifier, otp, newPin);

            ApiClient.getApiService().resetPin(request).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    resetPinButton.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(ForgotPinActivity.this, "PIN reset successfully! Please unlock with your new PIN.", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        String errMsg = "Reset failed. Invalid or expired OTP.";
                        if (response.errorBody() != null) {
                            try {
                                errMsg = response.errorBody().string();
                            } catch (Exception ignored) {}
                        }
                        Toast.makeText(ForgotPinActivity.this, errMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    resetPinButton.setEnabled(true);
                    Toast.makeText(ForgotPinActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
