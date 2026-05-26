package com.bidding.shared;

import java.math.BigDecimal;

public class Item {
    private String itemId;        // ID của sản phẩm (auto-increment)
    private String userId;        // ID của người bán (chủ sở hữu)
    private String ItemName;
    private String description;
    private String status; // Trạng thái: "Pending", "Approved", "Rejected"
    private BigDecimal firstprice;

    public Item(String itemId, String userId, String ItemName, String description, double price) {
        this(itemId, userId, ItemName, description, BigDecimal.valueOf(price));
    }

    public Item(String itemId, String userId, String ItemName, String description, BigDecimal price) {
        this.itemId = itemId;
        this.userId = userId;
        this.ItemName = ItemName;
        this.description = description;
        this.status = "Pending"; // Mặc định trạng thái là "Pending"
        this.firstprice = price != null && price.compareTo(BigDecimal.ZERO) >= 0 ? price : BigDecimal.ZERO;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getItemName() {
        return ItemName;
    }

    public void setItemName(String ItemName) {
        this.ItemName = ItemName;
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
    }
}
