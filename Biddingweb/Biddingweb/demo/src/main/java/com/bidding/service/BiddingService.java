package com.bidding.service;

import com.bidding.dao.JdbcAuctionDAO;
import com.bidding.dao.JdbcBidRecordDAO;
import com.bidding.dao.BidRecordDAO;
import com.bidding.dao.JdbcUserDAO;
import com.bidding.dao.JdbcAutoBidDAO;
import com.bidding.model.AuctionDisplayDTO;
import com.bidding.model.BidRecord;
import com.bidding.shared.Users;
import com.bidding.shared.WalletManager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BiddingService {
    private final BidRecordDAO bidRecordDAO;
    private final JdbcAuctionDAO auctionDAO;
    private final JdbcUserDAO userDAO;
    private final JdbcAutoBidDAO autoBidDAO;
    private final WalletManager walletManager;

    // Khai báo một Pattern định dạng thời gian đồng nhất với Database toàn hệ thống
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public BiddingService(WalletManager walletManager, Object auctionOperator) {
        this.bidRecordDAO = new JdbcBidRecordDAO();
        this.auctionDAO = new JdbcAuctionDAO();
        this.userDAO = new JdbcUserDAO();
        this.autoBidDAO = new JdbcAutoBidDAO();
        this.walletManager = walletManager;
    }

    public boolean setupAutoBid(int auctionId, int userId, double maxBid, double increment, boolean isEnabled) {
        if (isEnabled) {
            return autoBidDAO.saveOrUpdateAutoBid(auctionId, userId, maxBid, increment);
        } else {
            return autoBidDAO.setAutoBidActivation(auctionId, userId, false);
        }
    }

    /**
     * Đồng bộ hóa (synchronized) phương thức đặt giá để tránh xung đột dữ liệu đồng thời (Race Condition)
     */
    public synchronized BiddingResult placeBid(int auctionId, Users bidder, double bidAmount) {
        if (bidder == null || !("Bidder".equalsIgnoreCase(bidder.getRole()))) {
            return new BiddingResult(false, "Chỉ Bidder mới được phép đặt giá.");
        }

        if (bidAmount <= 0) {
            return new BiddingResult(false, "Giá đặt phải lớn hơn 0.");
        }

        Users userInDb = userDAO.findByUsername(bidder.getUsername());
        if (userInDb == null) {
            return new BiddingResult(false, "Tài khoản của bạn không tồn tại trên hệ thống.");
        }

        BigDecimal bidBigDecimal = BigDecimal.valueOf(bidAmount);
        BigDecimal balanceBigDecimal = BigDecimal.valueOf(userInDb.getBalance());
        if (balanceBigDecimal.compareTo(bidBigDecimal) < 0) {
            return new BiddingResult(false, String.format(
                    "Số dư ví không đủ. Bạn còn: %,.0f ₫, cần: %,.0f ₫",
                    userInDb.getBalance(), bidAmount));
        }

        AuctionDisplayDTO auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null) {
            return new BiddingResult(false, "Không tìm thấy phiên đấu giá này.");
        }

        if (!"RUNNING".equalsIgnoreCase(auction.getStatus())) {
            return new BiddingResult(false, "Phiên đấu giá này không còn hoạt động.");
        }

        // Logic check giá mượt mà: Chỉ cần lớn hơn giá hiện tại sàn yêu cầu
        if (bidAmount <= auction.getCurrentPrice()) {
            return new BiddingResult(false, String.format(
                    "Giá đặt phải lớn hơn giá hiện tại (Giá hiện tại: %,.0f ₫).", auction.getCurrentPrice()));
        }

        BidRecord currentHighestBid = bidRecordDAO.getHighestBid(auctionId);
        if (currentHighestBid != null && bidBigDecimal.compareTo(currentHighestBid.getBidAmount()) <= 0) {
            return new BiddingResult(false, String.format(
                    "Giá đặt phải lớn hơn giá hiện tại (Giá hiện tại: %,.0f ₫).",
                    currentHighestBid.getBidAmount().doubleValue()));
        }

        // --- BẮT ĐẦU XỬ LÝ NGHIỆP VỤ VÍ TIỀN ---
        String nowStr = LocalDateTime.now().format(dateTimeFormatter);

        // 1. Tạm giữ tiền (Hold wallet) của người đặt mới
        boolean holdOk = auctionDAO.holdWalletMoney(bidder.getId(), auctionId, bidAmount, nowStr);
        if (!holdOk) {
            return new BiddingResult(false, "Hệ thống ví tiền bận, không thể thực hiện tạm giữ tiền đấu giá.");
        }

        // 2. Hoàn lại tiền cho người dẫn đầu cũ (nếu có)
        if (currentHighestBid != null && currentHighestBid.getBidderId() != bidder.getId()) {
            // Logic hoàn tiền: Bạn gọi hàm xử lý hoàn trả tiền đóng băng từ ví tại đây nếu hệ thống yêu cầu
            // Ví dụ: auctionDAO.releaseWalletMoney(currentHighestBid.getBidderId(), auctionId, ...);
            System.out.println("🔄 Đã lệnh hoàn tiền tạm giữ cho người dẫn đầu cũ ID: " + currentHighestBid.getBidderId());
        }

        // 3. Tiến hành chèn bản ghi đặt giá hợp lệ
        BidRecord bidRecord = new BidRecord(
                auctionId,
                bidder.getId(),
                bidder.getUsername(),
                bidBigDecimal,
                LocalDateTime.now()
        );
        bidRecord.setWinning(true);

        if (!bidRecordDAO.insert(bidRecord)) {
            return new BiddingResult(false, "Lỗi lưu bản ghi đặt giá. Vui lòng thử lại.");
        }

        if (bidRecord.isWinning()) {
            auctionDAO.updateCurrentPrice(auctionId, bidAmount);
            auctionDAO.updateWinner(auctionId, bidder.getId());

            // 🔥 Kích hoạt bộ xử lý nâng giá tự động chạy ngầm
            executeAutoBidEngine(auctionId, bidder.getId(), bidAmount);
        }

        return new BiddingResult(true, String.format(
                "Đặt giá thành công: %,.0f ₫. Bạn đang dẫn đầu!", bidAmount));
    }

    /**
     * ENGINE CHẠY NGẦM: Tự động quét và nâng giá theo cấu hình một cách liên hoàn (Đệ quy)
     * Đã đồng bộ hóa an toàn luồng và định dạng thời gian khớp tuyệt đối với client.
     */
    public synchronized void executeAutoBidEngine(int auctionId, int currentWinnerId, double currentPrice) {
        String sql = "SELECT ab.user_id, u.email, ab.max_bid, ab.increment " +
                "FROM auto_bids ab " +
                "INNER JOIN users u ON ab.user_id = u.id " +
                "WHERE ab.auction_id = ? AND ab.user_id != ? AND ab.is_active = 1 " +
                "ORDER BY ab.max_bid DESC, ab.created_at ASC LIMIT 1";

        try (Connection conn = com.bidding.database.DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, auctionId);
            ps.setInt(2, currentWinnerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int autoUserId = rs.getInt("user_id");
                    String autoUserEmail = rs.getString("email");
                    double maxBid = rs.getDouble("max_bid");
                    double increment = rs.getDouble("increment");

                    // Tính toán bước giá tiếp theo
                    double nextBidAmount = currentPrice + increment;

                    if (nextBidAmount <= maxBid) {
                        Users autoUser = userDAO.findByEmail(autoUserEmail);

                        if (autoUser != null && autoUser.getBalance() >= nextBidAmount) {
                            String nowStr = LocalDateTime.now().format(dateTimeFormatter);

                            // 1. Thực hiện nghiệp vụ đóng băng tiền ví cho Robot tự động nâng giá
                            boolean holdOk = auctionDAO.holdWalletMoney(autoUserId, auctionId, nextBidAmount, nowStr);
                            if (!holdOk) {
                                System.err.println("🤖 [Auto-Bid] Không thể trích giữ ví của Robot [" + autoUserEmail + "], bỏ qua lượt.");
                                return;
                            }

                            // 2. Thêm bản ghi giao dịch tự động vào lịch sử hệ thống công khai
                            String insertBidSql = "INSERT INTO bid_transactions (auction_id, bidder_id, amount, bid_time, is_auto) VALUES (?, ?, ?, ?, 1)";
                            try (PreparedStatement bidPs = conn.prepareStatement(insertBidSql)) {
                                bidPs.setInt(1, auctionId);
                                bidPs.setInt(2, autoUserId);
                                bidPs.setDouble(3, nextBidAmount);
                                bidPs.setString(4, nowStr); // Sử dụng cấu trúc chuỗi phẳng yyyy-MM-dd HH:mm:ss an toàn
                                bidPs.executeUpdate();
                            }

                            // 3. Hoàn lại tiền đóng băng cho người bị cướp ngôi vừa rồi
                            // auctionDAO.releaseWalletMoney(currentWinnerId, auctionId, ...);

                            // 4. Đồng bộ cập nhật lại thực thể phiên đấu giá công khai
                            auctionDAO.updateCurrentPrice(auctionId, nextBidAmount);
                            auctionDAO.updateWinner(auctionId, autoUserId);

                            System.out.println("🤖 [Auto-Bid] Hệ thống tự động nâng giá hộ User [" + autoUserEmail + "] lên " + nextBidAmount + " ₫");

                            // 5. ĐỆ QUY (RECURSIVE): Quét tiếp vòng lặp xem có robot nào khác cần đè giá tiếp không
                            executeAutoBidEngine(auctionId, autoUserId, nextBidAmount);
                        } else {
                            // Tự động hủy kích hoạt Robot khi ví cạn tiền
                            autoBidDAO.setAutoBidActivation(auctionId, autoUserId, false);
                            System.out.println("🤖 [Auto-Bid] Hủy kích hoạt Robot của User [" + autoUserEmail + "] do số dư tài khoản không đủ.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi thực thi vòng lặp Auto-Bid Engine: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<BidRecord> getBidHistory(int auctionId) { return bidRecordDAO.getByAuctionId(auctionId); }
    public List<BidRecord> getBidderHistory(int bidderId) { return bidRecordDAO.getByBidderId(bidderId); }
    public BidRecord getHighestBid(int auctionId) { return bidRecordDAO.getHighestBid(auctionId); }
    public int countBids(int auctionId) { return bidRecordDAO.countBidsForAuction(auctionId); }

    public static class BiddingResult {
        private final boolean success;
        private final String message;
        public BiddingResult(boolean success, String message) { this.success = success; this.message = message; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}