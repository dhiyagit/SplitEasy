package com.example.spliteasy.api;

public class ShoppingItem {
    private Long id;
    private Long groupId;
    private String itemName;
    private String quantity;
    private boolean purchased;
    private String addedByName;

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public String getItemName() { return itemName; }
    public String getQuantity() { return quantity; }
    public boolean isPurchased() { return purchased; }
    public String getAddedByName() { return addedByName; }
}
