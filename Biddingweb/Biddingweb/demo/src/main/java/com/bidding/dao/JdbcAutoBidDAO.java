package com.bidding.dao;

import com.bidding.database.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcAutoBidDAO {

    public boolean saveOrUpdateAutoBid(int auctionId, int userId, double maxBid, double increment) {
        String checkSql = "SELECT id FROM auto_bids WHERE auction_id = ? AND user_id = ?";
        String insertSql = "INSERT INTO auto_bids (auction_id, user_id, max_bid, increment, created_at, is_active) VALUES (?, ?, ?, ?, ?, 1)";
        String updateSql = "UPDATE auto_bids SET max_bid = ?, increment = ?, created_at = ?, is_active = 1 WHERE auction_id = ? AND user_id = ?";

        // Đảm bảo userId truyền vào tồn tại trong DB, thử ép cứng kiểm tra nếu cần
        System.out.println("DEBUG DB: Chuẩn bị lưu AutoBid cho AuctionID=" + auctionId + ", UserID=" + userId + ", MaxBid=" + maxBid);

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setInt(1, auctionId);
                checkPs.setInt(2, userId);
                try (ResultSet rs = checkPs.executeQuery()) {
                    long currentMillis = System.currentTimeMillis();

                    if (rs.next()) {
                        // Thực hiện UPDATE
                        try (PreparedStatement upPs = conn.prepareStatement(updateSql)) {
                            // 💡 GIẢI PHÁP AN TOÀN: Dùng setDouble thay vì setBigDecimal để Driver tự map với DECIMAL của MySQL
                            upPs.setDouble(1, maxBid);
                            upPs.setDouble(2, increment);
                            upPs.setLong(3, currentMillis);
                            upPs.setInt(4, auctionId);
                            upPs.setInt(5, userId);
                            return upPs.executeUpdate() > 0;
                        }
                    } else {
                        // Thực hiện INSERT
                        try (PreparedStatement insPs = conn.prepareStatement(insertSql)) {
                            insPs.setInt(1, auctionId);
                            insPs.setInt(2, userId);
                            insPs.setDouble(3, maxBid); // Dùng setDouble an toàn hơn với số lớn nguyên thủy
                            insPs.setDouble(4, increment);
                            insPs.setLong(5, currentMillis);
                            return insPs.executeUpdate() > 0;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ [DATABASE ERROR] Thất bại tại hàm saveOrUpdateAutoBid:");
            System.err.println("Mã lỗi: " + e.getErrorCode() + " | Nội dung: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean setAutoBidActivation(int auctionId, int userId, boolean isActive) {
        String sql = "UPDATE auto_bids SET is_active = ? WHERE auction_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, isActive);
            ps.setInt(2, auctionId);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ [DATABASE ERROR] Thất bại tại hàm setAutoBidActivation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

}