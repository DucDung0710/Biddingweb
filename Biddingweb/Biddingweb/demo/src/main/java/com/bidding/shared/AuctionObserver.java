package com.bidding.shared;

/**
 * Giao diện observer cho hệ thống đấu giá.
 * Tách ra file riêng để các package khác (ví dụ com.bidding.engine)
 * có thể tham chiếu (public interface).
 */
public interface AuctionObserver {
    void update(String message);
    String getUserId();
}
