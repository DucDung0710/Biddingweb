package com.bidding.model;

public class AuctionHistory {
    private String id;
    private String productName;
    private String winner;
    private String finalPrice;
    private String endTime;

    public AuctionHistory(String id, String productName, String winner, String finalPrice, String endTime) {
        this.id = id;
        this.productName = productName;
        this.winner = winner;
        this.finalPrice = finalPrice;
        this.endTime = endTime;
    }

    // Khai báo các Getter/Setter để TableView đọc được dữ liệu
    public String getId() { return id; }
    public String getProductName() { return productName; }
    public String getWinner() { return winner; }
    public String getFinalPrice() { return finalPrice; }
    public String getEndTime() { return endTime; }
}