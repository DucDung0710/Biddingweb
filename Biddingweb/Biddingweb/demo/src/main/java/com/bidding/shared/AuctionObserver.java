package com.bidding.shared;

public interface AuctionObserver {
    void update(String message);
    int getUserId();
}
