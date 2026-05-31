package com.bidding.model;

/**
 * BidderAuctionRow - Model hiển thị 1 hàng trong bảng Lịch sử đấu giá của Bidder
 */
public class BidderAuctionRow {
    private final int auctionId;
    private final String itemName;
    private final double myLastBid;
    private final double currentHighest;
    private final String status;
    private final String result;

    public BidderAuctionRow(int auctionId, String itemName, double myLastBid, double currentHighest, String status, String result) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.myLastBid = myLastBid;
        this.currentHighest = currentHighest;
        this.status = status;
        this.result = result;
    }

    public int getAuctionId() { return auctionId; }
    public String getItemName() { return itemName; }

    // Trả về chuỗi đã định dạng để hiển thị trong TableView
    public String getMyLastBidStr() { return String.format("%,.0f ₫", myLastBid); }
    public String getCurrentHighestStr() { return String.format("%,.0f ₫", currentHighest); }

    public String getStatus() { return status; }
    public String getResult() { return result; }
}

