package com.bidding.dao;

import java.util.List;

public interface WalletTransactionDAO {
    /**
     * Thêm một giao dịch ví vào database
     */
    boolean insertTransaction(int userId, String type, double amount, double balanceAfter, Integer refId, String note, String createdAt);

    /**
     * Lấy danh sách giao dịch của một người dùng
     */
    List<WalletTransaction> getTransactionsByUserId(int userId);

    /**
     * Lấy danh sách giao dịch theo loại (HOLD, RELEASE, RECEIVE, WITHDRAW, etc.)
     */
    List<WalletTransaction> getTransactionsByType(int userId, String type);

    /**
     * Lấy tổng số giao dịch của người dùng
     */
    int getTransactionCount(int userId);

    /**
     * Lấy lịch sử giao dịch với giới hạn số bản ghi
     */
    List<WalletTransaction> getTransactionHistory(int userId, int limit);

    public static class WalletTransaction {
        public int id;
        public int userId;
        public String type;
        public double amount;
        public double balanceAfter;
        public Integer refId;
        public String note;
        public String createdAt;

        public WalletTransaction(int id, int userId, String type, double amount, double balanceAfter, Integer refId, String note, String createdAt) {
            this.id = id;
            this.userId = userId;
            this.type = type;
            this.amount = amount;
            this.balanceAfter = balanceAfter;
            this.refId = refId;
            this.note = note;
            this.createdAt = createdAt;
        }

        // Getters
        public int getId() { return id; }
        public int getUserId() { return userId; }
        public String getType() { return type; }
        public double getAmount() { return amount; }
        public double getBalanceAfter() { return balanceAfter; }
        public Integer getRefId() { return refId; }
        public String getNote() { return note; }
        public String getCreatedAt() { return createdAt; }
    }
}

