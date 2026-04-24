package com.bidding.shared;

import java.util.HashMap; //Thư viện (key,value))

public class WalletManager {
    // Kho chứa tất cả ví của hệ thống  
    private HashMap<String, Balance> allWallets = new HashMap<>();

    // 1. Hành động: Đăng ký ví mới khi có User mới
    public void registerNewWallet(String userId) {
        if (!allWallets.containsKey(userId)) {
            allWallets.put(userId, new Balance(userId, 0.0));
        }
    }

    // 2. Hành động: "Link" - Tìm ví dựa trên ID
    public Balance getWalletByUserId(String userId) {
        return allWallets.get(userId);
    }

    // 3. Hành động: Chuyển tiền (Nghiệp vụ đặc thụ của Manager)
    public boolean transferMoney(String fromUserId, String toUserId, double amount) {
        Balance fromWallet = getWalletByUserId(fromUserId);
        Balance toWallet = getWalletByUserId(toUserId);

        if (fromWallet != null && toWallet != null) {
            if (fromWallet.withdraw(amount)) { // Rút từ người gửi
                toWallet.deposit(amount);      // Nạp cho người nhận
                return true;
            }
        }
        return false;
    }
}
