package com.bidding.dao;

import com.bidding.model.BidRecord;
import java.util.List;

/**
 * BidRecordDAO - Interface định nghĩa các phương thức quản lý lịch sử đặt giá
 * Tuân thủ Interface Segregation Principle (ISP) của SOLID
 */
public interface BidRecordDAO {
    
    /**
     * Thêm một bản ghi đặt giá vào cơ sở dữ liệu
     * @param bidRecord Bản ghi cần thêm
     * @return true nếu thêm thành công
     */
    boolean insert(BidRecord bidRecord);
    
    /**
     * Lấy toàn bộ bản ghi đặt giá của một phiên đấu giá
     * @param auctionId ID của phiên đấu giá
     * @return Danh sách bản ghi theo thứ tự thời gian (cũ nhất trước)
     */
    List<BidRecord> getByAuctionId(int auctionId);
    
    /**
     * Lấy toàn bộ bản ghi đặt giá của một bidder
     * @param bidderId ID của bidder
     * @return Danh sách bản ghi
     */
    List<BidRecord> getByBidderId(int bidderId);
    
    /**
     * Lấy giá cao nhất hiện tại của một phiên đấu giá
     * @param auctionId ID của phiên đấu giá
     * @return Bản ghi đặt giá cao nhất, hoặc null nếu không có
     */
    BidRecord getHighestBid(int auctionId);
    
    /**
     * Đếm số lượt đặt giá trong một phiên
     * @param auctionId ID của phiên đấu giá
     * @return Số lượt
     */
    int countBidsForAuction(int auctionId);
}

