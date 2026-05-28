package com.bidding.model;

/**
 * DTO truyền lịch sử đặt giá từ Server → Client qua Socket (JSON).
 */
public class BidHistoryDTO {
    private int    bidId;
    private int    auctionId;
    private String bidderName;   // tên người đặt giá (để hiển thị, che bớt ở client)
    private double amount;
    private String bidTime;      // format "HH:mm:ss" hoặc "yyyy-MM-dd HH:mm:ss"
    private boolean isAuto;      // true nếu là auto-bid

    public BidHistoryDTO() {}

    public BidHistoryDTO(int bidId, int auctionId, String bidderName,
                         double amount, String bidTime, boolean isAuto) {
        this.bidId      = bidId;
        this.auctionId  = auctionId;
        this.bidderName = bidderName;
        this.amount     = amount;
        this.bidTime    = bidTime;
        this.isAuto     = isAuto;
    }

    // ── Getters & Setters ────────────────────────────────────────

    public int getBidId()            { return bidId; }
    public void setBidId(int id)     { this.bidId = id; }

    public int getAuctionId()              { return auctionId; }
    public void setAuctionId(int id)       { this.auctionId = id; }

    public String getBidderName()              { return bidderName; }
    public void setBidderName(String name)     { this.bidderName = name; }

    public double getAmount()              { return amount; }
    public void setAmount(double amount)   { this.amount = amount; }

    public String getBidTime()             { return bidTime; }
    public void setBidTime(String time)    { this.bidTime = time; }

    public boolean isAuto()                { return isAuto; }
    public void setAuto(boolean auto)      { this.isAuto = auto; }
}