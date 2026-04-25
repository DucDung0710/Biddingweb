package com.bidding.shared;

public class Item {
    private String ItemName;
    private String description;
    private String status; // Trạng thái: "Pending", "Approved", "Rejected"
    private double firstprice ;

    public Item(String ItemName, String description, double price) {
        this.ItemName = ItemName;
        this.description = description;
        this.status = "Pending"; // Mặc định trạng thái là "Pending"
        this.firstprice = price; // Mặc định giá khởi điểm là 0
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

    public double getFirstprice() {
        return firstprice;
    }

    public void setFirstprice(double firstprice) {
        this.firstprice = firstprice;
    }
}
