package com.bidding.service;

import com.bidding.dao.JdbcWalletTransactionDAO;
import com.bidding.dao.JdbcWalletDepositRequestDAO;
import com.bidding.dao.JdbcUserDAO;
import com.bidding.dao.WalletTransactionDAO;
import com.bidding.dao.WalletDepositRequestDAO;
import com.bidding.shared.Users;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class WalletService {
    private final WalletTransactionDAO transactionDAO = new JdbcWalletTransactionDAO();
    private final WalletDepositRequestDAO depositRequestDAO = new JdbcWalletDepositRequestDAO();
    private final JdbcUserDAO userDAO = new JdbcUserDAO();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Nạp tiền trực tiếp vào ví người dùng (không cần duyệt)
     */
    public boolean depositDirectly(int userId, double amount, String note) {
        Users user = userDAO.findByUsername(String.valueOf(userId));
        if (user == null || amount <= 0) {
            return false;
        }

        double newBalance = user.getBalance() + amount;
        if (!userDAO.updateBalance(userId, newBalance)) {
            return false;
        }

        String now = LocalDateTime.now().format(dateFormatter);
        return transactionDAO.insertTransaction(userId, "DEPOSIT", amount, newBalance, null, note, now);
    }

    /**
     * Rút tiền từ ví người dùng
     */
    public boolean withdraw(int userId, double amount, String note) {
        Users user = userDAO.findByUsername(String.valueOf(userId));
        if (user == null || amount <= 0 || user.getBalance() < amount) {
            return false;
        }

        double newBalance = user.getBalance() - amount;
        if (!userDAO.updateBalance(userId, newBalance)) {
            return false;
        }

        String now = LocalDateTime.now().format(dateFormatter);
        return transactionDAO.insertTransaction(userId, "WITHDRAW", amount, newBalance, null, note, now);
    }

    /**
     * Tạo yêu cầu nạp tiền (chờ admin duyệt)
     */
    public boolean createDepositRequest(int userId, double amount) {
        if (amount <= 0) {
            return false;
        }

        String now = LocalDateTime.now().format(dateFormatter);
        return depositRequestDAO.insertDepositRequest(userId, amount, "PENDING", now);
    }

    /**
     * Admin duyệt yêu cầu nạp tiền
     */
    public boolean approveDepositRequest(int requestId) {
        WalletDepositRequestDAO.DepositRequest request = depositRequestDAO.getDepositRequestById(requestId);
        if (request == null || !"PENDING".equals(request.getStatus())) {
            return false;
        }

        // Cập nhật trạng thái yêu cầu
        if (!depositRequestDAO.updateDepositRequestStatus(requestId, "APPROVED")) {
            return false;
        }

        // Nạp tiền vào ví người dùng
        Users user = userDAO.findById(request.getUserId());
        if (user == null) {
            return false;
        }

        double newBalance = user.getBalance() + request.getAmount();
        if (!userDAO.updateBalance(request.getUserId(), newBalance)) {
            return false;
        }

        String now = LocalDateTime.now().format(dateFormatter);
        transactionDAO.insertTransaction(request.getUserId(), "DEPOSIT", request.getAmount(), newBalance, requestId, "Admin duyệt nạp tiền", now);

        return true;
    }

    /**
     * Admin từ chối yêu cầu nạp tiền
     */
    public boolean rejectDepositRequest(int requestId) {
        WalletDepositRequestDAO.DepositRequest request = depositRequestDAO.getDepositRequestById(requestId);
        if (request == null || !"PENDING".equals(request.getStatus())) {
            return false;
        }

        return depositRequestDAO.updateDepositRequestStatus(requestId, "REJECTED");
    }

    /**
     * Tạm giữ tiền cho phiên đấu giá
     */
    public boolean holdWallet(int userId, double amount, String note) {
        Users user = userDAO.findById(userId);
        if (user == null || amount <= 0 || user.getBalance() < amount) {
            return false;
        }

        String now = LocalDateTime.now().format(dateFormatter);
        return transactionDAO.insertTransaction(userId, "HOLD", amount, user.getBalance(), null, note, now);
    }

    /**
     * Giải phóng tiền từ tạm giữ (hoàn tiền cho người không thắng)
     */
    public boolean releaseHeldWallet(int userId, double amount, String note) {
        String now = LocalDateTime.now().format(dateFormatter);
        Users user = userDAO.findById(userId);
        if (user == null) {
            return false;
        }
        return transactionDAO.insertTransaction(userId, "RELEASE", amount, user.getBalance(), null, note, now);
    }

    /**
     * Chuyển tiền từ người thắng đấu giá sang ví seller (khi phiên kết thúc)
     */
    public boolean transferWinnings(int winnerId, int sellerId, double amount, String auctionNote) {
        Users winner = userDAO.findById(winnerId);
        Users seller = userDAO.findById(sellerId);

        if (winner == null || seller == null || amount <= 0) {
            return false;
        }

        // Giảm tiền người thắng
        double newWinnerBalance = winner.getBalance() - amount;
        if (newWinnerBalance < 0) {
            return false; // Không đủ tiền
        }

        if (!userDAO.updateBalance(winnerId, newWinnerBalance)) {
            return false;
        }

        // Tăng tiền seller
        double newSellerBalance = seller.getBalance() + amount;
        if (!userDAO.updateBalance(sellerId, newSellerBalance)) {
            // Rollback nếu thất bại
            userDAO.updateBalance(winnerId, winner.getBalance());
            return false;
        }

        String now = LocalDateTime.now().format(dateFormatter);

        // Ghi lịch sử trừ tiền cho winner
        transactionDAO.insertTransaction(winnerId, "PAYMENT", amount, newWinnerBalance, null,
                "Thanh toán phiên đấu giá: " + auctionNote, now);

        // Ghi lịch sử cộng tiền cho seller
        transactionDAO.insertTransaction(sellerId, "RECEIVED", amount, newSellerBalance, null,
                "Doanh thu bán hàng: " + auctionNote, now);

        return true;
    }

    /**
     * Lấy số dư khả dụng của người dùng
     */
    public double getBalance(int userId) {
        Users user = userDAO.findById(userId);
        return user != null ? user.getBalance() : 0;
    }

    /**
     * Lấy lịch sử giao dịch
     */
    public List<WalletTransactionDAO.WalletTransaction> getTransactionHistory(int userId, int limit) {
        return transactionDAO.getTransactionHistory(userId, limit);
    }

    /**
     * Lấy danh sách yêu cầu nạp tiền chờ duyệt
     */
    public List<WalletDepositRequestDAO.DepositRequest> getPendingDepositRequests() {
        return depositRequestDAO.getPendingRequests();
    }

    /**
     * Đếm số yêu cầu nạp tiền chờ duyệt
     */
    public int countPendingRequests() {
        return depositRequestDAO.countPendingRequests();
    }

    /**
     * Lấy danh sách yêu cầu nạp tiền theo trạng thái
     */
    public List<WalletDepositRequestDAO.DepositRequest> getDepositRequestsByStatus(String status) {
        return depositRequestDAO.getDepositRequestsByStatus(status);
    }
}

