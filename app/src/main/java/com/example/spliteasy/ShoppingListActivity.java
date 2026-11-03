package com.example.spliteasy;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.spliteasy.api.ApiClient;
import com.example.spliteasy.api.ShoppingItem;
import com.example.spliteasy.api.ShoppingRequest;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShoppingListActivity extends AppCompatActivity {

    private Long groupId, userId;
    private ListView shoppingListView;
    private ArrayAdapter<ShoppingItem> adapter;
    private final List<ShoppingItem> shoppingList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_list);

        groupId = getIntent().getLongExtra("GROUP_ID", -1L);
        userId = getIntent().getLongExtra("USER_ID", -1L);
        if (userId == -1L) {
            userId = SessionManager.getUserId(this);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            View bottomNav = findViewById(R.id.bottomNavContainer);
            if (bottomNav != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomNav.getLayoutParams();
                params.bottomMargin = insets.bottom;
                bottomNav.setLayoutParams(params);
            }
            View scrollView = findViewById(R.id.mainScrollView);
            if (scrollView != null) {
                float density = getResources().getDisplayMetrics().density;
                int defaultBottomPaddingPx = (int) (64 * density);
                scrollView.setPadding(
                        scrollView.getPaddingLeft(),
                        scrollView.getPaddingTop(),
                        scrollView.getPaddingRight(),
                        defaultBottomPaddingPx + insets.bottom
                );
            }
            return windowInsets;
        });

        EditText itemNameInput = findViewById(R.id.itemNameInput);
        EditText itemQuantityInput = findViewById(R.id.itemQuantityInput);
        Button addItemButton = findViewById(R.id.addItemButton);
        shoppingListView = findViewById(R.id.shoppingListView);

        // Bottom Navigation setup (Highlight Home)
        Button navHome = findViewById(R.id.navHomeButton);
        Button navCreateGroup = findViewById(R.id.navCreateGroupButton);
        Button navEvent = findViewById(R.id.navEventButton);
        Button navMoney = findViewById(R.id.navMoneyButton);

        if (navHome != null) {
            navHome.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
            navHome.setOnClickListener(v -> {}); // already home
        }

        if (navCreateGroup != null) {
            navCreateGroup.setOnClickListener(v -> {
                Intent intent = new Intent(ShoppingListActivity.this, CreateGroupActivity.class);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
                finish();
            });
        }

        if (navEvent != null) {
            navEvent.setOnClickListener(v -> {
                startActivity(new Intent(ShoppingListActivity.this, EventActivity.class));
                finish();
            });
        }

        if (navMoney != null) {
            navMoney.setOnClickListener(v -> {
                startActivity(new Intent(ShoppingListActivity.this, DashboardActivity.class));
                finish();
            });
        }

        adapter = new ArrayAdapter<ShoppingItem>(this, 0, shoppingList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item_list_delete, parent, false);
                }
                TextView view = convertView.findViewById(R.id.itemTextView);
                Button deleteBtn = convertView.findViewById(R.id.itemDeleteButton);

                ShoppingItem item = getItem(position);
                if (item != null) {
                    String status = item.isPurchased() ? "✅ Bought" : "⏳ Needed";
                    view.setText(item.getItemName() + (item.getQuantity() != null && !item.getQuantity().isEmpty() ? " (" + item.getQuantity() + ")" : "") + "\nAdded by: " + item.getAddedByName() + " | Status: " + status);
                    view.setTypeface(null, Typeface.BOLD);
                    view.setTextColor(Color.BLACK);
                    view.setTextSize(14);
                    view.setPadding(10, 10, 10, 10);

                    deleteBtn.setOnClickListener(v -> confirmAndDelete(item));
                }
                return convertView;
            }
        };
        shoppingListView.setAdapter(adapter);

        shoppingListView.setOnItemClickListener((parent, view, position, id) -> {
            ShoppingItem item = shoppingList.get(position);
            updateItemStatus(item, !item.isPurchased());
        });

        addItemButton.setOnClickListener(v -> {
            String name = itemNameInput.getText().toString().trim();
            String qty = itemQuantityInput.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter item name", Toast.LENGTH_SHORT).show();
                return;
            }

            addItemButton.setEnabled(false);
            ShoppingRequest req = new ShoppingRequest(userId, name, qty.isEmpty() ? null : qty, false);

            ApiClient.getApiService().addShoppingItem(groupId, req).enqueue(new Callback<ShoppingItem>() {
                @Override
                public void onResponse(Call<ShoppingItem> call, Response<ShoppingItem> response) {
                    addItemButton.setEnabled(true);
                    if (response.isSuccessful()) {
                        itemNameInput.setText("");
                        itemQuantityInput.setText("");
                        Toast.makeText(ShoppingListActivity.this, "Item added to shopping list!", Toast.LENGTH_SHORT).show();
                        loadShoppingItems();
                    } else {
                        String err = "Failed to add item (Code: " + response.code() + ")";
                        if (response.errorBody() != null) {
                            try (ResponseBody eb = response.errorBody()) {
                                err = "Failed to add item: " + eb.string() + " (Code: " + response.code() + ")";
                            } catch (Exception ignored) {}
                        }
                        Toast.makeText(ShoppingListActivity.this, err, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ShoppingItem> call, Throwable t) {
                    addItemButton.setEnabled(true);
                    Toast.makeText(ShoppingListActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        loadShoppingItems();
    }

    private void updateItemStatus(ShoppingItem item, boolean purchased) {
        ShoppingRequest req = new ShoppingRequest(userId, item.getItemName(), item.getQuantity(), purchased);
        ApiClient.getApiService().updateShoppingItemStatus(groupId, item.getId(), req).enqueue(new Callback<ShoppingItem>() {
            @Override
            public void onResponse(Call<ShoppingItem> call, Response<ShoppingItem> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ShoppingListActivity.this, "Status updated!", Toast.LENGTH_SHORT).show();
                    loadShoppingItems();
                } else {
                    Toast.makeText(ShoppingListActivity.this, "Failed to update status", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ShoppingItem> call, Throwable t) {
                Toast.makeText(ShoppingListActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmAndDelete(ShoppingItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete '" + item.getItemName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    ApiClient.getApiService().deleteShoppingItem(groupId, item.getId(), userId).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(ShoppingListActivity.this, "Item deleted successfully", Toast.LENGTH_SHORT).show();
                                loadShoppingItems();
                            } else {
                                String err = "Failed to delete item (Code: " + response.code() + ")";
                                if (response.errorBody() != null) {
                                    try (ResponseBody eb = response.errorBody()) {
                                        err = "Failed to delete item: " + eb.string() + " (Code: " + response.code() + ")";
                                    } catch (Exception ignored) {}
                                }
                                Toast.makeText(ShoppingListActivity.this, err, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(ShoppingListActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadShoppingItems() {
        ApiClient.getApiService().getShoppingItems(groupId, userId).enqueue(new Callback<List<ShoppingItem>>() {
            @Override
            public void onResponse(Call<List<ShoppingItem>> call, Response<List<ShoppingItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    shoppingList.clear();
                    shoppingList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<ShoppingItem>> call, Throwable t) {
                Toast.makeText(ShoppingListActivity.this, "Failed to load shopping list", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SessionManager.needsPinUnlock(this)) {
            Intent intent = new Intent(ShoppingListActivity.this, PinLockActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
