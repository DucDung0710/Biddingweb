package com.bidding.shared;

public class UserSession {
    // 1. Thực thể Singleton duy nhất
    private static UserSession instance;
    private Users loggedInUser = null;

    // 2. Private constructor để chặn việc khởi tạo tự do bằng từ khóa 'new'
    private UserSession() {}

    // 3. Hàm toàn cục để lấy thực thể duy nhất
    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    // Cất user vào session (Khi đăng nhập thành công)
    public void setLoggedInUser(Users user) {
        this.loggedInUser = user;
    }

    // Lấy user ra để sử dụng
    public Users getLoggedInUser() {
        return loggedInUser;
    }

    // Kiểm tra xem có ai đang đăng nhập không
    public boolean isLoggedIn() {
        return loggedInUser != null;
    }

    // Hủy session (Khi đăng xuất)
    public void clearSession() {
        this.loggedInUser = null;
    }

    public void logout() {
        if (this.loggedInUser != null) {
            System.out.println("Tạm biệt " + loggedInUser.getUsername() + "!");
            this.loggedInUser = null;
        } else {
            System.out.println("Lỗi: Hiện tại không có người dùng nào đăng nhập.");
        }
    }
}