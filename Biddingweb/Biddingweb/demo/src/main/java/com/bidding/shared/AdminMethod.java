package com.bidding.shared;

   // 3. Lớp con: Nạp qua Admin (Tạo phiếu chờ)
    public class AdminMethod extends DepositMethod {
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
