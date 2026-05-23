package com.bidding.shared;

public class DepositRequest {
    // 1. Lớp chứa thông tin phiếu nạp (Data Object)
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
