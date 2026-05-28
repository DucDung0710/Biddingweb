package com.bidding.dao;

import com.bidding.database.DatabaseConnection;
import com.bidding.model.BidHistoryDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO xử lý các thao tác liên quan tới bảng bid_transactions.
 */
public class JdbcBidDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Lưu một lượt đặt giá mới.
     * Được gọi từ BidService.placeBid() trong transaction.
     */
    public boolean insert(int auctionId, int bidderId, double amount, boolean isAuto)
            throws SQLException {
        String sql = """
            INSERT INTO bid_transactions (auction_id, bidder_id, amount, bid_time, is_auto)
            VALUES (?, ?, ?, datetime('now','localtime'), ?)
            """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setInt(2, bidderId);
            ps.setDouble(3, amount);
            ps.setInt(4, isAuto ? 1 : 0);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Lấy lịch sử đặt giá của 1 phiên, sắp xếp mới nhất lên đầu.
     * Join với bảng users để lấy tên người đặt giá.
     */
    public List<BidHistoryDTO> getHistory(int auctionId) throws SQLException {
        String sql = """
            SELECT bt.id, bt.auction_id, u.username AS bidder_name,
                   bt.amount, bt.bid_time, bt.is_auto
            FROM bid_transactions bt
            JOIN users u ON u.id = bt.bidder_id
            WHERE bt.auction_id = ?
            ORDER BY bt.bid_time DESC
            LIMIT 50
            """;
        List<BidHistoryDTO> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new BidHistoryDTO(
                        rs.getInt("id"),
                        rs.getInt("auction_id"),
                        rs.getString("bidder_name"),
                        rs.getDouble("amount"),
                        rs.getString("bid_time"),
                        rs.getInt("is_auto") == 1
                ));
            }
        }
        return list;
    }

    /**
     * Lấy giá cao nhất hiện tại của 1 phiên đấu giá.
     */
    public double getMaxBid(int auctionId) throws SQLException {
        String sql = "SELECT MAX(amount) FROM bid_transactions WHERE auction_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }
}