package com.example.spliteasy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.LoginRequest;
import com.example.spliteasy.api.LoginResponse;
import com.example.spliteasy.api.RegisterRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText fullName = findViewById(R.id.fullName);
        EditText email = findViewById(R.id.registerEmail);
        EditText phone = findViewById(R.id.registerPhone);
        EditText password = findViewById(R.id.registerPassword);
        EditText confirmPassword = findViewById(R.id.confirmPassword);
        EditText pinInput = findViewById(R.id.registerPin);
        EditText confirmPinInput = findViewById(R.id.confirmPin);

        Button registerButton = findViewById(R.id.registerButton);
        TextView loginLink = findViewById(R.id.loginLink);

        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        registerButton.setOnClickListener(v -> {
            String name = fullName.getText().toString().trim();
            String userEmail = email.getText().toString().trim();
            String userPhone = phone.getText().toString().trim();
            String userPassword = password.getText().toString();
            String confirm = confirmPassword.getText().toString();
            String pin = pinInput.getText().toString().trim();
            String confirmPin = confirmPinInput.getText().toString().trim();

            if (name.isEmpty() || userEmail.isEmpty() || userPassword.isEmpty() || confirm.isEmpty() || pin.isEmpty() || confirmPin.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!userPassword.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userPassword.length() < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pin.length() != 4 || !pin.matches("\\d{4}")) {
                Toast.makeText(this, "PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pin.equals(confirmPin)) {
                Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userPhone.isEmpty()) {
                userPhone = "+91";
            }

            RegisterRequest request = new RegisterRequest(name, userEmail, userPhone, userPassword, pin);

            registerButton.setEnabled(false);

            ApiClient.getApiService().registerUser(request).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> responseCall, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_LONG).show();

                        // Automatically log in
                        LoginRequest loginRequest = new LoginRequest(userEmail, userPassword);
                        ApiClient.getApiService().loginUser(loginRequest).enqueue(new Callback<LoginResponse>() {
                            @Override
                            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> loginResp) {
                                registerButton.setEnabled(true);
                                if (loginResp.isSuccessful() && loginResp.body() != null) {
                                    LoginResponse lr = loginResp.body();
                                    SessionManager.saveSession(RegisterActivity.this, lr.getUserId(), lr.getFullName(), lr.getEmail());
                                    SessionManager.setNeedsPinUnlock(RegisterActivity.this, false);

                                    Intent intent = new Intent(RegisterActivity.this, MainMenuActivity.class);
                                    intent.putExtra("USER_ID", lr.getUserId());
                                    intent.putExtra("FULL_NAME", lr.getFullName());
                                    intent.putExtra("EMAIL", lr.getEmail());
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                    finish();
                                }
                            }

                            @Override
                            public void onFailure(Call<LoginResponse> call, Throwable t) {
                                registerButton.setEnabled(true);
                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                finish();
                            }
                        });
                    } else {
                        registerButton.setEnabled(true);
                        String errMsg = "Registration failed. Email or phone may already be registered.";
                        if (response.errorBody() != null) {
                            try {
                                errMsg = response.errorBody().string();
                            } catch (Exception ignored) {}
                        }
                        Toast.makeText(RegisterActivity.this, errMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    registerButton.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "Cannot connect to server. Check your connection.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
