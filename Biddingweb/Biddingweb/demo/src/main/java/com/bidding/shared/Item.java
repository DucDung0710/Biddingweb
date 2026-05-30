package com.bidding.shared;

import java.math.BigDecimal;

public class Item {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_IN_AUCTION = "IN_AUCTION";
    public static final String STATUS_SOLD = "SOLD";
    public static final String STATUS_UNSOLD = "UNSOLD";

    private int itemId;
    private int userId;
    private String itemName;
    private String type;
    private String description;
    private String status;
    private BigDecimal firstprice;
    private BigDecimal currentPrice;

    public Item() {
        this.status = STATUS_PENDING;
        this.firstprice = BigDecimal.ZERO;
        this.currentPrice = BigDecimal.ZERO;
    }

    public Item(int itemId, int userId, String itemName, String type, String description, double price) {
        this(itemId, userId, itemName, type, description, BigDecimal.valueOf(price));
    }

    public Item(int itemId, int userId, String itemName, String type, String description, BigDecimal price) {
        this.itemId = itemId;
        this.userId = userId;
        this.itemName = itemName;
        this.type = type;
        this.description = description;
        this.status = STATUS_PENDING;
        this.firstprice = price != null && price.compareTo(BigDecimal.ZERO) >= 0 ? price : BigDecimal.ZERO;
        this.currentPrice = this.firstprice;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getFirstprice() {
        return firstprice;
    }

    public void setFirstprice(BigDecimal firstprice) {
        if (firstprice == null || firstprice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải là số không âm.");
        }
        this.firstprice = firstprice;
        this.currentPrice = firstprice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá hiện tại phải là số không âm.");
        }
        this.currentPrice = currentPrice;
    }
}
