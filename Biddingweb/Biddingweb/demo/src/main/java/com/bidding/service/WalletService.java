package com.bidding.service;

import com.bidding.shared.Balance;
import com.bidding.shared.WalletManager;
import com.bidding.shared.Users;
import java.math.BigDecimal;

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
