package com.bidding.shared;

import java.math.BigDecimal;

public class Balance {
    private final Users user;
    private BigDecimal currentBalance;
    private BigDecimal lockedBalance;

    public Balance(Users user, BigDecimal initialBalance) {
        if (user == null) {
            throw new IllegalArgumentException("Người dùng không hợp lệ.");
        }
        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số dư ban đầu phải là số không âm.");
        }
        this.user = user;
        this.currentBalance = initialBalance;
        this.lockedBalance = BigDecimal.ZERO;
    }

    public synchronized void deposit(BigDecimal amount) {
        validatePositiveAmount(amount, "Số tiền nạp phải lớn hơn 0.");
        this.currentBalance = this.currentBalance.add(amount);
        System.out.println("Nạp thành công: " + amount);
    }

    public synchronized boolean withdraw(BigDecimal amount) {
        validatePositiveAmount(amount, "Số tiền rút phải lớn hơn 0.");
        if (currentBalance.compareTo(amount) >= 0) {
            currentBalance = currentBalance.subtract(amount);
            System.out.println("Rút thành công: " + amount);
            return true;
        }
        return false;
    }

    public synchronized BigDecimal getAmount() {
        return currentBalance;
    }

    public synchronized BigDecimal getLockedAmount() {
        return lockedBalance;
    }

    public synchronized void lockAmount(BigDecimal amount) {
        validatePositiveAmount(amount, "Số tiền khóa phải lớn hơn 0.");
        if (currentBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Số dư không đủ để khóa số tiền yêu cầu.");
        }
        currentBalance = currentBalance.subtract(amount);
        lockedBalance = lockedBalance.add(amount);
        System.out.println("Đã khóa số tiền: " + amount);
    }

    public synchronized void unlockAmount(BigDecimal amount) {
        validatePositiveAmount(amount, "Số tiền mở khóa phải lớn hơn 0.");
        if (lockedBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Số tiền khóa không đủ để mở khóa.");
        }
        lockedBalance = lockedBalance.subtract(amount);
        currentBalance = currentBalance.add(amount);
        System.out.println("Đã mở khóa số tiền: " + amount);
    }

    public synchronized BigDecimal commitLockedAmount(BigDecimal amount) {
        validatePositiveAmount(amount, "Số tiền cam kết phải lớn hơn 0.");
        if (lockedBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Số tiền khóa không đủ để cam kết.");
        }
        lockedBalance = lockedBalance.subtract(amount);
        System.out.println("Đã thanh toán số tiền: " + amount);
        return amount;
    }

    private void validatePositiveAmount(BigDecimal amount, String errorMessage) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public Users getUser() {
        return user;
    }

    public String getUserId() {
        return user.getId();
    }
}

