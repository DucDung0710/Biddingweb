package com.bidding.shared;

import java.util.ArrayList;
import java.util.HashMap; //Thư viện (key,value))
import java.util.List;

public class WalletManager {
    // Kho chứa tất cả ví của hệ thống  
    private HashMap<String, Balance> allWallets = new HashMap<>();

    private List<DepositRequest> pendingRequests = new ArrayList<>();


    // 1. Hành động: Đăng ký ví mới khi có User mới
    public void registerNewWallet(Users user) {
        if (!allWallets.containsKey(user.getId())) {
            allWallets.put(user.getId(), new Balance(user, 0.0));
        }
    }

    // 2. Hành động: "Link" - Tìm ví dựa trên ID
    public Balance getWalletByUserId(String userId) {
        return allWallets.get(userId);
    }

    // 1. Hàm dành cho AdminMethod: Nhận phiếu nạp và cất vào danh sách chờ
public void addPendingRequest(DepositRequest req) {
    if (this.pendingRequests == null) {
        this.pendingRequests = new ArrayList<>();
    }
    this.pendingRequests.add(req);
}

public void execute(String userId, double amount, DepositMethod method) {
    // Khi dòng này chạy, một trong hai lớp con sẽ được kích hoạt
    method.processDeposit(userId, amount, this); 
}


// 2. Hàm dành cho CardMethod (hoặc khi Admin bấm duyệt): Bơm tiền trực tiếp vào ví
public void depositDirectly(String userId, double amount) {
    Balance wallet = allWallets.get(userId); 
    
    if (wallet != null) {
        wallet.deposit(amount); // Gọi hàm deposit gốc trong file Balance của bạn
    } else {
        System.out.println("Lỗi: Không tìm thấy ví của người dùng " + userId);
    }
}

    // 3. Hành động: Chuyển tiền 
    public boolean transferMoney(String fromUserId, String toUserId, double amount) {
        Balance fromWallet = getWalletByUserId(fromUserId);
        Balance toWallet = getWalletByUserId(toUserId);

        if (fromWallet != null && toWallet != null) {
            
            if (fromWallet.withdraw(amount)) { // Rút từ người gửi
                try { 
                    toWallet.deposit(amount);      // Nạp cho người nhận
                    return true;
                } catch (Exception e) { // Nếu có lỗi khi nạp tiền cho người nhận, hoàn tác giao dịch
                    System.out.println("Lỗi khi chuyển tiền: " + e.getMessage());
                    // Hoàn tác giao dịch nếu có lỗi
                    System.out.println("Hoàn tác giao dịch: Đang hoàn trả tiền về ví người gửi...");
                    fromWallet.deposit(amount);
                    System.out.println("Giao dịch đã được hoàn tác.");
                    return false;
                }
            }
        }
        return false;
    }


}
