package com.bidding.shared;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class UserManager {
    // Lưu trữ tất cả người dùng (Key: Username, Value: Đối tượng Users)
    private HashMap<String, Users> allUsers = new HashMap<>();
    
    // Danh sách các email "quyền lực" được phép đăng ký Admin
    private final List<String> AUTHORIZED_ADMIN_EMAILS = Arrays.asList(
        "25023196@vnu.edu.vn",
        "25023427@vnu.edu.vn",
        "25023249@vnu.edu.vn",
        "bbuoi812@gmail.com"
        );

    // --- 1. SIGN UP (Đăng ký) ---
    public boolean signUp(String id, String username, String password, String email, String roleType) {
        if (allUsers.containsKey(username)) {
            System.out.println("Lỗi: Tên đăng nhập đã tồn tại!");
            return false;
        }

        // 2. Kiểm tra quyền Admin (Logic bạn yêu cầu)
    if (roleType.equalsIgnoreCase("Admin") && !AUTHORIZED_ADMIN_EMAILS.contains(email)) {
        System.out.println("Lỗi: Gmail chưa được đăng ký cho tài khoản Admin!");
        return false;
    }
        Users newUser;
        if (roleType.equalsIgnoreCase("Bidder")) {
            newUser = new Bidder(username, password, id, email);
        } else if (roleType.equalsIgnoreCase("Seller")) {
            newUser = new Seller(username, password, id, email);
        } else if (roleType.equalsIgnoreCase("Admin")) {
            newUser = new Admin(username, password, id, email);
        } else {
            System.out.println("Lỗi: Loại người dùng không hợp lệ!");
            return false;
        }
        allUsers.put(username, newUser);
        System.out.println("Đăng ký thành công tài khoản: " + username);
        return true;
    }

    // --- 2. SIGN IN (Đăng nhập) ---
    public Users signIn(String username, String password) {
        if (allUsers.containsKey(username)) {
            Users user = allUsers.get(username);
            if (user.getPassword().equals(password)) { // So sánh mật khẩu
                System.out.println("Đăng nhập thành công! Chào " + username);
                return user;
            }
        }
        System.out.println("Lỗi: Sai tài khoản hoặc mật khẩu!");
        return null;
    }

    public void makeAdmin(Users currentUser, String targetUsername) {
    // 1. Kiểm tra quyền của người đang thao tác
        if (currentUser == null || !currentUser.getRole().equalsIgnoreCase("Admin")) {
            System.out.println("Lỗi: Chỉ Admin mới có quyền thực hiện!");
                return;
    }

    // 2. Tìm người dùng cần thăng chức
    Users user = allUsers.get(targetUsername);
        if (user != null) {
            user.setRole("Admin"); 
            System.out.println("Đã thăng chức Admin cho: " + targetUsername);
    }   else {
            System.out.println("Lỗi: Không tìm thấy người dùng này.");
    }

}
    //Xóa người dùng (Chỉ Admin mới có quyền xóa, không cho phép xóa Admin khác hoặc tự xóa chính mình)
    public void deleteUser(Users currentUser, String targetId, ItemManager itemManager) {
        // 1. Kiểm tra quyền Admin
        if (currentUser == null || !currentUser.getRole().equalsIgnoreCase("Admin")) {
            System.out.println("Lỗi: Không có quyền xóa người dùng!");
            return;
        }

        // 2. Không cho phép Admin tự xóa chính mình 
        if (currentUser.getId().equals(targetId)) {
            System.out.println("Lỗi: Bạn không thể tự xóa tài khoản của chính mình!");
            return;
        }           

        // 3. Tìm người dùng cần xóa
        Users user = null;
        for (Users u : allUsers.values()) {
            if (u.getId().equals(targetId)) {
                user = u;
                break;
            }
        }

        if (user == null) {
            System.out.println("Lỗi: Người dùng không tồn tại.");
            return;
        }

        if (user.getRole().equalsIgnoreCase("Admin")) {
            System.out.println("Lỗi: Admin không có quyền xóa các Admin khác.");
            return;
        }

        // 4. Thực hiện xóa
        if (allUsers.containsKey(user.getUsername())) {
            itemManager.deleteItemsByUserId(targetId); // Xóa sản phẩm liên quan đến người dùng này (nếu có)
            allUsers.remove(user.getUsername());
            System.out.println("Đã xóa người dùng: " + user.getUsername());
        } else {
            System.out.println("Lỗi: Người dùng không tồn tại.");
        }
    }
}


