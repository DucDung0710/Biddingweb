package com.bidding.service;

import com.bidding.dao.JdbcAuctionDAO;
import com.bidding.dao.JdbcBidRecordDAO;
import com.bidding.model.BidRecord;
import com.bidding.model.BidderAuctionRow;
import com.bidding.model.AuctionDisplayDTO;
import java.util.*;

public class AuctionHistoryService {
    private final JdbcBidRecordDAO bidRecordDAO = new JdbcBidRecordDAO();
    private final JdbcAuctionDAO auctionDAO = new JdbcAuctionDAO();

    /**
     * Lấy toàn bộ lịch sử đấu giá của một người dùng và tự động tính toán trạng thái kết quả.
     */
    public List<BidderAuctionRow> getAuctionHistoryForUser(int userId) {
        List<BidderAuctionRow> historyList = new ArrayList<>();

        // 1. Lấy toàn bộ các lượt bid của user này
        List<BidRecord> bids = bidRecordDAO.getByBidderId(userId);
        if (bids == null || bids.isEmpty()) {
            return historyList;
        }

        // 2. Lọc ra lượt đặt giá cuối cùng (mới nhất) của user cho mỗi phiên đấu giá
        Map<Integer, BidRecord> lastBidPerAuction = new LinkedHashMap<>();
        for (BidRecord b : bids) {
            lastBidPerAuction.putIfAbsent(b.getAuctionId(), b);
        }

        // 3. Duyệt qua từng phiên để tính toán kết quả (Business Logic)
        for (Map.Entry<Integer, BidRecord> entry : lastBidPerAuction.entrySet()) {
            int auctionId = entry.getKey();
            BidRecord myLast = entry.getValue();

            // Lấy giá cao nhất hiện tại của phiên
            BidRecord highest = bidRecordDAO.getHighestBid(auctionId);
            double currentHighest = (highest != null) ? highest.getBidAmount().doubleValue() : 0;

            // Lấy thông tin phiên đấu giá
            AuctionDisplayDTO auction = auctionDAO.getAuctionById(auctionId);
            String status = (auction != null) ? auction.getStatus() : "N/A";

            // Tách Logic nghiệp vụ phân định kết quả
            String result = determineBidResult(status, auction, userId, myLast.getBidAmount().doubleValue(), currentHighest);
            String itemName = (auction != null) ? auction.getItemName() : ("#" + auctionId);

            // Thêm vào danh sách kết quả xử lý dữ liệu sạch
            BidderAuctionRow row = new BidderAuctionRow(
                    auctionId,
                    itemName,
                    myLast.getBidAmount().doubleValue(),
                    currentHighest,
                    status,
                    result
            );
            historyList.add(row);
        }
        return historyList;
    }

    /**
     * Logic nghiệp vụ định đoạt trạng thái đấu giá (Thắng, Thua, Đang dẫn đầu, Bị vượt)
     */
    private String determineBidResult(String status, AuctionDisplayDTO auction, int userId, double myLastBid, double currentHighest) {
        if ("FINISHED".equals(status)) {
            if (auction != null && auction.getWinnerId() == userId) {
                return "Thắng";
            } else {
                return "Thua";
            }
        } else {
            if (myLastBid >= currentHighest) {
                return "Đang dẫn đầu";
            } else {
                return "Bị vượt";
            }
        }
    }
}