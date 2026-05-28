package com.bidding.shared;

import com.bidding.dao.JdbcUserDAO;
import com.bidding.dao.UserDAO;
import java.util.Arrays;
import java.util.List;

public class UserManager {
    private final UserDAO userDao = new JdbcUserDAO();
    private int nextUserId = 1;
    private static final List<String> AUTHORIZED_ADMIN_EMAILS = Arrays.asList(
            "25023196@vnu.edu.vn",
            "25023427@vnu.edu.vn",
            "25023249@vnu.edu.vn",
            "bbuoi812@gmail.com"
    );

    public boolean signUp(String username, String password, String email, String roleType) {
        if (userDao.existsByUsername(username)) {
            System.out.println("Lỗi: Tên đăng nhập đã tồn tại!");
            return false;
        }
        if (userDao.existsByEmail(email)) {
            System.out.println("Lỗi: Email đã được sử dụng bởi tài khoản khác!");
            return false;
        }
        if (roleType.equalsIgnoreCase("Admin") && !AUTHORIZED_ADMIN_EMAILS.contains(email)) {
            System.out.println("Lỗi: Gmail chưa được đăng ký cho tài khoản Admin!");
            return false;
        }

        int id = generateUserId();
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

        return userDao.insert(newUser);
    }

    private int generateUserId() {
        while (true) {
            int candidate = nextUserId++;
            if (userDao.findByUsername(String.valueOf(candidate)) == null) {
                return candidate;
            }
        }
    }

    public Users signIn(String username, String password) {
        Users user = userDao.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            System.out.println("Đăng nhập thành công! Chào " + username);
            return user;
        }
        System.out.println("Lỗi: Sai tài khoản hoặc mật khẩu!");
        return null;
    }

    public void makeAdmin(Users currentUser, String targetUsername) {
        if (currentUser == null || !currentUser.getRole().equalsIgnoreCase("Admin")) {
            System.out.println("Lỗi: Chỉ Admin mới có quyền thực hiện!");
            return;
        }
        Users user = userDao.findByUsername(targetUsername);
        if (user != null) {
            user.setRole("Admin");
            userDao.insert(user);
            System.out.println("Đã thăng chức Admin cho: " + targetUsername);
        } else {
            System.out.println("Lỗi: Không tìm thấy người dùng này.");
        }
    }

    public void deleteUser(Users currentUser, String targetId, ItemManager itemManager) {
        if (currentUser == null || !currentUser.getRole().equalsIgnoreCase("Admin")) {
            System.out.println("Lỗi: Không có quyền xóa người dùng!");
            return;
        }
        int targetIdInt;
        try {
            targetIdInt = Integer.parseInt(targetId);
        } catch (NumberFormatException e) {
            System.out.println("Lỗi: Mã định danh ID không hợp lệ.");
            return;
        }
        if (currentUser.getId() == targetIdInt) {
            System.out.println("Lỗi: Bạn không thể tự xóa tài khoản của chính mình!");
            return;
        }
        Users user = userDao.findByUsername(targetId);
        if (user == null) {
            System.out.println("Lỗi: Người dùng không tồn tại.");
            return;
        }
        if (user.getRole().equalsIgnoreCase("Admin")) {
            System.out.println("Lỗi: Admin không có quyền xóa các Admin khác.");
            return;
        }
        if (itemManager != null) {
            itemManager.deleteItemsByUserId(targetIdInt);
        }
        boolean isDeleted = userDao.deleteById(targetId);
        if (isDeleted) {
            System.out.println("Đã xóa người dùng thành công khỏi hệ thống.");
        } else {
            System.out.println("Lỗi hệ thống: Xóa thất bại.");
        }
    }
}
