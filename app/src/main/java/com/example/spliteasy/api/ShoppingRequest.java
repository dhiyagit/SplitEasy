package com.example.spliteasy.api;

public class ShoppingRequest {
    private Long userId;
    private String itemName;
    private String quantity;
    private boolean purchased;

    public ShoppingRequest() {}

    public ShoppingRequest(Long userId, String itemName, String quantity, boolean purchased) {
        this.userId = userId;
        this.itemName = itemName;
        this.quantity = (quantity == null || quantity.trim().isEmpty()) ? null : quantity.trim();
        this.purchased = purchased;
    }
}
