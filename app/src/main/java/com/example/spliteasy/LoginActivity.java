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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText emailInput = findViewById(R.id.email);
        EditText passwordInput = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.loginButton);
        Button togglePasswordButton = findViewById(R.id.togglePasswordButton);
        TextView registerLink = findViewById(R.id.registerLink);

        final boolean[] isPasswordVisible = {false};
        togglePasswordButton.setOnClickListener(v -> {
            if (isPasswordVisible[0]) {
                passwordInput.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                togglePasswordButton.setText("👁");
                isPasswordVisible[0] = false;
            } else {
                passwordInput.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
                togglePasswordButton.setText("🔒");
                isPasswordVisible[0] = true;
            }
            passwordInput.setSelection(passwordInput.getText().length());
        });

        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            loginButton.setEnabled(false);

            LoginRequest request = new LoginRequest(email, password);

            ApiClient.getApiService().loginUser(request).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    loginButton.setEnabled(true);

                    if (response.isSuccessful() && response.body() != null) {
                        LoginResponse loginResponse = response.body();
                        Toast.makeText(LoginActivity.this, "Authentication successful! Enter PIN.", Toast.LENGTH_SHORT).show();

                        // Save session and require PIN verification
                        SessionManager.saveSession(LoginActivity.this, loginResponse.getUserId(), loginResponse.getFullName(), loginResponse.getEmail());
                        SessionManager.setNeedsPinUnlock(LoginActivity.this, true);

                        Intent intent = new Intent(LoginActivity.this, PinLockActivity.class);
                        intent.putExtra("USER_ID", loginResponse.getUserId());
                        intent.putExtra("FULL_NAME", loginResponse.getFullName());
                        intent.putExtra("EMAIL", loginResponse.getEmail());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    loginButton.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Cannot connect to server. Check your connection.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
