package com.bidding.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.bidding.dao.JdbcUserDAO;
import com.bidding.dao.JdbcWalletDepositRequestDAO;
import com.bidding.dao.JdbcWalletTransactionDAO;
import com.bidding.dao.WalletDepositRequestDAO;
import com.bidding.dao.WalletTransactionDAO;
import com.bidding.shared.Balance;
import com.bidding.shared.Users;
import com.bidding.shared.WalletManager;

/**
 * WalletService - Quản lý các giao dịch ví tiền
 * 
 * Cung cấp các operation liên quan đến ví:
 * - Xem số dư
 * - Nạp tiền
 * - Chuyển tiền
 * - Khóa/mở khóa tiền (khi đặt giá)
 */
public class WalletService {
    private static WalletService instance;
    private final WalletManager walletManager;
    private final WalletTransactionDAO transactionDAO = new JdbcWalletTransactionDAO();
    private final WalletDepositRequestDAO depositRequestDAO = new JdbcWalletDepositRequestDAO();
    private final JdbcUserDAO userDAO = new JdbcUserDAO();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public WalletService() {
        this(new WalletManager());
    }

    private WalletService(WalletManager walletManager) {
        this.walletManager = walletManager;
    }
    
    /**
     * Khởi tạo WalletService
     */
    public static void initialize(WalletManager walletManager) {
        if (instance == null) {
            instance = new WalletService(walletManager);
        }
    }
    
    /**
     * Lấy instance của WalletService
     */
    public static WalletService getInstance() {
        if (instance == null) {
            throw new RuntimeException("WalletService chưa được initialize");
        }
        return instance;
    }
    
    // ==================== QUERY OPERATIONS ====================
    
    /**
     * Xem tổng số dư (available + locked)
     */
    public double getTotalBalance(int userId) {
        Balance wallet = walletManager.getWalletByUserId(userId);
        if (wallet == null) return 0;
        return wallet.getAmount().add(wallet.getLockedAmount()).doubleValue();
    }
    
    /**
     * Xem số dư có thể sử dụng
     */
    public double getAvailableBalance(int userId) {
        Balance wallet = walletManager.getWalletByUserId(userId);
        return wallet != null ? wallet.getAmount().doubleValue() : 0;
    }
    
    /**
     * Xem số tiền đang bị khóa (ví dụ: đặt cọc, đặt giá)
     */
    public double getLockedBalance(int userId) {
        Balance wallet = walletManager.getWalletByUserId(userId);
        return wallet != null ? wallet.getLockedAmount().doubleValue() : 0;
    }
    
    /**
     * Xem chi tiết ví
     */
    public WalletInfo getWalletInfo(int userId) {
        Balance wallet = walletManager.getWalletByUserId(userId);
        if (wallet == null) {
            return new WalletInfo(0, 0, String.valueOf(userId));
        }
        return new WalletInfo(
            wallet.getAmount().doubleValue(),
            wallet.getLockedAmount().doubleValue(),
            String.valueOf(userId)
        );
    }
    
    // ==================== DEPOSIT OPERATIONS ====================
    
    /**
     * Nạp tiền trực tiếp vào ví (không cần duyệt)
     * Thường dùng cho: thẻ cào, admin duyệt
     */
    public String depositDirectly(int userId, double amount) {
        try {
            if (amount <= 0) {
                return "Lỗi: Số tiền phải lớn hơn 0";
            }
            
            walletManager.depositDirectly(userId, amount);
            return "Nạp tiền thành công: " + amount;
        } catch (Exception e) {
            return "Lỗi nạp tiền: " + e.getMessage();
        }
    }
    
    /**
     * Nạp tiền qua Admin (chờ duyệt)
     * Trả về request ID để user theo dõi
     */
    public int requestDeposit(int userId, double amount) {
        try {
            if (amount <= 0) {
                return -1;
            }
            
            // TODO: Implement AdminMethod pattern
            return 1; // Return request ID
        } catch (Exception e) {
            return -1;
        }
    }
    
    // ==================== TRANSFER OPERATIONS ====================
    
