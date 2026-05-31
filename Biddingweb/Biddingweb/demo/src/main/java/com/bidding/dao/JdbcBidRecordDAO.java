package com.bidding.dao;

import com.bidding.database.DatabaseConnection;
import com.bidding.model.BidRecord;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JdbcBidRecordDAO - Triển khai JDBC cho BidRecordDAO
 * Đã sửa đổi để đồng bộ chính xác với bảng `bid_transactions` và `users` trong MySQL.
 */
public class JdbcBidRecordDAO implements BidRecordDAO {

    @Override
    public boolean insert(BidRecord bidRecord) {
        // Bảng bid_transactions gồm các cột: id (AI), auction_id, bidder_id, amount, bid_time, is_auto
        String sql = "INSERT INTO bid_transactions (auction_id, bidder_id, amount, bid_time, is_auto) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, bidRecord.getAuctionId());
            ps.setInt(2, bidRecord.getBidderId());
            ps.setBigDecimal(3, bidRecord.getBidAmount());
            // Vì cột bid_time trong DB đang để kiểu VARCHAR(50), ta lưu dưới dạng String.
            // Nếu sau này bạn đổi sang DATETIME/TIMESTAMP thì dùng ps.setTimestamp thay thế.
            ps.setString(4, bidRecord.getBidTime().toString());
            // Cột is_auto trong DB kiểu tinyint, map với boolean (0 = false, 1 = true)
            ps.setBoolean(5, false); // Mặc định hoặc có thể bổ sung thuộc tính isAuto vào model nếu cần

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        bidRecord.setBidId(rs.getInt(1));
                        System.out.println("✅ Đã thêm bản ghi đặt giá thành công: " + bidRecord);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi thêm bản ghi đặt giá: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<BidRecord> getByAuctionId(int auctionId) {
        List<BidRecord> bidRecords = new ArrayList<>();
        // JOIN với bảng users để lấy username làm bidder_name
        String sql = "SELECT bt.id AS bid_id, bt.auction_id, bt.bidder_id, u.username AS bidder_name, " +
                "bt.amount AS bid_amount, bt.bid_time " +
                "FROM bid_transactions bt " +
                "JOIN users u ON bt.bidder_id = u.id " +
                "WHERE bt.auction_id = ? ORDER BY bt.bid_time ASC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Xử lý chuyển đổi chuỗi thời gian VARCHAR thành LocalDateTime
                    String timeStr = rs.getString("bid_time");
                    LocalDateTime bidTime = (timeStr != null) ? LocalDateTime.parse(timeStr.replace(" ", "T")) : LocalDateTime.now();

                    BidRecord record = new BidRecord(
                            rs.getInt("bid_id"),
                            rs.getInt("auction_id"),
                            rs.getInt("bidder_id"),
                            rs.getString("bidder_name"),
                            rs.getBigDecimal("bid_amount"),
                            bidTime,
                            false // Bảng bid_transactions không lưu trạng thái is_winning trực tiếp
                    );
                    bidRecords.add(record);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi lấy bản ghi đặt giá theo auction: " + e.getMessage());
            e.printStackTrace();
        }
        return bidRecords;
    }

    @Override
    public List<BidRecord> getByBidderId(int bidderId) {
        List<BidRecord> bidRecords = new ArrayList<>();
        String sql = "SELECT bt.id AS bid_id, bt.auction_id, bt.bidder_id, u.username AS bidder_name, " +
                "bt.amount AS bid_amount, bt.bid_time " +
                "FROM bid_transactions bt " +
                "JOIN users u ON bt.bidder_id = u.id " +
                "WHERE bt.bidder_id = ? ORDER BY bt.bid_time DESC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bidderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String timeStr = rs.getString("bid_time");
                    LocalDateTime bidTime = (timeStr != null) ? LocalDateTime.parse(timeStr.replace(" ", "T")) : LocalDateTime.now();

                    BidRecord record = new BidRecord(
                            rs.getInt("bid_id"),
                            rs.getInt("auction_id"),
                            rs.getInt("bidder_id"),
                            rs.getString("bidder_name"),
                            rs.getBigDecimal("bid_amount"),
                            bidTime,
                            false
                    );
                    bidRecords.add(record);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi lấy bản ghi đặt giá theo bidder: " + e.getMessage());
            e.printStackTrace();
        }
        return bidRecords;
    }

    @Override
    public BidRecord getHighestBid(int auctionId) {
        String sql = "SELECT bt.id AS bid_id, bt.auction_id, bt.bidder_id, u.username AS bidder_name, " +
                "bt.amount AS bid_amount, bt.bid_time " +
                "FROM bid_transactions bt " +
                "JOIN users u ON bt.bidder_id = u.id " +
                "WHERE bt.auction_id = ? ORDER BY bt.amount DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String timeStr = rs.getString("bid_time");
                    LocalDateTime bidTime = (timeStr != null) ? LocalDateTime.parse(timeStr.replace(" ", "T")) : LocalDateTime.now();

                    BidRecord highestBid = new BidRecord(
                            rs.getInt("bid_id"),
                            rs.getInt("auction_id"),
                            rs.getInt("bidder_id"),
                            rs.getString("bidder_name"),
                            rs.getBigDecimal("bid_amount"),
                            bidTime,
                            true // Đây chính xác là lượt đặt giá cao nhất hiện tại
                    );
                    return highestBid;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi lấy giá cao nhất: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int countBidsForAuction(int auctionId) {
        String sql = "SELECT COUNT(*) FROM bid_transactions WHERE auction_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi đếm số lượt đặt giá: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
}