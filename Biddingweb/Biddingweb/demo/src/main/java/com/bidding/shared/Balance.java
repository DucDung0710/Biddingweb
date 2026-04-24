package com.bidding.shared;

public class Balance {
    private String userId;
    private double currentBalance;

    public Balance(String userId, double initialBalance) {
        this.userId = userId;
        this.currentBalance = initialBalance;
    }

    // Phương thức nạp tiền
    public void deposit(double amount) {
        if (amount > 0) {
            this.currentBalance += amount;
            System.out.println("Nạp thành công: " + amount);
        } else {
            System.out.println("Số tiền nạp không hợp lệ!");
        }
    }

    // Phương thức rút tiền
    public boolean withdraw(double amount) {
        if (amount > 0 && this.currentBalance >= amount) {
            this.currentBalance -= amount;
            System.out.println("Rút thành công: " + amount);
            return true; // Rút thành công
        } else {
            System.out.println("Số dư không đủ hoặc số tiền không hợp lệ!");
            return false; // Rút thất bại
        }
    }

    public double getAmount() {
        return currentBalance;
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
}
