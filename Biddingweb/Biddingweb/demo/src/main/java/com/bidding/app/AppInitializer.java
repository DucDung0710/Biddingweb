package com.bidding.app;

import com.bidding.database.DatabaseConnection;
import com.bidding.service.AuctionService;
import com.bidding.service.WalletService;
import com.bidding.shared.UserManager;
import com.bidding.shared.ItemManager;
import com.bidding.shared.WalletManager;
import com.bidding.shared.Users;

/**
 * AppInitializer - Khởi tạo toàn bộ backend services khi app start
 *
 * Trách nhiệm:
 * 1. Tạo instance của các Managers (UserManager, ItemManager, WalletManager)
 * 2. Khởi tạo Services (AuctionService, WalletService)
 * 3. Tạo mock data nếu cần cho development
 */
public class AppInitializer {

    // Singleton Managers
    private static UserManager userManager;
    private static ItemManager itemManager;
    private static WalletManager walletManager;

    /**
     * Khởi tạo toàn bộ hệ thống backend
     * Gọi method này trong BiddingApplication.start()
     */
    public static void initialize() {
        System.out.println("[AppInitializer] Khởi tạo hệ thống backend...");

        // Khởi tạo DB connection nếu cần
        DatabaseConnection.getInstance();

        // 1. Tạo các managers
        userManager = new UserManager();
        itemManager = new ItemManager();
        walletManager = new WalletManager();

        // 2. Khởi tạo Services
        AuctionService.initialize(walletManager, itemManager);
        WalletService.initialize(walletManager);

        // 3. Tạo mock data cho development (optional)
        initializeMockData();

        System.out.println("[AppInitializer] ✓ Khởi tạo thành công!");
    }

    /**
     * Tạo mock data cho testing (optional - có thể remove sau)
     */
    private static void initializeMockData() {
        try {
            System.out.println("[AppInitializer] Tạo mock data...");

            // Đăng ký user mẫu
            userManager.signUp("admin", "admin123", "admin@vnu.edu.vn", "Admin");
            userManager.signUp("seller", "seller123", "seller@example.com", "Seller");
            userManager.signUp("bidder", "bidder123", "bidder@example.com", "Bidder");

            Users admin = userManager.signIn("admin", "admin123");
            Users seller = userManager.signIn("seller", "seller123");
            Users bidder = userManager.signIn("bidder", "bidder123");

            if (admin == null || seller == null || bidder == null) {
                System.err.println("[AppInitializer] ✗ Không thể tạo đủ mock users.");
                return;
            }

            // Tạo ví
            walletManager.registerNewWallet(admin);
            walletManager.registerNewWallet(seller);
            walletManager.registerNewWallet(bidder);

            // Nạp tiền test
            walletManager.depositDirectly(admin.getId(), 1000000);
            walletManager.depositDirectly(seller.getId(), 500000);
            walletManager.depositDirectly(bidder.getId(), 5000000);

            // Set current admin cho AuctionService
            AuctionService.getInstance().setCurrentAdmin(admin);

            System.out.println("[AppInitializer] ✓ Mock data tạo thành công!");
            System.out.println("  - Admin: " + admin.getUsername() + " (Balance: 1,000,000)");
            System.out.println("  - Seller: " + seller.getUsername() + " (Balance: 500,000)");
            System.out.println("  - Bidder: " + bidder.getUsername() + " (Balance: 5,000,000)");

        } catch (Exception e) {
            System.err.println("[AppInitializer] ✗ Lỗi tạo mock data: " + e.getMessage());
        }
    }

    // ==================== GETTERS ====================

    public static UserManager getUserManager() {
        return userManager;
    }

    public static ItemManager getItemManager() {
        return itemManager;
    }

    public static WalletManager getWalletManager() {
        return walletManager;
    }

    public static AuctionService getAuctionService() {
        return AuctionService.getInstance();
    }

    public static WalletService getWalletService() {
        return WalletService.getInstance();
    }

    /**
     * Shutdown toàn bộ services (gọi khi app đóng)
     */
    public static void shutdown() {
        System.out.println("[AppInitializer] Đóng các services...");
        AuctionService.getInstance().shutdown();
        System.out.println("[AppInitializer] ✓ Shutdown complete");
    }
}
