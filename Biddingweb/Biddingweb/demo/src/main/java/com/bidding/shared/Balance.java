package com.bidding.shared;

import java.math.BigDecimal;

/**
 * Lớp Balance quản lý số dư tài khoản và tiền khóa của một user.
 * 
 * Khái niệm:
 * - currentBalance: Số tiền khả dụng (có thể rút hoặc dùng)
 * - lockedBalance: Số tiền bị khóa do đặt giá, không được phép rút
 * 
 * Các hoạt động:
 * 1. deposit(amount): Nạp tiền vào tài khoản
 * 2. withdraw(amount): Rút tiền ra (yêu cầu đủ số dư)
 * 3. lockAmount(amount): Khóa một phần tiền khi đặt giá
 * 4. unlockAmount(amount): Mở khóa tiền khi không đặt giá nữa
 * 5. commitLockedAmount(amount): Xác nhận rút tiền khóa (thanh toán)
 */
public class Balance {
    // Người dùng sở hữu tài khoản này
    private final Users user;
    // Số tiền hiện có và khả dụng
    private BigDecimal currentBalance;
    // Số tiền bị khóa (không được rút)
    private BigDecimal lockedBalance;

    /**
     * Khởi tạo Balance cho một user
     * 
     * @param user Người dùng sở hữu tài khoản
     * @param initialBalance Số dư ban đầu (phải >= 0)
     * @throws IllegalArgumentException nếu user null hoặc initialBalance < 0
     */
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

    /**
     * Nạp tiền vào tài khoản.
     * Thêm vào currentBalance (số tiền khả dụng).
     * 
     * @param amount Số tiền nạp (phải > 0)
     * @throws IllegalArgumentException nếu amount <= 0
     */
    public synchronized void deposit(BigDecimal amount) {
        validatePositiveAmount(amount, "Số tiền nạp phải lớn hơn 0.");
        this.currentBalance = this.currentBalance.add(amount);
        System.out.println("Nạp thành công: " + amount);
    }

    /**
     * Rút tiền ra khỏi tài khoản.
     * Chỉ có thể rút nếu currentBalance đủ.
     * 
     * @param amount Số tiền rút (phải > 0)
     * @return true nếu rút thành công, false nếu số dư không đủ
     * @throws IllegalArgumentException nếu amount <= 0
     */
    public synchronized boolean withdraw(BigDecimal amount) {
        validatePositiveAmount(amount, "Số tiền rút phải lớn hơn 0.");
        if (currentBalance.compareTo(amount) >= 0) {
            currentBalance = currentBalance.subtract(amount);
            System.out.println("Rút thành công: " + amount);
            return true;
        }
        return false;
    }

    /**
     * Lấy số tiền hiện có (khả dụng)
     * @return Số dư hiện tại
     */
    public synchronized BigDecimal getAmount() {
        return currentBalance;
    }

    /**
     * Lấy số tiền bị khóa
     * @return Số tiền khóa
     */
    public synchronized BigDecimal getLockedAmount() {
        return lockedBalance;
    }

    /**
     * Khóa một phần tiền khi người dùng đặt giá.
     * Chuyển tiền từ currentBalance -> lockedBalance.
     * 
     * @param amount Số tiền cần khóa (phải > 0)
     * @throws IllegalArgumentException nếu amount <= 0 hoặc currentBalance < amount
     */
    public synchronized void lockAmount(BigDecimal amount) {
        validatePositiveAmount(amount, "Số tiền khóa phải lớn hơn 0.");
        if (currentBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Số dư không đủ để khóa số tiền yêu cầu.");
        }
        currentBalance = currentBalance.subtract(amount);
        lockedBalance = lockedBalance.add(amount);
        System.out.println("Đã khóa số tiền: " + amount);
    }

    /**
     * Mở khóa (hoàn lại) tiền khi người dùng không đặt giá nữa.
     * Chuyển tiền từ lockedBalance -> currentBalance.
     * 
     * @param amount Số tiền cần mở khóa (phải > 0)
     * @throws IllegalArgumentException nếu amount <= 0 hoặc lockedBalance < amount
     */
    public synchronized void unlockAmount(BigDecimal amount) {
        validatePositiveAmount(amount, "Số tiền mở khóa phải lớn hơn 0.");
        if (lockedBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Số tiền khóa không đủ để mở khóa.");
        }
        lockedBalance = lockedBalance.subtract(amount);
        currentBalance = currentBalance.add(amount);
        System.out.println("Đã mở khóa số tiền: " + amount);
    }

    /**
     * Xác nhận rút tiền khóa (thanh toán).
     * Giảm lockedBalance (tiền khóa được sử dụng để thanh toán).
     * 
     * @param amount Số tiền cam kết rút (phải > 0)
     * @return Số tiền đã được cam kết rút
     * @throws IllegalArgumentException nếu amount <= 0 hoặc lockedBalance < amount
     */
    public synchronized BigDecimal commitLockedAmount(BigDecimal amount) {
        validatePositiveAmount(amount, "Số tiền cam kết phải lớn hơn 0.");
        if (lockedBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Số tiền khóa không đủ để cam kết.");
        }
        lockedBalance = lockedBalance.subtract(amount);
        System.out.println("Đã thanh toán số tiền: " + amount);
        return amount;
    }

    /**
     * Kiểm tra số tiền có hợp lệ (phải dương)
     * @param amount Số tiền cần kiểm tra
     * @param errorMessage Thông báo lỗi nếu không hợp lệ
     * @throws IllegalArgumentException nếu amount <= 0
     */
    private void validatePositiveAmount(BigDecimal amount, String errorMessage) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Lấy người dùng sở hữu tài khoản
     * @return Đối tượng Users
     */
    public Users getUser() {
        return user;
    }

    /**
     * Lấy ID người dùng sở hữu tài khoản
     * @return User ID
     */
    public String getUserId() {
        return user.getId();
    }
}

