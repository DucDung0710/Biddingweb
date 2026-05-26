package com.bidding.shared;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WalletManager {
    // Kho chứa tất cả ví của hệ thống
    private final Map<String, Balance> allWallets = new ConcurrentHashMap<>();
    private final List<DepositRequest> pendingRequests = Collections.synchronizedList(new ArrayList<>());

    // Đăng ký ví mới
    public void registerNewWallet(Users user) {
        if (user == null) {
            return;
        }
        allWallets.computeIfAbsent(user.getId(), id -> new Balance(user, BigDecimal.ZERO));
    }

    // Lấy ví theo userId
    public Balance getWalletByUserId(String userId) {
        return allWallets.get(userId);
    }

    // Thêm yêu cầu nạp vào danh sách chờ
    void addPendingRequest(DepositRequest req) {
        if (req == null) {
            return;
        }
        this.pendingRequests.add(req);
    }

    // Thực thi phương thức nạp
    public void execute(String userId, BigDecimal amount, DepositMethod method) {
        if (method == null) {
            throw new IllegalArgumentException("Phương thức nạp tiền không hợp lệ.");
        }
        method.processDeposit(userId, amount, this);
    }

    public void execute(String userId, double amount, DepositMethod method) {
        execute(userId, BigDecimal.valueOf(amount), method);
    }

    // Nạp trực tiếp (Card/Admin approved)
    public void depositDirectly(String userId, BigDecimal amount) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("UserId không hợp lệ.");
        }
        Balance wallet = allWallets.get(userId);
        if (wallet != null) {
            wallet.deposit(amount);
        } else {
            throw new IllegalArgumentException("Không tìm thấy ví của người dùng " + userId);
        }
    }

    public void depositDirectly(String userId, double amount) {
        depositDirectly(userId, BigDecimal.valueOf(amount));
    }

    // Chuyển tiền giữa hai ví
    public boolean transferMoney(String fromUserId, String toUserId, BigDecimal amount) {
        if (fromUserId == null || toUserId == null || amount == null) {
            return false;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        Balance fromWallet = getWalletByUserId(fromUserId);
        Balance toWallet = getWalletByUserId(toUserId);

        if (fromWallet == null || toWallet == null) {
            return false;
        }

        try {
            if (fromWallet.withdraw(amount)) {
                toWallet.deposit(amount);
                return true;
            }
        } catch (IllegalArgumentException ex) {
            System.out.println("Lỗi khi chuyển tiền: " + ex.getMessage());
        }
        return false;
    }

    public boolean transferMoney(String fromUserId, String toUserId, double amount) {
        return transferMoney(fromUserId, toUserId, BigDecimal.valueOf(amount));
    }

}
