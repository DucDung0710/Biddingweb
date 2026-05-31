package com.bidding.shared;

/**
 * Observer pattern cho auction events
 * Bất kì ai tham gia vào đấu giá đều nhận thông báo về:
 * - Bid cao hơn được đặt
 * - Người chiến thắng cuối cùng
 * - Đấu giá kết thúc (bán thành công hay không)
 */
public interface AuctionObserver {
    /**
     * Nhận thông báo bid mới trong đấu giá
     * @param auctionId ID của đấu giá
     * @param bidderId ID của người đặt bid
     * @param bidAmount Số tiền bid
     * @param isAutoBid true nếu là auto-bid
     * @param message Thông điệp mô tả
     */
    void onNewBid(int auctionId, int bidderId, double bidAmount, boolean isAutoBid, String message);
    
    /**
     * Nhận thông báo khi đấu giá kết thúc (bán thành công)
     * @param auctionId ID của đấu giá
     * @param winnerId ID của người chiến thắng
     * @param finalPrice Giá cuối cùng
     * @param message Thông điệp mô tả
     */
    void onAuctionEnded(int auctionId, int winnerId, double finalPrice, String message);
    
    /**
     * Nhận thông báo khi đấu giá bị hủy (không bán)
     * @param auctionId ID của đấu giá
     * @param reason Lý do hủy
     */
    void onAuctionCancelled(int auctionId, String reason);
    
    int getUserId();
}
