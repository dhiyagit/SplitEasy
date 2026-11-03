package com.example.spliteasy;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.CreateExpenseRequest;
import com.example.spliteasy.api.Expense;
import com.example.spliteasy.api.UserDto;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddExpenseActivity extends AppCompatActivity {

    private Long groupId;
    private Spinner paidBySpinner, splitMethodSpinner, categorySpinner;
    private LinearLayout sharesSectionLayout, sharesContainer;
    private LinearLayout exactAmountsSectionLayout, exactAmountsContainer;
    private LinearLayout percentageSectionLayout, percentageContainer;
    private TextView exactRemainingTextView, percentageRemainingTextView;
    private EditText amountInput, titleInput, descriptionInput;
    private Button scanReceiptButton;

    private List<UserDto> memberList;
    private final Map<Long, EditText> shareInputMap = new HashMap<>();
    private final Map<Long, TextView> calculatedAmountMap = new HashMap<>();
    private final Map<Long, EditText> exactAmountInputMap = new HashMap<>();
    private final Map<Long, EditText> percentageInputMap = new HashMap<>();
    private final Map<Long, TextView> percentageCalculatedAmountMap = new HashMap<>();

    private Uri cameraImageUri;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        groupId = getIntent().getLongExtra("GROUP_ID", -1L);

        titleInput = findViewById(R.id.titleInput);
        amountInput = findViewById(R.id.amountInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        paidBySpinner = findViewById(R.id.paidBySpinner);
        splitMethodSpinner = findViewById(R.id.splitMethodSpinner);
        categorySpinner = findViewById(R.id.categorySpinner);

        sharesSectionLayout = findViewById(R.id.sharesSectionLayout);
        sharesContainer = findViewById(R.id.sharesContainer);
        exactAmountsSectionLayout = findViewById(R.id.exactAmountsSectionLayout);
        exactAmountsContainer = findViewById(R.id.exactAmountsContainer);
        exactRemainingTextView = findViewById(R.id.exactRemainingTextView);
        percentageSectionLayout = findViewById(R.id.percentageSectionLayout);
        percentageContainer = findViewById(R.id.percentageContainer);
        percentageRemainingTextView = findViewById(R.id.percentageRemainingTextView);

        scanReceiptButton = findViewById(R.id.scanReceiptButton);
        Button submitExpenseButton = findViewById(R.id.submitExpenseButton);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Scanning receipt with OCR...");
        progressDialog.setCancelable(false);

        // Setup Category Spinner
        String[] categories = {"Food", "Travel", "Shopping", "Bills", "Others"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(catAdapter);

        // Prepare camera file URI via FileProvider
        File photoFile = new File(getCacheDir(), "receipt_temp.jpg");
        cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);

        // Initialize Activity Result Launchers for Camera, Gallery, and Permissions
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                            new AlertDialog.Builder(this)
                                    .setTitle("Camera Permission Required")
                                    .setMessage("Camera access is required to scan receipts. Please enable it in App Settings.")
                                    .setPositiveButton("Settings", (d, w) -> {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                                        intent.setData(uri);
                                        startActivity(intent);
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        } else {
                            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    SplitEasyApplication.setExternalActionInProgress(false);
                    if (success && cameraImageUri != null) {
                        try {
                            Bitmap bitmap;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), cameraImageUri));
                            } else {
                                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), cameraImageUri);
                            }
                            if (bitmap != null) {
                                processReceiptImage(bitmap);
                            } else {
                                Toast.makeText(this, "Failed to load captured photo", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            Toast.makeText(this, "Error loading photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Camera capture cancelled or failed", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    SplitEasyApplication.setExternalActionInProgress(false);
                    if (uri != null) {
                        try {
                            Bitmap bitmap;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
                            } else {
                                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            }
                            if (bitmap != null) {
                                processReceiptImage(bitmap);
                            } else {
                                Toast.makeText(this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            Toast.makeText(this, "Error loading gallery image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Gallery selection cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        scanReceiptButton.setOnClickListener(v -> {
            CharSequence[] options = {"Take Photo", "Choose from Gallery"};
            new AlertDialog.Builder(this)
                    .setTitle("Scan Receipt")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                launchCamera();
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                            }
                        } else {
                            SplitEasyApplication.setExternalActionInProgress(true);
                            try {
                                galleryLauncher.launch("image/*");
                            } catch (Exception e) {
                                SplitEasyApplication.setExternalActionInProgress(false);
                                Toast.makeText(this, "Gallery not supported: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .show();
        });

        // Setup Split Method Spinner (All 4 methods available)
        String[] splitMethods = {"Split Equally", "Split by Shares", "Split by Exact Amounts", "Split by Percentage"};
        ArrayAdapter<String> splitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, splitMethods);
        splitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        splitMethodSpinner.setAdapter(splitAdapter);

        splitMethodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sharesSectionLayout.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                exactAmountsSectionLayout.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
                percentageSectionLayout.setVisibility(position == 3 ? View.VISIBLE : View.GONE);

                if (position == 1) recalculateSharesAmounts();
                else if (position == 2) recalculateExactAmounts();
                else if (position == 3) recalculatePercentageAmounts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        amountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                int pos = splitMethodSpinner.getSelectedItemPosition();
                if (pos == 1) recalculateSharesAmounts();
                else if (pos == 2) recalculateExactAmounts();
                else if (pos == 3) recalculatePercentageAmounts();
            }
        });

        loadGroupMembers();

        submitExpenseButton.setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            String amountStr = amountInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            String category = categorySpinner.getSelectedItem() != null ? categorySpinner.getSelectedItem().toString() : "Others";

            if (title.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter title and amount", Toast.LENGTH_SHORT).show();
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

            int selectedPosition = paidBySpinner.getSelectedItemPosition();
            if (memberList == null || memberList.isEmpty() || selectedPosition < 0 || selectedPosition >= memberList.size()) {
                Toast.makeText(this, "Select who paid", Toast.LENGTH_SHORT).show();
                return;
            }

            UserDto selectedUser = memberList.get(selectedPosition);
            Long paidByUserId = selectedUser.getId();

            int splitPos = splitMethodSpinner.getSelectedItemPosition();
            String splitType = "EQUAL";
            Map<Long, Double> sharesMap = null;
            Map<Long, Double> exactMap = null;
            Map<Long, Double> pctMap = null;

            if (splitPos == 1) { // Split by Shares
                splitType = "SHARES";
                sharesMap = new HashMap<>();
                double totalShares = 0.0;
                for (Map.Entry<Long, EditText> entry : shareInputMap.entrySet()) {
                    try {
                        double val = Double.parseDouble(entry.getValue().getText().toString().trim());
                        if (val <= 0) {
                            Toast.makeText(this, "Shares must be greater than zero", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        sharesMap.put(entry.getKey(), val);
                        totalShares += val;
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid share value", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                if (totalShares <= 0) {
                    Toast.makeText(this, "Total shares must be greater than zero", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else if (splitPos == 2) { // Split by Exact Amounts
                splitType = "EXACT";
                exactMap = new HashMap<>();
                double sumExact = 0.0;
                for (Map.Entry<Long, EditText> entry : exactAmountInputMap.entrySet()) {
                    try {
                        double val = Double.parseDouble(entry.getValue().getText().toString().trim());
                        if (val < 0) {
                            Toast.makeText(this, "Amounts cannot be negative", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        exactMap.put(entry.getKey(), val);
                        sumExact += val;
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                if (Math.abs(sumExact - amount) > 0.01) {
                    Toast.makeText(this, "Sum of exact amounts (" + CurrencyUtils.format(sumExact) + ") must equal total expense amount (" + CurrencyUtils.format(amount) + ").", Toast.LENGTH_LONG).show();
                    return;
                }
            } else if (splitPos == 3) { // Split by Percentage
                splitType = "PERCENTAGE";
                pctMap = new HashMap<>();
                double totalPct = 0.0;
                for (Map.Entry<Long, EditText> entry : percentageInputMap.entrySet()) {
                    try {
                        double val = Double.parseDouble(entry.getValue().getText().toString().trim());
                        if (val < 0) {
                            Toast.makeText(this, "Percentages cannot be negative", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        pctMap.put(entry.getKey(), val);
                        totalPct += val;
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid percentage format", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                if (Math.abs(totalPct - 100.0) > 0.01) {
                    Toast.makeText(this, "Total percentage must equal exactly 100% (currently " + totalPct + "%)", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            submitExpenseButton.setEnabled(false);

            CreateExpenseRequest request = new CreateExpenseRequest(title, amount, paidByUserId, description, splitType, sharesMap, exactMap, pctMap, null, category);

            ApiClient.getApiService().createExpense(groupId, request).enqueue(new Callback<Expense>() {
                @Override
                public void onResponse(Call<Expense> call, Response<Expense> response) {
                    submitExpenseButton.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(AddExpenseActivity.this, "Expense added successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AddExpenseActivity.this, "Failed to add expense", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Expense> call, Throwable t) {
                    submitExpenseButton.setEnabled(true);
                    Toast.makeText(AddExpenseActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void launchCamera() {
        SplitEasyApplication.setExternalActionInProgress(true);
        try {
            takePictureLauncher.launch(cameraImageUri);
        } catch (Exception e) {
            SplitEasyApplication.setExternalActionInProgress(false);
            Toast.makeText(this, "Camera not supported: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processReceiptImage(Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(this, "Invalid receipt image", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    progressDialog.dismiss();
                    String extractedText = visionText.getText();

                    if (extractedText == null || extractedText.trim().isEmpty()) {
                        Toast.makeText(this, "No text detected in receipt. Please try another image.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    ReceiptParser.ReceiptData receiptData = ReceiptParser.parse(extractedText);

                    StringBuilder summary = new StringBuilder();
                    summary.append("Merchant: ").append(receiptData.merchantName).append("\n\n");
                    summary.append("Items:\n");
                    for (ReceiptParser.ReceiptItem item : receiptData.items) {
                        summary.append("• ").append(item.name).append(" - ").append(CurrencyUtils.format(item.price)).append("\n");
                    }
                    if (receiptData.subtotal > 0) {
                        summary.append("\nSubtotal: ").append(CurrencyUtils.format(receiptData.subtotal));
                    }
                    if (receiptData.tax > 0) {
                        summary.append("\nTax/GST: ").append(CurrencyUtils.format(receiptData.tax));
                    }
                    summary.append("\nGrand Total: ").append(CurrencyUtils.format(receiptData.grandTotal));

                    new AlertDialog.Builder(this)
                            .setTitle("Scanned Receipt Details")
                            .setMessage(summary.toString())
                            .setPositiveButton("Use Receipt Details", (dialog, which) -> {
                                titleInput.setText(receiptData.merchantName);
                                if (receiptData.grandTotal > 0) {
                                    amountInput.setText(String.format(java.util.Locale.US, "%.2f", receiptData.grandTotal));
                                }
                            })
                            .setNegativeButton("Close", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "OCR Scanning Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadGroupMembers() {
        ApiClient.getApiService().getGroupMembers(groupId).enqueue(new Callback<List<UserDto>>() {
            @Override
            public void onResponse(Call<List<UserDto>> call, Response<List<UserDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    memberList = response.body();
                    ArrayAdapter<UserDto> adapter = new ArrayAdapter<>(AddExpenseActivity.this, android.R.layout.simple_spinner_item, memberList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    paidBySpinner.setAdapter(adapter);

                    populateSharesRows();
                    populateExactRows();
                    populatePercentageRows();
                }
            }

            @Override
            public void onFailure(Call<List<UserDto>> call, Throwable t) {
                Toast.makeText(AddExpenseActivity.this, "Failed to load members", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateSharesRows() {
        sharesContainer.removeAllViews();
        shareInputMap.clear();
        calculatedAmountMap.clear();

        if (memberList == null) return;

        for (UserDto m : memberList) {
            View rowView = LayoutInflater.from(this).inflate(R.layout.item_member_share, sharesContainer, false);
            TextView nameTv = rowView.findViewById(R.id.memberNameTextView);
            EditText shareEt = rowView.findViewById(R.id.shareInput);
            TextView amtTv = rowView.findViewById(R.id.calculatedAmountTextView);

            nameTv.setText(m.getFullName());
            shareEt.setText("1");

            shareEt.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    recalculateSharesAmounts();
                }
            });

            shareInputMap.put(m.getId(), shareEt);
            calculatedAmountMap.put(m.getId(), amtTv);
            sharesContainer.addView(rowView);
        }

        recalculateSharesAmounts();
    }

    private void populateExactRows() {
        exactAmountsContainer.removeAllViews();
        exactAmountInputMap.clear();

        if (memberList == null) return;

        for (UserDto m : memberList) {
            View rowView = LayoutInflater.from(this).inflate(R.layout.item_member_exact, exactAmountsContainer, false);
            TextView nameTv = rowView.findViewById(R.id.memberNameTextView);
            EditText exactEt = rowView.findViewById(R.id.exactAmountInput);

            nameTv.setText(m.getFullName());
            exactEt.setText("0");

            exactEt.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    recalculateExactAmounts();
                }
            });

            exactAmountInputMap.put(m.getId(), exactEt);
            exactAmountsContainer.addView(rowView);
        }

        recalculateExactAmounts();
    }

    private void populatePercentageRows() {
        percentageContainer.removeAllViews();
        percentageInputMap.clear();
        percentageCalculatedAmountMap.clear();

        if (memberList == null || memberList.isEmpty()) return;

        double defaultPct = 100.0 / memberList.size();

        for (UserDto m : memberList) {
            View rowView = LayoutInflater.from(this).inflate(R.layout.item_member_percentage, percentageContainer, false);
            TextView nameTv = rowView.findViewById(R.id.memberNameTextView);
            EditText pctEt = rowView.findViewById(R.id.percentageInput);
            TextView amtTv = rowView.findViewById(R.id.calculatedAmountTextView);

            nameTv.setText(m.getFullName());
            pctEt.setText(String.format(java.util.Locale.US, "%.1f", defaultPct));

            pctEt.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    recalculatePercentageAmounts();
                }
            });

            percentageInputMap.put(m.getId(), pctEt);
            percentageCalculatedAmountMap.put(m.getId(), amtTv);
            percentageContainer.addView(rowView);
        }

        recalculatePercentageAmounts();
    }

    private void recalculateSharesAmounts() {
        String amountStr = amountInput.getText().toString().trim();
        double totalAmount = 0.0;
        try {
            totalAmount = Double.parseDouble(amountStr);
        } catch (NumberFormatException ignored) {}

        double totalShares = 0.0;
        Map<Long, Double> rawShares = new HashMap<>();

        for (Map.Entry<Long, EditText> entry : shareInputMap.entrySet()) {
            try {
                double val = Double.parseDouble(entry.getValue().getText().toString().trim());
                if (val > 0) {
                    rawShares.put(entry.getKey(), val);
                    totalShares += val;
                }
            } catch (NumberFormatException ignored) {}
        }

        for (Map.Entry<Long, TextView> entry : calculatedAmountMap.entrySet()) {
            Long userId = entry.getKey();
            TextView amtTv = entry.getValue();

            if (totalShares <= 0 || totalAmount <= 0) {
                amtTv.setText(CurrencyUtils.format(0.0));
            } else {
                double userShare = rawShares.getOrDefault(userId, 0.0);
                double calculatedAmt = (totalAmount * userShare) / totalShares;
                amtTv.setText(CurrencyUtils.format(calculatedAmt));
            }
        }
    }

    private void recalculateExactAmounts() {
        String amountStr = amountInput.getText().toString().trim();
        double totalAmount = 0.0;
        try {
            totalAmount = Double.parseDouble(amountStr);
        } catch (NumberFormatException ignored) {}

        double allocated = 0.0;
        for (Map.Entry<Long, EditText> entry : exactAmountInputMap.entrySet()) {
            try {
                double val = DialogUtilsGetDouble(entry.getValue().getText().toString().trim());
                allocated += val;
            } catch (NumberFormatException ignored) {}
        }

        double remaining = totalAmount - allocated;
        exactRemainingTextView.setText("Remaining to allocate: " + CurrencyUtils.format(remaining));
        if (Math.abs(remaining) < 0.01) {
            exactRemainingTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            exactRemainingTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private double DialogUtilsGetDouble(String str) {
        if (str.isEmpty()) return 0.0;
        return Double.parseDouble(str);
    }

    private void recalculatePercentageAmounts() {
        String amountStr = amountInput.getText().toString().trim();
        double totalAmount = 0.0;
        try {
            totalAmount = Double.parseDouble(amountStr);
        } catch (NumberFormatException ignored) {}

        double totalPct = 0.0;
        Map<Long, Double> rawPct = new HashMap<>();

        for (Map.Entry<Long, EditText> entry : percentageInputMap.entrySet()) {
            try {
                double val = DialogUtilsGetDouble(entry.getValue().getText().toString().trim());
                rawPct.put(entry.getKey(), val);
                totalPct += val;
            } catch (NumberFormatException ignored) {}
        }

        double remainingPct = 100.0 - totalPct;
        percentageRemainingTextView.setText(String.format(java.util.Locale.US, "Remaining percentage: %.1f%%", remainingPct));
        if (Math.abs(remainingPct) < 0.01) {
            percentageRemainingTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            percentageRemainingTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        for (Map.Entry<Long, TextView> entry : percentageCalculatedAmountMap.entrySet()) {
            Long userId = entry.getKey();
            TextView amtTv = entry.getValue();

            if (totalAmount <= 0) {
                amtTv.setText(CurrencyUtils.format(0.0));
            } else {
                double userPct = rawPct.getOrDefault(userId, 0.0);
                double calculatedAmt = (totalAmount * userPct) / 100.0;
                amtTv.setText(CurrencyUtils.format(calculatedAmt));
            }
        }
    }
}
