package com.bidding.dao;

import java.util.List;

public interface WalletDepositRequestDAO {
    /**
     * Thêm một yêu cầu nạp tiền
     */
    boolean insertDepositRequest(int userId, double amount, String status, String createdAt);

    /**
     * Lấy danh sách yêu cầu nạp tiền theo trạng thái
     */
    List<DepositRequest> getDepositRequestsByStatus(String status);

    /**
     * Lấy danh sách yêu cầu nạp tiền của một người dùng
     */
    List<DepositRequest> getDepositRequestsByUserId(int userId);

    /**
     * Lấy yêu cầu nạp tiền theo ID
     */
    DepositRequest getDepositRequestById(int requestId);

    /**
     * Cập nhật trạng thái yêu cầu nạp tiền
     */
    boolean updateDepositRequestStatus(int requestId, String status);

    /**
     * Lấy tất cả yêu cầu nạp tiền chưa xử lý (PENDING)
     */
    List<DepositRequest> getPendingRequests();

    /**
     * Đếm số yêu cầu chờ duyệt
     */
    int countPendingRequests();

    public static class DepositRequest {
        public int id;
        public int userId;
        public double amount;
        public String status; // PENDING, APPROVED, REJECTED
        public String createdAt;
        public String userName;

        public DepositRequest(int id, int userId, double amount, String status, String createdAt, String userName) {
            this.id = id;
            this.userId = userId;
            this.amount = amount;
            this.status = status;
            this.createdAt = createdAt;
            this.userName = userName;
        }

        // Getters
        public int getId() { return id; }
        public int getUserId() { return userId; }
        public double getAmount() { return amount; }
        public String getStatus() { return status; }
        public String getCreatedAt() { return createdAt; }
        public String getUserName() { return userName; }
    }
}

