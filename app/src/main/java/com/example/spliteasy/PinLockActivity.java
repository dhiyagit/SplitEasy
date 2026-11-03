package com.example.spliteasy;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.PinVerifyRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PinLockActivity extends AppCompatActivity {

    private StringBuilder pinBuilder = new StringBuilder();
    private TextView pinDotsTextView, errorTextView;
    private int failedAttempts = 0;
    private boolean isLockedOut = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_lock);

        pinDotsTextView = findViewById(R.id.pinDotsTextView);
        errorTextView = findViewById(R.id.errorTextView);
        TextView forgotPinLink = findViewById(R.id.forgotPinLink);

        forgotPinLink.setOnClickListener(v -> {
            startActivity(new Intent(PinLockActivity.this, ForgotPinActivity.class));
        });

        int[] numButtons = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };

        for (int id : numButtons) {
            Button btn = findViewById(id);
            btn.setOnClickListener(v -> {
                if (isLockedOut) return;
                if (pinBuilder.length() < 4) {
                    pinBuilder.append(btn.getText().toString());
                    updatePinDisplay();
                    if (pinBuilder.length() == 4) {
                        verifyPin(pinBuilder.toString());
                    }
                }
            });
        }

        findViewById(R.id.btnClear).setOnClickListener(v -> {
            if (isLockedOut) return;
            pinBuilder.setLength(0);
            updatePinDisplay();
            errorTextView.setText("");
        });

        findViewById(R.id.btnDelete).setOnClickListener(v -> {
            if (isLockedOut) return;
            if (pinBuilder.length() > 0) {
                pinBuilder.setLength(pinBuilder.length() - 1);
                updatePinDisplay();
                errorTextView.setText("");
            }
        });
    }

    private void updatePinDisplay() {
        int len = pinBuilder.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i < len) {
                sb.append("● ");
            } else {
                sb.append("○ ");
            }
        }
        pinDotsTextView.setText(sb.toString().trim());
    }

    private void verifyPin(String pin) {
        Long userId = SessionManager.getUserId(this);
        if (userId == -1L) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            SessionManager.clearSession(this);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        PinVerifyRequest request = new PinVerifyRequest(userId, pin);
        ApiClient.getApiService().verifyPin(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    failedAttempts = 0;
                    SessionManager.setNeedsPinUnlock(PinLockActivity.this, false);
                    Toast.makeText(PinLockActivity.this, "Unlocked successfully!", Toast.LENGTH_SHORT).show();
                    
                    Intent intent = new Intent(PinLockActivity.this, MainMenuActivity.class);
                    intent.putExtra("USER_ID", userId);
                    intent.putExtra("FULL_NAME", SessionManager.getFullName(PinLockActivity.this));
                    intent.putExtra("EMAIL", SessionManager.getEmail(PinLockActivity.this));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    failedAttempts++;
                    pinBuilder.setLength(0);
                    updatePinDisplay();
                    errorTextView.setText("Incorrect PIN. Please try again.");

                    if (failedAttempts >= 3) {
                        isLockedOut = true;
                        errorTextView.setText("Too many incorrect attempts. Try again in 10 seconds.");
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            isLockedOut = false;
                            errorTextView.setText("");
                        }, 10000);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                pinBuilder.setLength(0);
                updatePinDisplay();
                errorTextView.setText("Network error. Check connection.");
            }
        });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
