package com.bidding.service;

import com.bidding.dao.JdbcAuctionDAO;
import com.bidding.dao.JdbcBidDAO;
import com.bidding.database.DatabaseConnection;
import com.bidding.model.AuctionDisplayDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * BidService — xử lý toàn bộ logic nghiệp vụ đấu giá.
 *
 * Các chức năng:
 *  - placeBid()    : đặt giá thủ công, có transaction + lock để tránh race condition
 *  - setAutoBid()  : đăng ký auto-bid (maxBid + increment)
 *  - processAutoBids(): tự động kích hoạt auto-bid khi có bid mới từ đối thủ
 */
public class BidService {

    private final JdbcBidDAO     bidDao     = new JdbcBidDAO();
    private final JdbcAuctionDAO auctionDao = new JdbcAuctionDAO();

    // ── ĐẶT GIÁ THỦ CÔNG ─────────────────────────────────────────

    /**
     * Đặt giá cho 1 phiên đấu giá.
     * Toàn bộ logic chạy trong 1 transaction để đảm bảo ACID,
     * tránh lost update khi nhiều bidder đặt giá cùng lúc.
     *
     * @return true nếu đặt giá thành công
     * @throws Exception nếu giá không hợp lệ hoặc phiên đã đóng
     */
    public synchronized boolean placeBid(int auctionId, int bidderId, double amount)
            throws Exception {

        Connection conn = DatabaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false); // bắt đầu transaction

        try {
            // 1. Đọc trạng thái phiên đấu giá (có lock để tránh race condition)
            String checkSql = """
                SELECT current_price, status
                FROM auctions
                WHERE id = ?
                """;
            double currentPrice;
            String status;

            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setInt(1, auctionId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new Exception("Phiên đấu giá không tồn tại!");
                currentPrice = rs.getDouble("current_price");
                status       = rs.getString("status");
            }

            // 2. Kiểm tra trạng thái phiên
            if (!"RUNNING".equalsIgnoreCase(status)) {
                throw new Exception("Phiên đấu giá đã đóng hoặc chưa bắt đầu!");
            }

            // 3. Kiểm tra giá hợp lệ
            if (amount <= currentPrice) {
                throw new Exception(String.format(
                        "Giá đấu phải cao hơn giá hiện tại (%.0f ₫)!", currentPrice));
            }

            // 4. Cập nhật giá mới và người dẫn đầu vào bảng auctions
            String updateSql = """
                UPDATE auctions
                SET current_price = ?, winner_id = ?
                WHERE id = ?
                """;
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setDouble(1, amount);
                ps.setInt(2, bidderId);
                ps.setInt(3, auctionId);
                ps.executeUpdate();
            }

            // 5. Lưu lịch sử bid vào bảng bid_transactions
            bidDao.insert(auctionId, bidderId, amount, false);

            conn.commit(); // hoàn thành transaction

            // 6. Sau khi commit, kiểm tra và kích hoạt auto-bid của đối thủ
            processAutoBids(auctionId, bidderId, amount);

