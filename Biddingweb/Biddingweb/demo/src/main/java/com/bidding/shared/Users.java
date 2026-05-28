package com.bidding.shared;

/**
 * Lớp trừu tượng Users đại diện cho một người dùng trong hệ thống đấu giá.
 * 
 * Đặc điểm:
 * - Người dùng có vai trò cụ thể: Admin, Seller, hoặc Bidder
 * - Mỗi người dùng có tài khoản (username, password, email)
 * - Mỗi người dùng có ID duy nhất
 * - Người dùng là Observer (nhận thông báo từ AuctionRoom)
 * 
 * Các lớp con:
 * - Admin: Người quản lý hệ thống
 * - Seller: Người đưa item lên bán đấu giá
 * - Bidder: Người tham gia đấu giá
 */
public abstract class Users implements AuctionObserver {
    // Tên đăng nhập của người dùng
    private String username;
    // Mật khẩu của người dùng
    private String password;
    // ID duy nhất của người dùng
    private String id;
    // Email của người dùng
    private String email;
    // Vai trò của người dùng (Admin, Seller, Bidder)
    protected String role;

    public Users(String username, String password, String id, String email) {
        this.username = username;
        this.password = password;
        this.id = id;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

     public String getId(){
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Nhận thông báo từ AuctionRoom (Observer pattern)
     * Thường in ra thông báo hoặc lưu vào lịch sử
     * 
     * @param message Nội dung thông báo
     */
    @Override
    public void update(String message) {
        // Có thể extend để lưu vào lịch sử thông báo của user
        System.out.println("Thông báo cho " + username + ": " + message);
    }

    /**
     * Lấy ID để sử dụng trong Observer pattern
     * @return User ID
     */
    @Override
    public String getUserId() {
        return id;
    }

}

/**
 * Lớp Bidder đại diện cho một người tham gia đấu giá.
 * Người dùng này có thể tham gia các phiên đấu giá và đặt giá.
 */
class Bidder extends Users {
    /**
     * Khởi tạo Bidder
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @param id ID duy nhất
     * @param email Email
     */
    public Bidder(String username, String password, String id, String email) {
        super(username, password, id, email);
        this.role = "Bidder";
    }
}

/**
 * Lớp Seller đại diện cho một người bán hàng.
 * Người dùng này có thể đăng ký item để bán thông qua đấu giá.
 */
class Seller extends Users {
    /**
     * Khởi tạo Seller
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @param id ID duy nhất
     * @param email Email
     */
    public Seller(String username, String password, String id, String email) {
        super(username, password, id, email);
        this.role = "Seller";
    }
}

/**
 * Lớp Admin đại diện cho người quản trị hệ thống.
 * Admin có quyền duyệt các item và quản lý các phiên đấu giá.
 */
class Admin extends Users {
    /**
     * Khởi tạo Admin
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @param id ID duy nhất
     * @param email Email
     */
    public Admin(String username, String password, String id, String email) {
        super(username, password, id, email);
        this.role = "Admin";
    }

}