    /**
     * Chuyển tiền giữa hai user
     * @return true nếu thành công
     */
    public boolean transfer(int fromUserId, int toUserId, double amount) {
        try {
            if (amount <= 0) {
                return false;
            }
            
            return walletManager.transferMoney(fromUserId, toUserId, amount);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Rút tiền từ ví
     * @return true nếu thành công
     */
    public boolean withdraw(int userId, double amount) {
        try {
            if (amount <= 0) {
                return false;
            }
            
            Balance wallet = walletManager.getWalletByUserId(userId);
            if (wallet == null) {
                return false;
            }
            
            return wallet.withdraw(BigDecimal.valueOf(amount));
        } catch (Exception e) {
            return false;
        }
    }
    
    // ==================== LOCK/UNLOCK OPERATIONS ====================
    
    /**
     * Khóa tiền (khi user đặt giá cao nhất)
     * Tiền khóa là tiền không thể rút được cho đến khi
     * phiên kết thúc hoặc bị vượt giá
     */
    public boolean lockAmount(int userId, double amount) {
        try {
            if (amount <= 0) {
                return false;
            }
            
            Balance wallet = walletManager.getWalletByUserId(userId);
            if (wallet == null || wallet.getAmount().compareTo(BigDecimal.valueOf(amount)) < 0) {
                return false;
            }
            
            wallet.lockAmount(BigDecimal.valueOf(amount));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Mở khóa tiền (khi bị người khác vượt giá)
     */
    public boolean unlockAmount(int userId, double amount) {
        try {
            if (amount <= 0) {
                return false;
            }
            
            Balance wallet = walletManager.getWalletByUserId(userId);
            if (wallet == null) {
                return false;
            }
            
            wallet.unlockAmount(BigDecimal.valueOf(amount));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Commit tiền (chuyển từ locked sang committed khi thua/thắng)
     */
    public double commitLockedAmount(int userId, double amount) {
        try {
            Balance wallet = walletManager.getWalletByUserId(userId);
            if (wallet == null) {
                return 0;
            }
            
            return wallet.commitLockedAmount(BigDecimal.valueOf(amount)).doubleValue();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Kiểm tra xem user có đủ tiền không
     */
    public boolean hasEnoughBalance(int userId, double amount) {
        Balance wallet = walletManager.getWalletByUserId(userId);
        if (wallet == null) {
            return false;
        }
        return wallet.getAmount().compareTo(BigDecimal.valueOf(amount)) >= 0;
    }
    
    // ==================== REGISTRATION ====================
    
    /**
     * Đăng ký ví mới cho user
     */
    public void registerWallet(Users user) {
        walletManager.registerNewWallet(user);
    }

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

        if (!depositRequestDAO.updateDepositRequestStatus(requestId, "APPROVED")) {
            return false;
        }

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
        Users user = userDAO.findById(userId);
        if (user == null) {
            return false;
        }
        String now = LocalDateTime.now().format(dateFormatter);
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

        double newWinnerBalance = winner.getBalance() - amount;
        if (newWinnerBalance < 0) {
            return false;
        }

        if (!userDAO.updateBalance(winnerId, newWinnerBalance)) {
            return false;
        }

        double newSellerBalance = seller.getBalance() + amount;
        if (!userDAO.updateBalance(sellerId, newSellerBalance)) {
            userDAO.updateBalance(winnerId, winner.getBalance());
            return false;
        }

        String now = LocalDateTime.now().format(dateFormatter);
        transactionDAO.insertTransaction(winnerId, "PAYMENT", amount, newWinnerBalance, null,
                "Thanh toán phiên đấu giá: " + auctionNote, now);
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

    /**
     * DTO để truyền thông tin ví qua lại
     */
    public static class WalletInfo {
        public double available;
        public double locked;
        public double total;
        public String userId;
        
        public WalletInfo(double available, double locked, String userId) {
            this.available = available;
            this.locked = locked;
            this.total = available + locked;
            this.userId = userId;
        }
        
        @Override
        public String toString() {
            return String.format("Ví [User: %s | Available: %.0f | Locked: %.0f | Total: %.0f]",
                userId, available, locked, total);
        }
    }
}

