package com.bidding.service;

import java.util.List;

import com.bidding.engine.AuctionOperator;
import com.bidding.shared.Item;
import com.bidding.shared.ItemManager;
import com.bidding.shared.Users;
import com.bidding.shared.WalletManager;

/**
 * AuctionService - Bridge giữa GUI Controllers và Backend Engine
 *
 * Chức năng:
 * - Quản lý lifecycle của auction engine
 * - Cung cấp các operation được gọi từ GUI
 * - Xử lý validation trước khi gọi engine
 * - Trả về kết quả readable cho UI
 *
 * Sử dụng Pattern: Singleton (để maintain state)
 */
public class AuctionService {
    private static AuctionService instance;

    private final WalletManager walletManager;
    private final ItemManager itemManager;
    private final AuctionOperator auctionOperator;
    private Users currentAdmin;

    private AuctionService(WalletManager walletManager, ItemManager itemManager) {
        this.walletManager = walletManager;
        this.itemManager = itemManager;
        this.auctionOperator = new AuctionOperator(walletManager);
        this.currentAdmin = null;
    }

    /**
     * Khởi tạo AuctionService (gọi khi application start)
     */
    public static void initialize(WalletManager walletManager, ItemManager itemManager) {
        if (instance == null) {
            instance = new AuctionService(walletManager, itemManager);
        }
    }

    /**
     * Lấy instance của AuctionService
     */
    public static AuctionService getInstance() {
        if (instance == null) {
            throw new RuntimeException("AuctionService chưa được initialize. Gọi initialize() trước.");
        }
        return instance;
    }

    // ==================== AUCTION OPERATIONS ====================

    public String approveItem(Users admin, int itemId, boolean approve) {
        if (admin == null || !"Admin".equalsIgnoreCase(admin.getRole())) {
            return "Lỗi: Chỉ Admin mới có quyền duyệt sản phẩm.";
        }
        boolean result = itemManager.reviewItem(admin, itemId, approve);
        return result ? "Sản phẩm đã được xử lý." : "Sản phẩm đã bị từ chối hoặc không tồn tại.";
    }

    public String scheduleAuction(int itemId, Users seller, String roomId, String password, long startTimeMillis) {
        // Backwards-compatible: mặc định end time = start + 3 phút
        long defaultEnd = startTimeMillis + (3 * 60 * 1000L);
        return scheduleAuction(itemId, seller, roomId, password, startTimeMillis, defaultEnd);
    }

    public String scheduleAuction(int itemId, Users seller, String roomId, String password, long startTimeMillis, long endTimeMillis) {
        try {
            Item item = itemManager.getItemById(itemId);
            if (item == null) {
                return "Lỗi: Không tìm thấy sản phẩm.";
            }
            if (seller == null || item.getUserId() != seller.getId()) {
                return "Lỗi: Chỉ seller sở hữu sản phẩm mới có thể lên lịch đấu giá.";
            }
            if (!Item.STATUS_APPROVED.equalsIgnoreCase(item.getStatus())) {
                return "Lỗi: Sản phẩm chưa được Admin duyệt.";
            }
            if (currentAdmin == null || !"Admin".equalsIgnoreCase(currentAdmin.getRole())) {
                return "Lỗi: Hệ thống chưa xác định Admin để tạo phiên đấu giá.";
            }
            if (roomId == null || roomId.isEmpty()) {
                return "Lỗi: Room ID không hợp lệ.";
            }

            com.bidding.dao.JdbcAuctionDAO auctionDAO = new com.bidding.dao.JdbcAuctionDAO();
            int auctionId = auctionDAO.createAuction(itemId, item.getFirstprice().doubleValue(), startTimeMillis, endTimeMillis);

            AuctionOperator.AuctionResult result = auctionOperator.scheduleAuction(item, seller, roomId, startTimeMillis, endTimeMillis, auctionId);
            if (result.isAccepted()) {
                itemManager.markItemInAuction(itemId);
            }
            return result.getMessage();
        } catch (Exception e) {
            return "Lỗi: " + e.getMessage();
        }
    }

