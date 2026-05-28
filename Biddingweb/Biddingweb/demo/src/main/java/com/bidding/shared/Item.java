package com.bidding.shared;

public class Item {
    private int itemId;
    private int userId; // ID của người bán
    private String ItemName;
    private String type;
    private String description;
    private String status; // Trạng thái: "Pending", "Approved", "Rejected"
    private double firstprice ;

    public Item(int itemId, int userId, String ItemName, String type, String description, double price) {
        this.itemId = itemId;
        this.userId = userId;
        this.ItemName = ItemName;
        this.type = type;
        this.description = description;
        this.status = "Pending"; // Mặc định trạng thái là "Pending"
        this.firstprice = price; // Mặc định giá khởi điểm là 0
    }

    public Item() {

    }

    public int getItemId() {return itemId;}
    public void setItemId(int itemId) {this.itemId = itemId;}

    public int getUserId() {return userId;}
    public void setUserId(int userId) {this.userId = userId;}

    public String getItemName() {return ItemName;
    }
    public void setItemName(String ItemName) {
        this.ItemName = ItemName;
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

    public double getFirstprice() {
        return firstprice;
    }

    public void setFirstprice(double firstprice) {
        this.firstprice = firstprice;
    }

    class Electronics extends Item {
        public Electronics(int itemId, int userId, String ItemName, String description, double price) {
            super(itemId, userId, ItemName, "ELECTRONICS", description, price);
        }
    }
    class Art extends Item {
        public Art(int itemId, int userId, String ItemName, String description, double price) {
            super(itemId, userId, ItemName, "ART", description, price);
        }
    }
    class Vehicle extends Item {
        public Vehicle(int itemId, int userId, String ItemName, String description, double price) {
            super(itemId, userId, ItemName, "VEHICLE", description, price);
        }
    }
}
