package com.bidding.shared;

public class Item {
    private String ItemName;
    private String description;
    private String status; // Trạng thái: "Pending", "Approved", "Rejected"

    public Item(String ItemName, String description) {
        this.ItemName = ItemName;
        this.description = description;
        this.status = "Pending"; // Mặc định trạng thái là "Pending"
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
}
