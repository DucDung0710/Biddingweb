package com.bidding.shared;
import java.util.HashMap; 
import java.util.Map;

// 1. Lớp chứa thông tin phiếu nạp (Data Object)
class DepositRequest {
    private static int idCounter = 1; // Logic tự tăng ID
    private int requestId;
    private String userId;
    private double amount;
    private String status; // "PENDING", "APPROVED", "REJECTED"

    public DepositRequest(String userId, double amount) {
        this.requestId = idCounter++;
        this.userId = userId;
        this.amount = amount;
        this.status = "PENDING";
    }

    // Getters và Setters để Manager sử dụng
    public int getRequestId() { return requestId; }
    public String getUserId() { return userId; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

// 2. Lớp cha định nghĩa khung nạp tiền
public abstract class DepositMethod {
    protected String methodName;

    public DepositMethod(String methodName) {
        this.methodName = methodName;
    }

    // Phương thức này sẽ được gọi trong WalletManager
    public abstract void processDeposit(String userId, double amount, WalletManager manager);
}

// 3. Lớp con: Nạp qua Admin (Tạo phiếu chờ)
class AdminMethod extends DepositMethod {
    public AdminMethod() {
        super("Admin Approval");
    }

    @Override
    public void processDeposit(String userId, double amount, WalletManager manager) {
        // Tạo phiếu nạp mới với requestId tự tăng
        DepositRequest req = new DepositRequest(userId, amount);
        
        // Gửi phiếu này vào danh sách quản lý của WalletManager
        manager.addPendingRequest(req);
        
        System.out.println("[Hệ thống] Đã tạo yêu cầu nạp " + amount + " cho User: " + userId);
        System.out.println("[Hệ thống] Vui lòng chờ Admin duyệt (Mã yêu cầu: " + req.getRequestId() + ")");
    }
}

// 4. Lớp con: Nạp bằng thẻ cào (Cộng tiền ngay)
class CardMethod extends DepositMethod {

    private Map<String, Double> cardLibrary;

    public CardMethod() {
        super("Card Payment");
        cardLibrary = new HashMap<>();
        
        // Khởi tạo một số mã thẻ mẫu
        cardLibrary.put("NAP20000", 20000.0);
        cardLibrary.put("NAP50000", 50000.0);
        cardLibrary.put("NAP100000", 100000.0);
        cardLibrary.put("NAP500000", 500000.0);
    }

    @Override
    public void processDeposit(String userId, double amount, WalletManager manager) {
        System.out.println("[THẺ CÀO] Đang quết mã thẻ ...");
    }

    // Hàm nạp thẻ chính 
    public void topUpWithCode(String userId, String inputCode, WalletManager manager) {
        // 1. Kiểm tra mã thẻ có tồn tại trong thư viện không
        if (cardLibrary.containsKey(inputCode)) {
            
            // 2. Lấy mệnh giá tương ứng với mã đó
            double cardValue = cardLibrary.get(inputCode);
            
            // 3. Gọi Manager để cộng tiền thẳng vào ví
            manager.depositDirectly(userId, cardValue);
            
            // 4. Xóa mã thẻ này đi để không cho nạp lần thứ 2 (Tránh bug hack tiền)
            cardLibrary.remove(inputCode);
            
            System.out.println("[THẺ CÀO] Nạp thành công mã " + inputCode + ". Ví của " + userId + " +" + cardValue);
        } else {
            System.out.println("[THẺ CÀO] Lỗi: Mã thẻ '" + inputCode + "' không hợp lệ hoặc đã sử dụng!");
        }
    }
}