package com.bidding.shared;

public class Balance {
    private Users user;
    private double currentBalance;
    private double lockedBalance; // Số tiền đang bị khóa (ví dụ: khi đặt cọc)

    public Balance(Users user, double initialBalance) {
        this.user = user;
        this.currentBalance = initialBalance;
        this.lockedBalance = 0;
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

    public double getLockedAmount() {
        return lockedBalance;
    }

   
    public void lockAmount(double amount) {
        if (amount > 0 && this.currentBalance >= amount) {
            this.currentBalance -= amount;
            this.lockedBalance += amount;
            System.out.println("Đã khóa số tiền: " + amount);
        } else {
            System.out.println("Số dư không đủ hoặc số tiền không hợp lệ để khóa!");
        }
    }

    public void unlockAmount(double amount) {
        if (amount > 0 && this.lockedBalance >= amount ) {
            this.lockedBalance -= amount;
            this.currentBalance += amount;
            System.out.println("Đã mở khóa số tiền: " + amount);
        } else {
            System.out.println("Số tiền khóa không đủ hoặc số tiền không hợp lệ để mở khóa!");
        }
    }

    public double commitLockedAmount(double amount) {
        if (amount > 0 && this.lockedBalance >= amount ) {
            this.lockedBalance -= amount;
            System.out.println("Đã thanh toán số tiền: " + amount);
            return amount; // Trả về số tiền đã cam kết để xử lý thanh toán
        } else {
            System.out.println("Số tiền khóa không đủ hoặc số tiền không hợp lệ để cam kết!");
        }
        return 0; // Trả về 0 nếu không thể cam kết
    }

    public Users getUser() {
        return user;
    }
    public String getUserId() {
        return user.getId();
    }


}

