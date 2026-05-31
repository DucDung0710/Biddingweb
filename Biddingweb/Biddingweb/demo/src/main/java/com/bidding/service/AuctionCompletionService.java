package com.bidding.service;

import com.bidding.dao.JdbcAuctionDAO;
import com.bidding.dao.JdbcUserDAO;
import com.bidding.shared.Users;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.bidding.database.DatabaseConnection;

/**
 * Xử lý hoàn thành phiên đấu giá và chuyển tiền:
 * - Kiểm tra phiên kết thúc
 * - Chuyển tiền từ người thắng sang ví seller
 * - Ghi lịch sử giao dịch
 */
public class AuctionCompletionService {
    private final WalletService walletService = new WalletService();
    private final JdbcUserDAO userDAO = new JdbcUserDAO();

    /**
     * Hoàn thành phiên đấu giá: chuyển tiền từ người thắng sang ví seller
     */
    public boolean completeAuction(int auctionId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT a.winner_id, a.current_price, i.seller_id, i.name FROM auctions a " +
                     "JOIN items i ON a.item_id = i.id WHERE a.id = ? AND a.status = 'RUNNING'")) {

            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int winnerId = rs.getInt("winner_id");
                    double finalPrice = rs.getDouble("current_price");
                    int sellerId = rs.getInt("seller_id");
                    String itemName = rs.getString("name");

                    if (winnerId > 0 && sellerId > 0) {
                        // Chuyển tiền từ người thắng sang seller
                        String note = "Mua sản phẩm: " + itemName;
                        if (walletService.transferWinnings(winnerId, sellerId, finalPrice, note)) {
                            // Cập nhật trạng thái phiên thành FINISHED
                            updateAuctionStatus(auctionId, conn);
                            return true;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi hoàn thành phiên đấu giá: " + e.getMessage());
        }
        return false;
    }

    /**
     * Cập nhật trạng thái phiên đấu giá thành FINISHED
     */
    private void updateAuctionStatus(int auctionId, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE auctions SET status = 'FINISHED' WHERE id = ?")) {
            ps.setInt(1, auctionId);
            ps.executeUpdate();
        }
    }

    /**
     * Kiểm tra các phiên đấu giá hết thời gian và tự động hoàn thành
     */
    public void checkAndCompleteExpiredAuctions() {
        String sql = "SELECT id FROM auctions WHERE status = 'RUNNING' AND end_time < NOW()";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int auctionId = rs.getInt("id");
                completeAuction(auctionId);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kiểm tra phiên hết hạn: " + e.getMessage());
        }
    }
}

