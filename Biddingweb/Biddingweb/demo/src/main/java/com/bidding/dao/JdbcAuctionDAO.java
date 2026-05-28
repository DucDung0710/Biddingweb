package com.bidding.dao;

import com.bidding.database.DatabaseConnection;
import com.bidding.model.AuctionDisplayDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcAuctionDAO {

    // Lấy chi tiết một phiên đấu giá theo ID kèm thông tin sản phẩm, người bán, người dẫn đầu và lượt bid
    public AuctionDisplayDTO findById(int auctionId) {
        String q = "SELECT a.id, " +
                "       i.ItemName, " +
                "       i.type AS item_type, " +
                "       a.item_id, " +
                "       a.start_price, " +
                "       a.current_price, " +
                "       a.start_time, " +
                "       a.end_time, " +
                "       a.status, " +
                "       a.winner_id, " +
                "       u_seller.username AS seller_name, " +
                "       u_winner.username AS leader_name, " +
                "       (SELECT COUNT(*) FROM bid_transactions WHERE auction_id = a.id) AS bid_count " +
                "FROM auctions a " +
                "JOIN items i ON a.item_id = i.id " +
                "JOIN users u_seller ON i.seller_id = u_seller.id " +
                "LEFT JOIN users u_winner ON a.winner_id = u_winner.id " +
                "WHERE a.id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setInt(1, auctionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        AuctionDisplayDTO dto = new AuctionDisplayDTO();
                        dto.setAuctionId(rs.getInt("id"));
                        dto.setItemName(rs.getString("ItemName"));
                        dto.setItemId(rs.getInt("item_id"));
                        dto.setStartPrice(rs.getDouble("start_price"));
                        dto.setCurrentPrice(rs.getDouble("current_price"));
                        dto.setStartTime(rs.getString("start_time"));
                        dto.setEndTime(rs.getString("end_time"));
                        dto.setStatus(rs.getString("status"));
                        dto.setWinnerId(rs.getInt("winner_id"));
                        dto.setItemType(rs.getString("item_type"));
                        dto.setSellerName(rs.getString("seller_name"));

                        String leader = rs.getString("leader_name");
                        dto.setLeaderName(leader != null ? leader : "Chưa có");
                        dto.setBidCount(rs.getInt("bid_count"));

                        return dto;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 1. Hàm đếm số phiên đang chạy (Status = 'RUNNING')
    public int countActiveAuctions() {
        String q = "SELECT COUNT(*) FROM auctions WHERE status = 'RUNNING'";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(q);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // 2. Hàm đếm số phiên người dùng này đã thắng
    public int countWonAuctions(int userId) {
        String q = "SELECT COUNT(*) FROM auctions WHERE winner_id = ? AND status = 'FINISHED'";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // 3. LẤY TOÀN BỘ PHIÊN ĐANG CHẠY ĐỂ ĐỔ LÊN GIAO DIỆN (Kèm thông tin mở rộng)
    public List<AuctionDisplayDTO> getActiveAuctionsWithItems() {
        List<AuctionDisplayDTO> list = new ArrayList<>();
        String q = "SELECT a.id, i.ItemName, i.type AS item_type, a.item_id, a.start_price, a.current_price, " +
                "a.start_time, a.end_time, a.status, a.winner_id, " +
                "u_seller.username AS seller_name, u_winner.username AS leader_name, " +
                "(SELECT COUNT(*) FROM bid_transactions WHERE auction_id = a.id) AS bid_count " +
                "FROM auctions a " +
                "JOIN items i ON a.item_id = i.id " +
                "JOIN users u_seller ON i.seller_id = u_seller.id " +
                "LEFT JOIN users u_winner ON a.winner_id = u_winner.id " +
                "WHERE a.status = 'RUNNING'";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(q);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuctionDisplayDTO dto = new AuctionDisplayDTO();
                    dto.setAuctionId(rs.getInt("id"));
                    dto.setItemName(rs.getString("ItemName"));
                    dto.setItemId(rs.getInt("item_id"));
                    dto.setStartPrice(rs.getDouble("start_price"));
                    dto.setCurrentPrice(rs.getDouble("current_price"));
                    dto.setStartTime(rs.getString("start_time"));
                    dto.setEndTime(rs.getString("end_time"));
                    dto.setStatus(rs.getString("status"));
                    dto.setWinnerId(rs.getInt("winner_id"));
                    dto.setItemType(rs.getString("item_type"));
                    dto.setSellerName(rs.getString("seller_name"));

                    String leader = rs.getString("leader_name");
                    dto.setLeaderName(leader != null ? leader : "Chưa có");
                    dto.setBidCount(rs.getInt("bid_count"));

                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 4. Hàm lọc phiên đấu giá từ SQLite dựa trên từ khóa, trạng thái và loại sản phẩm
    public List<AuctionDisplayDTO> getAuctionsByFilter(String keyword, String status, String type) {
        List<AuctionDisplayDTO> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT a.id, i.ItemName, i.type AS item_type, a.item_id, a.start_price, a.current_price, " +
                        "a.start_time, a.end_time, a.status, a.winner_id, " +
                        "u_seller.username AS seller_name, u_winner.username AS leader_name, " +
                        "(SELECT COUNT(*) FROM bid_transactions WHERE auction_id = a.id) AS bid_count " +
                        "FROM auctions a " +
                        "JOIN items i ON a.item_id = i.id " +
                        "JOIN users u_seller ON i.seller_id = u_seller.id " +
                        "LEFT JOIN users u_winner ON a.winner_id = u_winner.id WHERE 1=1 "
        );

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND i.ItemName LIKE ? ");
        }
        if (status != null && !status.equals("Tất cả")) {
            sql.append("AND a.status = ? ");
        }
        if (type != null && !type.equals("Tất cả")) {
            sql.append("AND i.type = ? ");
        }

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int paramIndex = 1;

                if (keyword != null && !keyword.trim().isEmpty()) {
                    ps.setString(paramIndex++, "%" + keyword.trim() + "%");
                }
                if (status != null && !status.equals("Tất cả")) {
                    String dbStatus = switch (status) {
                        case "Đang diễn ra" -> "RUNNING";
                        case "Sắp bắt đầu" -> "OPEN";
                        case "Đã kết thúc" -> "FINISHED";
                        default -> "RUNNING";
                    };
                    ps.setString(paramIndex++, dbStatus);
                }
                if (type != null && !type.equals("Tất cả")) {
                    ps.setString(paramIndex++, type.trim());
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        AuctionDisplayDTO dto = new AuctionDisplayDTO();
                        dto.setAuctionId(rs.getInt("id"));
                        dto.setItemName(rs.getString("ItemName"));
                        dto.setItemId(rs.getInt("item_id"));
                        dto.setStartPrice(rs.getDouble("start_price"));
                        dto.setCurrentPrice(rs.getDouble("current_price"));
                        dto.setStartTime(rs.getString("start_time"));
                        dto.setEndTime(rs.getString("end_time"));
                        dto.setStatus(rs.getString("status"));
                        dto.setWinnerId(rs.getInt("winner_id"));
                        dto.setItemType(rs.getString("item_type"));
                        dto.setSellerName(rs.getString("seller_name"));

                        String leader = rs.getString("leader_name");
                        dto.setLeaderName(leader != null ? leader : "Chưa có");
                        dto.setBidCount(rs.getInt("bid_count"));

                        list.add(dto);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lọc dữ liệu tại hàm getAuctionsByFilter:");
            e.printStackTrace();
        }
        return list;
    }

    // 5. Cập nhật giá cao nhất thời gian thực khi có người Bid thành công
    public boolean updateBidPrice(int auctionId, double newPrice, int bidderId) {
        String q = "UPDATE auctions SET current_price = ?, winner_id = ? WHERE id = ? AND status = 'RUNNING'";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setDouble(1, newPrice);
                ps.setInt(2, bidderId);
                ps.setInt(3, auctionId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 6. Hàm ghi Log giao dịch đặt giá vào bảng bid_transactions
    public boolean insertTransaction(int auctionId, int bidderId, double amount, String time, int isAuto) {
        String q = "INSERT INTO bid_transactions (auction_id, bidder_id, amount, bid_time, is_auto) VALUES (?, ?, ?, ?, ?)";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setInt(1, auctionId);
                ps.setInt(2, bidderId);
                ps.setDouble(3, amount);
                ps.setString(4, time);
                ps.setInt(5, isAuto);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 7. Tạm giữ tiền đóng băng của người dùng khi ra giá cao nhất
    public boolean holdWalletMoney(int userId, int auctionId, double amount, String createdAt) {
        String insertHold = "INSERT INTO wallet_holds (user_id, auction_id, amount, status) VALUES (?, ?, ?, 'ACTIVE')";
        String insertLog = "INSERT INTO wallet_transactions (user_id, type, amount, balance_after, ref_id, note, created_at) "
                + "VALUES (?, 'HOLD', ?, (SELECT balance FROM users WHERE id = ?) - ?, ?, 'Tạm giữ tiền đấu giá', ?)";
        String updateUser = "UPDATE users SET balance = balance - ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(insertHold);
                 PreparedStatement ps2 = conn.prepareStatement(insertLog);
                 PreparedStatement ps3 = conn.prepareStatement(updateUser)) {

                ps1.setInt(1, userId);
                ps1.setInt(2, auctionId);
                ps1.setDouble(3, amount);
                ps1.executeUpdate();

                ps2.setInt(1, userId);
                ps2.setDouble(2, amount);
                ps2.setInt(3, userId);
                ps2.setDouble(4, amount);
                ps2.setInt(5, auctionId);
                ps2.setString(6, createdAt);
                ps2.executeUpdate();

                ps3.setDouble(1, amount);
                ps3.setInt(2, userId);
                ps3.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                if (conn != null) conn.rollback();
                throw e;
            } finally {
                if (conn != null) conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}