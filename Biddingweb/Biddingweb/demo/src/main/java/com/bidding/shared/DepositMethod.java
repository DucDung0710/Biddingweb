package com.bidding.shared;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

// 1. Lớp chứa thông tin phiếu nạp (Data Object)
class DepositRequest {
    private static int idCounter = 1; // Logic tự tăng ID
    private int requestId;
    private String userId;
    private BigDecimal amount;
    private String status; // "PENDING", "APPROVED", "REJECTED"

    public DepositRequest(String userId, BigDecimal amount) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("UserId không hợp lệ.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0.");
        }
        this.requestId = idCounter++;
        this.userId = userId;
        this.amount = amount;
        this.status = "PENDING";
    }

    // Getters và Setters để Manager sử dụng
    public int getRequestId() { return requestId; }
    public String getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
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
    public abstract void processDeposit(String userId, BigDecimal amount, WalletManager manager);

    public void processDeposit(String userId, double amount, WalletManager manager) {
        processDeposit(userId, BigDecimal.valueOf(amount), manager);
    }
}

// 3. Lớp con: Nạp qua Admin (Tạo phiếu chờ)
class AdminMethod extends DepositMethod {
    public AdminMethod() {
        super("Admin Approval");
    }

    @Override
    public void processDeposit(String userId, BigDecimal amount, WalletManager manager) {
        DepositRequest req = new DepositRequest(userId, amount);
        manager.addPendingRequest(req);
        System.out.println("[Hệ thống] Đã tạo yêu cầu nạp " + amount + " cho User: " + userId);
        System.out.println("[Hệ thống] Vui lòng chờ Admin duyệt (Mã yêu cầu: " + req.getRequestId() + ")");
    }
}

// 4. Lớp con: Nạp bằng thẻ cào (Cộng tiền ngay)
class CardMethod extends DepositMethod {

    private Map<String, BigDecimal> cardLibrary;

    public CardMethod() {
        super("Card Payment");
        cardLibrary = new HashMap<>();
        cardLibrary.put("NAP20000", BigDecimal.valueOf(20000));
        cardLibrary.put("NAP50000", BigDecimal.valueOf(50000));
        cardLibrary.put("NAP100000", BigDecimal.valueOf(100000));
        cardLibrary.put("NAP500000", BigDecimal.valueOf(500000));
    }

    @Override
    public void processDeposit(String userId, BigDecimal amount, WalletManager manager) {
        System.out.println("[THẺ CÀO] Đang quét mã thẻ ...");
    }

    // Hàm nạp thẻ chính
    public void topUpWithCode(String userId, String inputCode, WalletManager manager) {
        if (cardLibrary.containsKey(inputCode)) {
            BigDecimal cardValue = cardLibrary.get(inputCode);
            manager.depositDirectly(userId, cardValue);
            cardLibrary.remove(inputCode);
            System.out.println("[THẺ CÀO] Nạp thành công mã " + inputCode + ". Ví của " + userId + " +" + cardValue);
        } else {
            System.out.println("[THẺ CÀO] Lỗi: Mã thẻ '" + inputCode + "' không hợp lệ hoặc đã sử dụng!");
        }
    }
}