    /**
     * Tạo và lên lịch đấu giá, trả về `auctionId` (generated) nếu thành công, -1 nếu lỗi.
     */
    public int scheduleAuctionAndReturnId(int itemId, Users seller, String roomId, String password, long startTimeMillis, long endTimeMillis) {
        try {
            Item item = itemManager.getItemById(itemId);
            if (item == null) {
                return -1;
            }
            if (seller == null || item.getUserId() != seller.getId()) {
                return -1;
            }
            if (!Item.STATUS_APPROVED.equalsIgnoreCase(item.getStatus())) {
                return -1;
            }
            if (currentAdmin == null || !"Admin".equalsIgnoreCase(currentAdmin.getRole())) {
                return -1;
            }
            if (roomId == null || roomId.isEmpty()) {
                return -1;
            }

            com.bidding.dao.JdbcAuctionDAO auctionDAO = new com.bidding.dao.JdbcAuctionDAO();
            int auctionId = auctionDAO.createAuction(itemId, item.getFirstprice().doubleValue(), startTimeMillis, endTimeMillis);
            if (auctionId <= 0) return -1;

            AuctionOperator.AuctionResult result = auctionOperator.scheduleAuction(item, seller, roomId, startTimeMillis, endTimeMillis, auctionId);
            if (result.isAccepted()) {
                itemManager.markItemInAuction(itemId);
                return auctionId;
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    public String registerAutoBid(String roomId, Users bidder, double maxBid, double increment) {
        try {
            AuctionOperator.AuctionResult result = auctionOperator.registerAutoBid(roomId, bidder, maxBid, increment);
            return result.getMessage();
        } catch (Exception e) {
            return "Lỗi: " + e.getMessage();
        }
    }

    public String placeBid(String roomId, Users bidder, double bidAmount) {
        try {
            if (roomId == null || roomId.isEmpty()) {
                return "Lỗi: Room ID không hợp lệ.";
            }
            if (bidder == null) {
                return "Lỗi: Bidder không hợp lệ.";
            }
            if (bidAmount <= 0) {
                return "Lỗi: Số tiền phải lớn hơn 0.";
            }
            AuctionOperator.AuctionResult result = auctionOperator.placeBid(roomId, bidder, bidAmount);
            return result.getMessage();
        } catch (Exception e) {
            return "Lỗi: " + e.getMessage();
        }
    }

    public AuctionOperator.AuctionSession getAuctionSession(String roomId) {
        return auctionOperator.getSession(roomId);
    }

    public String acceptAuctionInvitation(Users user, String roomId, String password) {
        try {
            AuctionOperator.AuctionSession session = auctionOperator.getSession(roomId);
            if (session == null) {
                return "Lỗi: Không tìm thấy phiên đấu giá.";
            }
            if (user == null) {
                return "Lỗi: Người dùng không hợp lệ.";
            }
            session.registerObserver(user);
            return "Bạn đã tham gia vào phiên đấu giá và sẽ nhận thông báo về các bid mới và kết quả phiên đấu giá.";
        } catch (Exception e) {
            return "Lỗi: " + e.getMessage();
        }
    }

    // ==================== WALLET OPERATIONS ====================

    public void depositMoney(int userId, double amount) {
        walletManager.depositDirectly(userId, amount);
    }

    public double getAvailableBalance(int userId) {
        var wallet = walletManager.getWalletByUserId(userId);
        return wallet != null ? wallet.getAmount().doubleValue() : 0.0;
    }

    public double getLockedBalance(int userId) {
        var wallet = walletManager.getWalletByUserId(userId);
        return wallet != null ? wallet.getLockedAmount().doubleValue() : 0.0;
    }

    public boolean transferMoney(int fromUserId, int toUserId, double amount) {
        return walletManager.transferMoney(fromUserId, toUserId, amount);
    }

    // ==================== ITEM OPERATIONS ====================

    public int registerNewItem(int userId, String itemName, String description, double price) {
        try {
            return itemManager.registerNewItem(userId, itemName, description, price);
        } catch (Exception e) {
            return -1;
        }
    }

    public void updateItem(int userId, int itemId, String itemName, String description) {
        itemManager.updateItem(userId, itemId, itemName, description);
    }

    public Item getSellerItem(int userId) {
        return itemManager.getItemByUserId(userId);
    }

    public List<Item> getSellerItems(int userId) {
        return itemManager.getItemsByUserId(userId);
    }

    public Item getItemById(int itemId) {
        return itemManager.getItemById(itemId);
    }

    public List<Item> getApprovedItems() {
        return itemManager.getItemsByStatus(Item.STATUS_APPROVED);
    }

    public void reviewItem(Users admin, int itemId, boolean approve) {
        itemManager.reviewItem(admin, itemId, approve);
    }

    // ==================== UTILITY ====================

    public void shutdown() {
        if (auctionOperator != null) {
            auctionOperator.shutdown();
        }
    }

    public void setCurrentAdmin(Users admin) {
        this.currentAdmin = admin;
        if (admin != null) {
            this.auctionOperator.setAdminUserId(admin.getId());
        }
    }

    /**
     * Kiểm tra xem có phòng đấu giá nào đang hoạt động không
     */
    public boolean hasActiveAuction(String roomId) {
        AuctionOperator.AuctionSession session = auctionOperator.getSession(roomId);
        return session != null && session.isStarted() && !session.isEnded();
    }
}
