package com.bidding.service;

import com.bidding.dao.JdbcAuctionDAO;
import com.bidding.dao.JdbcBidRecordDAO;
import com.bidding.dao.BidRecordDAO;
import com.bidding.engine.AuctionOperator;
import com.bidding.model.AuctionDisplayDTO;
import com.bidding.model.BidRecord;
import com.bidding.shared.Balance;
import com.bidding.shared.Users;
import com.bidding.shared.WalletManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * BiddingService - Lớp dịch vụ xử lý logic đấu giá
 * Tuân thủ nguyên tắc Single Responsibility: chỉ chịu trách nhiệm về logic đấu giá
 * Giao diện giữa Controller và DAO/Engine layers
 */
public class BiddingService {
    private final BidRecordDAO bidRecordDAO;
    private final JdbcAuctionDAO auctionDAO;
    private final WalletManager walletManager;
    private final AuctionOperator auctionOperator;

    public BiddingService(WalletManager walletManager, AuctionOperator auctionOperator) {
        this.bidRecordDAO = new JdbcBidRecordDAO();
        this.auctionDAO = new JdbcAuctionDAO();
        this.walletManager = walletManager;
        this.auctionOperator = auctionOperator;
    }

    /**
     * Xử lý logic đặt giá của một bidder
     * 
     * Các bước kiểm tra:
     * 1. Kiểm tra bidder có tồn tại không
     * 2. Kiểm tra bidder có đủ tiền không
     * 3. Kiểm tra giá đặt có hợp lệ không
     * 4. Gọi AuctionOperator để lưu bid vào phiên
     * 5. Lưu bản ghi vào database
     * 
     * @param auctionId ID của phiên đấu giá
     * @param bidder Đối tượng bidder
     * @param bidAmount Số tiền đặt giá
     * @return BiddingResult chứa kết quả chi tiết
     */
    public BiddingResult placeBid(int auctionId, Users bidder, double bidAmount) {
        // Kiểm tra dữ liệu đầu vào
        if (bidder == null || !("Bidder".equalsIgnoreCase(bidder.getRole()))) {
            return new BiddingResult(false, "Chỉ Bidder mới được phép đặt giá.");
        }

        if (bidAmount <= 0) {
            return new BiddingResult(false, "Giá đặt phải lớn hơn 0.");
        }

        // Lấy thông tin ví của bidder
        Balance wallet = walletManager.getWalletByUserId(bidder.getId());
        if (wallet == null) {
            return new BiddingResult(false, "Không tìm thấy ví của bạn trong hệ thống.");
        }

        // Kiểm tra số dư kả dụng
        BigDecimal bidBigDecimal = BigDecimal.valueOf(bidAmount);
        if (wallet.getAmount().compareTo(bidBigDecimal) < 0) {
            return new BiddingResult(false, String.format(
                    "Số dư không đủ. Bạn còn: %,.0f ₫, cần: %,.0f ₫",
                    wallet.getAmount().doubleValue(), bidAmount));
        }

        // Lấy thông tin phiên đấu giá từ database
        AuctionDisplayDTO auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null) {
            return new BiddingResult(false, "Không tìm thấy phiên đấu giá này.");
        }

        // Kiểm tra phiên đang chạy không
        if (!"RUNNING".equalsIgnoreCase(auction.getStatus())) {
            return new BiddingResult(false, "Phiên đấu giá này không còn hoạt động.");
        }

        // Gọi AuctionOperator để xử lý bid (nếu có)
        // Ở đây chúng ta chủ yếu lưu trữ bid vào database
        // Logic thực tế có thể tích hợp với AuctionOperator tùy theo thiết kế

        // Lưu bản ghi bid vào database
        BidRecord bidRecord = new BidRecord(
                auctionId,
                bidder.getId(),
                bidder.getUsername(),
                bidBigDecimal,
                LocalDateTime.now()
        );

        // Kiểm tra xem bid này có phải là cao nhất không
        BidRecord currentHighestBid = bidRecordDAO.getHighestBid(auctionId);
        if (currentHighestBid == null || bidBigDecimal.compareTo(currentHighestBid.getBidAmount()) > 0) {
            bidRecord.setWinning(true);
        }

        if (!bidRecordDAO.insert(bidRecord)) {
            return new BiddingResult(false, "Lỗi lưu bản ghi đặt giá. Vui lòng thử lại.");
        }

        // Cập nhật giá hiện tại trong phiên nếu bid này cao hơn
        if (bidRecord.isWinning()) {
            auctionDAO.updateCurrentPrice(auctionId, bidAmount);
            auctionDAO.updateWinner(auctionId, bidder.getId());
        }

        return new BiddingResult(true, String.format(
                "Đặt giá thành công: %,.0f ₫. Bạn đang dẫn đầu!", bidAmount));
    }

    /**
     * Lấy lịch sử đặt giá của một phiên
     * 
     * @param auctionId ID của phiên đấu giá
     * @return Danh sách bản ghi đặt giá
     */
    public List<BidRecord> getBidHistory(int auctionId) {
        return bidRecordDAO.getByAuctionId(auctionId);
    }

    /**
     * Lấy lịch sử đặt giá của một bidder
     * 
     * @param bidderId ID của bidder
     * @return Danh sách bản ghi đặt giá
     */
    public List<BidRecord> getBidderHistory(int bidderId) {
        return bidRecordDAO.getByBidderId(bidderId);
    }

    /**
     * Lấy giá cao nhất hiện tại của một phiên
     * 
     * @param auctionId ID của phiên đấu giá
     * @return BidRecord của giá cao nhất hoặc null
     */
    public BidRecord getHighestBid(int auctionId) {
        return bidRecordDAO.getHighestBid(auctionId);
    }

    /**
     * Đếm số lượt đặt giá trong một phiên
     * 
     * @param auctionId ID của phiên đấu giá
     * @return Số lượt
     */
    public int countBids(int auctionId) {
        return bidRecordDAO.countBidsForAuction(auctionId);
    }

    /**
     * BiddingResult - Lớp kết quả đặt giá
     * Chứa thông tin thành công/thất bại và thông điệp chi tiết
     */
    public static class BiddingResult {
        private final boolean success;
        private final String message;

        public BiddingResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}

