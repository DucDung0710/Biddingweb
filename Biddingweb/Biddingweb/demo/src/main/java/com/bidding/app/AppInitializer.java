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
