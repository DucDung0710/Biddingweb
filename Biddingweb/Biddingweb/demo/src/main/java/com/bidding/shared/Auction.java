package com.bidding.shared;

public class Auction {
    private int id;
    private int itemId;
    private double startPrice;
    private double currentPrice;
    private String startTime; // Định dạng chuỗi ISO-8601
    private String endTime;
    private String status; // 'OPEN','RUNNING','FINISHED','PAID','CANCELED'
    private int winnerId;

    public Auction() {}

    // Hệ thống Getter/Setter bảo vệ thuộc tính
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public double getStartPrice() { return startPrice; }
    public void setStartPrice(double startPrice) { this.startPrice = startPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getWinnerId() { return winnerId; }
    public void setWinnerId(int winnerId) { this.winnerId = winnerId; }
}