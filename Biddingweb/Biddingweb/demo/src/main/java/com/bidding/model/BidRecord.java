package com.bidding.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BidRecord - Bản ghi một lần đặt giá trong phiên đấu giá
 * Lưu trữ thông tin: bidder, giá, thời gian, trạng thái
 */
public class BidRecord {
    private int bidId;
    private int auctionId;
    private int bidderId;
    private String bidderName;
    private BigDecimal bidAmount;
    private LocalDateTime bidTime;
    private boolean isWinning; // Là giá cao nhất hiện tại?

    public BidRecord() {
    }

    public BidRecord(int auctionId, int bidderId, String bidderName, BigDecimal bidAmount, LocalDateTime bidTime) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.isWinning = false;
    }

    public BidRecord(int bidId, int auctionId, int bidderId, String bidderName, BigDecimal bidAmount, LocalDateTime bidTime, boolean isWinning) {
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.isWinning = isWinning;
    }

    // Getters & Setters
    public int getBidId() {
        return bidId;
    }

    public void setBidId(int bidId) {
        this.bidId = bidId;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getBidderId() {
        return bidderId;
    }

    public void setBidderId(int bidderId) {
        this.bidderId = bidderId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(BigDecimal bidAmount) {
        this.bidAmount = bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }

    public boolean isWinning() {
        return isWinning;
    }

    public void setWinning(boolean winning) {
        isWinning = winning;
    }

    @Override
    public String toString() {
        return String.format("BidRecord{bidId=%d, auctionId=%d, bidderId=%d, bidderName='%s', bidAmount=%s, bidTime=%s, isWinning=%s}",
                bidId, auctionId, bidderId, bidderName, bidAmount, bidTime, isWinning);
    }
}