            return true;

        } catch (Exception e) {
            conn.rollback(); // hoàn tác nếu có lỗi
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── AUTO-BID ─────────────────────────────────────────────────

    /**
     * Đăng ký cấu hình auto-bid cho 1 người dùng trong 1 phiên.
     * Nếu đã có record → cập nhật, chưa có → thêm mới.
     */
    public boolean setAutoBid(int auctionId, int bidderId, double maxBid, double increment)
            throws Exception {

        if (maxBid <= 0 || increment <= 0) {
            throw new Exception("Giá tối đa và bước giá phải lớn hơn 0!");
        }

        Connection conn = DatabaseConnection.getInstance().getConnection();

        // Dùng INSERT OR REPLACE để upsert (SQLite syntax)
        String sql = """
            INSERT INTO auto_bids (auction_id, bidder_id, max_bid, increment, registered_at)
            VALUES (?, ?, ?, ?, datetime('now','localtime'))
            ON CONFLICT(auction_id, bidder_id)
            DO UPDATE SET max_bid = excluded.max_bid,
                          increment = excluded.increment,
                          registered_at = excluded.registered_at
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setInt(2, bidderId);
            ps.setDouble(3, maxBid);
            ps.setDouble(4, increment);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new Exception("Lỗi thiết lập auto-bid: " + e.getMessage());
        }
    }

    /**
     * Sau mỗi bid thành công, kiểm tra xem có auto-bid nào của đối thủ
     * cần được kích hoạt không.
     *
     * Logic:
     *  - Lấy tất cả auto-bid trong phiên (trừ người vừa bid)
     *  - Sắp xếp theo maxBid giảm dần, registered_at tăng dần (ưu tiên đăng ký trước)
     *  - Nếu auto-bid có maxBid > currentPrice → đặt giá tự động (currentPrice + increment)
     *  - Không được vượt quá maxBid
     */
    private void processAutoBids(int auctionId, int lastBidderId, double currentPrice)
            throws Exception {

        String sql = """
            SELECT ab.bidder_id, ab.max_bid, ab.increment
            FROM auto_bids ab
            WHERE ab.auction_id = ?
              AND ab.bidder_id != ?
              AND ab.max_bid > ?
            ORDER BY ab.max_bid DESC, ab.registered_at ASC
            LIMIT 1
            """;

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setInt(2, lastBidderId);
            ps.setDouble(3, currentPrice);

            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return; // không có auto-bid nào đủ điều kiện

            int    autoBidderId = rs.getInt("bidder_id");
            double maxBid       = rs.getDouble("max_bid");
            double increment    = rs.getDouble("increment");

            // Tính giá tự động đặt = currentPrice + increment, không vượt maxBid
            double autoAmount = Math.min(currentPrice + increment, maxBid);

            if (autoAmount <= currentPrice) return; // vẫn không vượt được

            // Đặt giá tự động — gọi lại placeBid với flag isAuto = true
            placeBidAuto(auctionId, autoBidderId, autoAmount);

        } catch (SQLException e) {
            System.err.println("[AutoBid] Lỗi xử lý auto-bid: " + e.getMessage());
        }
    }

    /**
     * Đặt giá tự động (internal) — tương tự placeBid nhưng đánh dấu is_auto = true
     * và không trigger lại processAutoBids để tránh vòng lặp vô tận.
     */
    private synchronized void placeBidAuto(int auctionId, int bidderId, double amount)
            throws Exception {

        Connection conn = DatabaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            // Kiểm tra nhanh giá hiện tại
            String checkSql = "SELECT current_price, status FROM auctions WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setInt(1, auctionId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return;
                double currentPrice = rs.getDouble("current_price");
                String status       = rs.getString("status");
                if (!"RUNNING".equalsIgnoreCase(status)) return;
                if (amount <= currentPrice) return;
            }

            // Cập nhật giá
            String updateSql = "UPDATE auctions SET current_price=?, winner_id=? WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setDouble(1, amount);
                ps.setInt(2, bidderId);
                ps.setInt(3, auctionId);
                ps.executeUpdate();
            }

            // Lưu lịch sử — đánh dấu is_auto = true
            bidDao.insert(auctionId, bidderId, amount, true);

            conn.commit();

            // Broadcast auto-bid này lên tất cả client đang xem phiên
            com.bidding.server.ServerMain.broadcastBidUpdate(auctionId, amount, bidderId);

            System.out.printf("[AutoBid] Bidder #%d tự động đặt giá %.0f ₫ cho phiên #%d%n",
                    bidderId, amount, auctionId);

        } catch (Exception e) {
            conn.rollback();
            System.err.println("[AutoBid] Lỗi đặt giá tự động: " + e.getMessage());
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── TIỆN ÍCH ─────────────────────────────────────────────────

    /**
     * Kiểm tra xem một user có đang dẫn đầu phiên này không.
     */
    public boolean isLeading(int auctionId, int userId) throws SQLException {
        String sql = "SELECT winner_id FROM auctions WHERE id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt("winner_id") == userId;
        }
    }
}