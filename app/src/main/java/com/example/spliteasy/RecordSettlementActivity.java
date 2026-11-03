package com.example.spliteasy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.RecordSettlementRequest;
import com.example.spliteasy.api.Settlement;
import com.example.spliteasy.api.UserDto;
import com.example.spliteasy.api.UpiUpdateRequest;

import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecordSettlementActivity extends AppCompatActivity {

    private Long groupId;
    private Long currentUserId;
    private Spinner payerSpinner, payeeSpinner;
    private EditText payeeUpiInput, amountInput, descriptionInput;
    private List<UserDto> memberList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_settlement);

        groupId = getIntent().getLongExtra("GROUP_ID", -1L);
        currentUserId = SessionManager.getUserId(this);

        payerSpinner = findViewById(R.id.payerSpinner);
        payeeSpinner = findViewById(R.id.payeeSpinner);
        payeeUpiInput = findViewById(R.id.payeeUpiInput);
        amountInput = findViewById(R.id.amountInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        Button submitSettlementButton = findViewById(R.id.submitSettlementButton);
        Button gpayButton = findViewById(R.id.gpayButton);

        loadGroupMembers();

        payeeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (memberList != null && position >= 0 && position < memberList.size()) {
                    UserDto payee = memberList.get(position);
                    if (payee.getUpiId() != null && !payee.getUpiId().isEmpty()) {
                        payeeUpiInput.setText(payee.getUpiId());
                    } else {
                        payeeUpiInput.setText("");
                        Toast.makeText(RecordSettlementActivity.this, payee.getFullName() + " has not saved a UPI ID yet. You can enter one manually.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        submitSettlementButton.setOnClickListener(v -> {
            String amountStr = amountInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            int payerPos = payerSpinner.getSelectedItemPosition();
            int payeePos = payeeSpinner.getSelectedItemPosition();

            if (memberList == null || memberList.isEmpty() || payerPos < 0 || payeePos < 0) {
                Toast.makeText(this, "Select payer and payee", Toast.LENGTH_SHORT).show();
                return;
            }

            UserDto payer = memberList.get(payerPos);
            UserDto payee = memberList.get(payeePos);

            if (payer.getId().equals(payee.getId())) {
                Toast.makeText(this, "Payer and payee cannot be the same person", Toast.LENGTH_SHORT).show();
                return;
            }

            String upiId = payeeUpiInput.getText().toString().trim();
            if (!upiId.isEmpty() && payee.getId().equals(currentUserId)) {
                updateUserUpiId(upiId);
            }

            submitSettlementButton.setEnabled(false);

            RecordSettlementRequest request = new RecordSettlementRequest(payer.getId(), payee.getId(), amount, description);

            ApiClient.getApiService().recordSettlement(groupId, request).enqueue(new Callback<Settlement>() {
                @Override
                public void onResponse(Call<Settlement> call, Response<Settlement> response) {
                    submitSettlementButton.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(RecordSettlementActivity.this, "Settlement recorded successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(RecordSettlementActivity.this, "Failed to record settlement", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Settlement> call, Throwable t) {
                    submitSettlementButton.setEnabled(true);
                    Toast.makeText(RecordSettlementActivity.this, "Network error", Toast.LENGTH_LONG).show();
                }
            });
        });

        gpayButton.setOnClickListener(v -> {
            String amountStr = amountInput.getText().toString().trim();
            String payeeUpi = payeeUpiInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            if (amountStr.isEmpty() || payeeUpi.isEmpty()) {
                Toast.makeText(this, "Please enter amount and payee UPI ID", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!payeeUpi.matches("[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}")) {
                Toast.makeText(this, "Invalid UPI ID format (e.g. name@okaxis)", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            int payeePos = payeeSpinner.getSelectedItemPosition();
            String payeeName = "SplitEasy Payee";
            if (memberList != null && payeePos >= 0 && payeePos < memberList.size()) {
                UserDto payee = memberList.get(payeePos);
                payeeName = payee.getFullName();
                if (payee.getId().equals(currentUserId)) {
                    updateUserUpiId(payeeUpi);
                }
            }

            Uri uri = Uri.parse("upi://pay").buildUpon()
                    .appendQueryParameter("pa", payeeUpi)
                    .appendQueryParameter("pn", payeeName)
                    .appendQueryParameter("am", String.format(Locale.US, "%.2f", amount))
                    .appendQueryParameter("cu", "INR")
                    .appendQueryParameter("tn", description.isEmpty() ? "SplitEasy Settlement" : description)
                    .build();

            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.google.android.apps.nbu.paisa.user");

            try {
                startActivity(intent);
                Toast.makeText(this, "Returned from GPay. If payment was successful, please tap 'Record Settlement'.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Intent chooser = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(chooser);
                    Toast.makeText(this, "Google Pay not found. Opened UPI app chooser.", Toast.LENGTH_LONG).show();
                } catch (Exception ex) {
                    Toast.makeText(this, "No UPI payment app installed on device.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateUserUpiId(String upiId) {
        UpiUpdateRequest request = new UpiUpdateRequest(currentUserId, upiId);
        ApiClient.getApiService().updateUpiId(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void loadGroupMembers() {
        ApiClient.getApiService().getGroupMembers(groupId).enqueue(new Callback<List<UserDto>>() {
            @Override
            public void onResponse(Call<List<UserDto>> call, Response<List<UserDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    memberList = response.body();
                    ArrayAdapter<UserDto> adapter = new ArrayAdapter<>(RecordSettlementActivity.this, android.R.layout.simple_spinner_item, memberList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    payerSpinner.setAdapter(adapter);
                    payeeSpinner.setAdapter(adapter);
                    if (memberList.size() > 1) {
                        payeeSpinner.setSelection(1);
                        UserDto payee = memberList.get(1);
                        if (payee.getUpiId() != null && !payee.getUpiId().isEmpty()) {
                            payeeUpiInput.setText(payee.getUpiId());
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<UserDto>> call, Throwable t) {
                Toast.makeText(RecordSettlementActivity.this, "Failed to load members", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
