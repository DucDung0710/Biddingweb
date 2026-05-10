// File: UserSession.java
package com.bidding.shared;

public class UserSession {
    private Users loggedInUser = null;

    // Cất user vào session (Khi đăng nhập thành công)
    public void setLoggedInUser(Users user) {
        this.loggedInUser = user;
    }

    // Lấy user ra để dùng cho các hàm makeAdmin, deleteUser...
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
            this.loggedInUser = null; // Xóa thông tin người dùng khỏi phiên làm việc
        } else {
            System.out.println("Lỗi: Hiện tại không có người dùng nào đăng nhập.");
        }
    }
}