package com.bidding.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * WalletManager — Quản lý tất cả ví trong hệ thống.
 *
 * Thay thế file WalletManager.java gốc bằng file này.
 * Thêm: getPendingRequests() để WalletAdminController đọc danh sách chờ duyệt.
 */
public class WalletManager {

    // Kho chứa tất cả ví của hệ thống (userId → Balance)
    private final HashMap<String, Balance> allWallets = new HashMap<>();

    // Danh sách phiếu nạp tiền chờ Admin duyệt
    private List<DepositRequest> pendingRequests = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Đăng ký ví mới khi có User mới
    // ─────────────────────────────────────────────────────────────────────────
    public void registerNewWallet(Users user) {
        if (!allWallets.containsKey(user.getId())) {
            allWallets.put(user.getId(), new Balance(user, 0.0));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Tìm ví theo ID
    // ─────────────────────────────────────────────────────────────────────────
    public Balance getWalletByUserId(String userId) {
        return allWallets.get(userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Giao tiếp với DepositMethod (Strategy Pattern)
    // ─────────────────────────────────────────────────────────────────────────

    /** Gọi phương thức nạp tiền được chọn (CardMethod hoặc AdminMethod) */
    public void execute(String userId, double amount, DepositMethod method) {
        method.processDeposit(userId, amount, this);
    }

    /** Dành cho AdminMethod: nhận phiếu nạp vào danh sách chờ */
    public void addPendingRequest(DepositRequest req) {
        if (this.pendingRequests == null) {
            this.pendingRequests = new ArrayList<>();
        }
        this.pendingRequests.add(req);
    }

    /**
     * [MỚI] Dành cho WalletAdminController: lấy danh sách phiếu nạp.
     * Trả về unmodifiable list để tránh chỉnh sửa ngoài ý muốn.
     * Admin thay đổi trạng thái qua req.setStatus() trực tiếp trên object.
     */
    public List<DepositRequest> getPendingRequests() {
        if (pendingRequests == null) return Collections.emptyList();
        return Collections.unmodifiableList(pendingRequests);
    }

    /** Dành cho CardMethod hoặc khi Admin bấm duyệt: cộng tiền thẳng vào ví */
    public void depositDirectly(String userId, double amount) {
        Balance wallet = allWallets.get(userId);
        if (wallet != null) {
            wallet.deposit(amount);
        } else {
            System.out.println("[WalletManager] Lỗi: Không tìm thấy ví của " + userId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Chuyển tiền giữa 2 ví (dùng trong thanh toán đấu giá)
    // ─────────────────────────────────────────────────────────────────────────
    public boolean transferMoney(String fromUserId, String toUserId, double amount) {
        Balance fromWallet = getWalletByUserId(fromUserId);
        Balance toWallet   = getWalletByUserId(toUserId);

        if (fromWallet == null || toWallet == null) {
            System.out.println("[WalletManager] Lỗi: Ví không tồn tại.");
            return false;
        }

        if (fromWallet.withdraw(amount)) {
            try {
                toWallet.deposit(amount);
                return true;
            } catch (Exception e) {
                System.out.println("[WalletManager] Lỗi khi chuyển: " + e.getMessage());
                fromWallet.deposit(amount); // Hoàn tác
                System.out.println("[WalletManager] Đã hoàn tác giao dịch.");
                return false;
            }
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Tiện ích
    // ─────────────────────────────────────────────────────────────────────────

    /** Đếm số phiếu PENDING — dùng cho badge trên Sidebar của Admin */
    public int countPendingRequests() {
        if (pendingRequests == null) return 0;
        return (int) pendingRequests.stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .count();
    }
}
