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

    // 1. Hàm đếm số phiên đang chạy (Status = 'RUNNING')
    public int countActiveAuctions() {
        String q = "SELECT COUNT(*) FROM auctions WHERE status = 'RUNNING'";
        // Đưa Connection vào try-with-resources để tự động trả về pool lập tức khi xong việc
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(q);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // 2. Hàm đếm số phiên người dùng này đã thắng
    public int countWonAuctions(int userId) {
        String q = "SELECT COUNT(*) FROM auctions WHERE winner_id = ? AND status = 'FINISHED'";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    //Hàm kiểm tra và cập nhật phiên hết hạn
    public void updateExpiredAuctions() {
        String q = "UPDATE auctions SET status = 'FINISHED' " +
                "WHERE status = 'RUNNING' AND STR_TO_DATE(LEFT(REPLACE(end_time, 'T', ' '), 19), '%Y-%m-%d %H:%i:%s') < NOW()";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(q)) {
            int updated = ps.executeUpdate();
            if (updated > 0) {
                System.out.println("✅ updateExpiredAuctions: Cập nhật " + updated + " phiên từ RUNNING → FINISHED");
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi update phiên hết hạn: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 3. LẤY TOÀN BỘ PHIÊN ĐANG CHẠY ĐỂ ĐỔ LÊN GIAO DIỆN
    public List<AuctionDisplayDTO> getActiveAuctionsWithItems() {
        updateExpiredAuctions();
        List<AuctionDisplayDTO> list = new ArrayList<>();

        String q = "SELECT a.id AS auction_id, i.id AS item_id, i.name AS item_name, i.description, i.type, " +
                "u.username AS seller_name, a.start_price, a.current_price, " +
                "a.start_time, a.end_time, a.status, a.winner_id " +
                "FROM auctions a " +
                "INNER JOIN items i ON a.item_id = i.id " +
                "INNER JOIN users u ON i.seller_id = u.id " +
                "WHERE a.status = 'RUNNING'";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(q);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AuctionDisplayDTO dto = new AuctionDisplayDTO();

                dto.setAuctionId(rs.getInt("auction_id"));
                dto.setItemId(rs.getInt("item_id"));
                dto.setItemName(rs.getString("item_name"));
                dto.setDescription(rs.getString("description"));
                dto.setType(rs.getString("type"));
                dto.setSellerName(rs.getString("seller_name"));
                dto.setStartPrice(rs.getDouble("start_price"));
                dto.setCurrentPrice(rs.getDouble("current_price"));
                dto.setStartTime(rs.getString("start_time"));
                dto.setEndTime(rs.getString("end_time"));
                dto.setStatus(rs.getString("status"));
                dto.setWinnerId(rs.getInt("winner_id"));

                list.add(dto);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi nghiêm trọng tại hàm getActiveAuctionsWithItems:");
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 4. Hàm lọc phiên đấu giá dựa trên từ khóa, trạng thái và loại sản phẩm
     **/
    public List<AuctionDisplayDTO> getAuctionsByFilter(String keyword, String status, String type) {
        updateExpiredAuctions();
        List<AuctionDisplayDTO> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT a.id AS auction_id, i.id AS item_id, i.name AS item_name, i.description, i.type, " +
                        "u.username AS seller_name, a.start_price, a.current_price, a.start_time, a.end_time, a.status, a.winner_id " +
                        "FROM auctions a " +
                        "INNER JOIN items i ON a.item_id = i.id " +
                        "INNER JOIN users u ON i.seller_id = u.id " +
                        "WHERE 1=1 "
        );

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND i.name LIKE ? "); // Sửa lỗi i.ItemName thành i.name cho khớp DB của bạn
        }
        if (status != null && !status.equals("Tất cả")) {
            sql.append("AND a.status = ? ");
        }
        if (type != null && !type.equals("Tất cả")) {
            sql.append("AND i.type = ? ");
        }

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
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

                    dto.setAuctionId(rs.getInt("auction_id"));
                    dto.setItemId(rs.getInt("item_id"));
                    dto.setItemName(rs.getString("item_name"));
                    dto.setDescription(rs.getString("description"));
                    dto.setType(rs.getString("type"));
                    dto.setSellerName(rs.getString("seller_name"));
                    dto.setStartPrice(rs.getDouble("start_price"));
                    dto.setCurrentPrice(rs.getDouble("current_price"));
                    dto.setStartTime(rs.getString("start_time"));
                    dto.setEndTime(rs.getString("end_time"));
                    dto.setStatus(rs.getString("status"));
                    dto.setWinnerId(rs.getInt("winner_id"));

                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi thực thi dữ liệu tại hàm getAuctionsByFilter:");
            e.printStackTrace();
        }
        return list;
    }

    // 5. Cập nhật giá cao nhất thời gian thực khi có người Bid thành công
    public boolean updateBidPrice(int auctionId, double newPrice, int bidderId) {
        String q = "UPDATE auctions SET current_price = ?, winner_id = ? WHERE id = ? AND status = 'RUNNING'";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setDouble(1, newPrice);
            ps.setInt(2, bidderId);
            ps.setInt(3, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 6. Hàm ghi Log giao dịch đặt giá vào bảng bid_transactions
    public boolean insertTransaction(int auctionId, int bidderId, double amount, String time, int isAuto) {
        String q = "INSERT INTO bid_transactions (auction_id, bidder_id, amount, bid_time, is_auto) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, auctionId);
            ps.setInt(2, bidderId);
            ps.setDouble(3, amount);
            ps.setString(4, time);
            ps.setInt(5, isAuto);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 7. Nghiệp vụ Ví tiền: Đóng băng/Tạm giữ tiền (Transaction được quản lý chặt chẽ)
    public boolean holdWalletMoney(int userId, int auctionId, double amount, String createdAt) {
        String insertHold = "INSERT INTO wallet_holds (user_id, auction_id, amount, status) VALUES (?, ?, ?, 'ACTIVE')";
        String insertLog = "INSERT INTO wallet_transactions (user_id, type, amount, balance_after, ref_id, note, created_at) "
                + "VALUES (?, 'HOLD', ?, (SELECT balance FROM users WHERE id = ?) - ?, ?, 'Tạm giữ tiền đấu giá', ?)";
        String updateUser = "UPDATE users SET balance = balance - ? WHERE id = ?";

        // Để quản lý Transaction (commit/rollback), Connection phải được khai báo và đóng chuẩn chỉ
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(insertHold);
                 PreparedStatement ps2 = conn.prepareStatement(insertLog);
                 PreparedStatement ps3 = conn.prepareStatement(updateUser)) {

                // 1. Thêm vào bảng holds
                ps1.setInt(1, userId);
                ps1.setInt(2, auctionId);
                ps1.setDouble(3, amount);
                ps1.executeUpdate();

                // 2. Ghi log lịch sử giao dịch ví
                ps2.setInt(1, userId);
                ps2.setDouble(2, amount);
                ps2.setInt(3, userId);
                ps2.setDouble(4, amount);
                ps2.setInt(5, auctionId);
                ps2.setString(6, createdAt);
                ps2.executeUpdate();

                // 3. Trừ số dư khả dụng ở bảng users
                ps3.setDouble(1, amount);
                ps3.setInt(2, userId);
                ps3.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback(); // Rollback lập tức nếu bất kỳ lệnh nào lỗi
                throw e;
            } finally {
                conn.setAutoCommit(true); // Trả trạng thái auto-commit về mặc định trước khi giả lại Pool
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 8. LẤY CHI TIẾT MỘT PHIÊN ĐẤU GIÁ THEO ID
    public AuctionDisplayDTO getAuctionById(int auctionId) {
        String q = "SELECT a.id AS auction_id, i.id AS item_id, i.name AS item_name, i.description, i.type, " +
                "u.username AS seller_name, a.start_price, a.current_price, " +
                "a.start_time, a.end_time, a.status, a.winner_id " +
                "FROM auctions a " +
                "INNER JOIN items i ON a.item_id = i.id " +
                "INNER JOIN users u ON i.seller_id = u.id " +
                "WHERE a.id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AuctionDisplayDTO dto = new AuctionDisplayDTO();
                    dto.setAuctionId(rs.getInt("auction_id"));
                    dto.setItemId(rs.getInt("item_id"));
                    dto.setItemName(rs.getString("item_name"));
                    dto.setDescription(rs.getString("description"));
                    dto.setType(rs.getString("type"));
                    dto.setSellerName(rs.getString("seller_name"));
                    dto.setStartPrice(rs.getDouble("start_price"));
                    dto.setCurrentPrice(rs.getDouble("current_price"));
                    dto.setStartTime(rs.getString("start_time"));
                    dto.setEndTime(rs.getString("end_time"));
                    dto.setStatus(rs.getString("status"));
                    dto.setWinnerId(rs.getInt("winner_id"));
                    return dto;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy chi tiết phiên đấu giá:");
            e.printStackTrace();
        }
        return null;
    }

    // 9. Cập nhật giá hiện tại của phiên đấu giá
    public boolean updateCurrentPrice(int auctionId, double newPrice) {
        String q = "UPDATE auctions SET current_price = ? WHERE id = ? AND status = 'RUNNING'";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setDouble(1, newPrice);
            ps.setInt(2, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi cập nhật giá hiện tại:");
            e.printStackTrace();
            return false;
        }
    }

    // 10. Cập nhật người chiến thắng (winner_id)
    public boolean updateWinner(int auctionId, int winnerId) {
        String q = "UPDATE auctions SET winner_id = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, winnerId);
            ps.setInt(2, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi cập nhật người chiến thắng:");
            e.printStackTrace();
            return false;
        }
    }
}