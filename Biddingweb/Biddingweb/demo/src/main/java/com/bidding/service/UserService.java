package com.bidding.service;

import com.bidding.dao.JdbcUserDAO;
import com.bidding.dao.UserDAO;
import com.bidding.shared.ItemManager;
import com.bidding.shared.Users;
import com.bidding.validator.UserValidator;

public class UserService {

    private final UserDAO userDao = new JdbcUserDAO();
    private final UserValidator validator = new UserValidator();

    /**
     * 1. ĐĂNG NHẬP HỆ THỐNG
     */
    public Users login(String emailOrUsername, String password) {
        Users u = userDao.findByEmail(emailOrUsername);
        if (u == null) {
            u = userDao.findByUsername(emailOrUsername);
        }
        if (u == null) return null;

        if (u.getPassword().equals(password)) return u;
        return null;
    }

    /**
     * 2. ĐĂNG KÝ TÀI KHOẢN MỚI (Lưu MySQL trực tiếp)
     */
    public boolean register(String fullName, String email, String password, String confirmPassword, String role) {
        // Kiểm tra dữ liệu hợp lệ & kiểm tra whitelist Email Admin đặc quyền
        if (!validator.validateSignUpData(fullName, email, password, confirmPassword, role)) {
            return false;
        }
        // Đảm bảo không trùng tài khoản trong DB
        if (userDao.existsByEmail(email) || userDao.existsByUsername(fullName)) {
            return false;
        }

        String id = String.valueOf(System.currentTimeMillis());
        Users user = new Users(fullName, password, id, email);
        user.setRole(role);
        return userDao.insert(user);
    }

    /**
     * 3. XÓA NGƯỜI DÙNG VÀ XÓA SẠCH MẶT HÀNG ĐI KÈM CỦA HỌ
     * (Dùng cho chức năng Quản lý người dùng của Admin sau này)
     */
    public boolean deleteUserWithProducts(Users currentUser, String targetId, ItemManager itemManager) {
        // Phân quyền bảo mật: Chỉ Admin mới có quyền xóa tài khoản người khác
        if (currentUser == null || !currentUser.getRole().equalsIgnoreCase("Admin")) {
            System.out.println("Lỗi: Chỉ Admin mới có quyền xóa người dùng!");
            return false;
        }
        // Chặn hành vi tự xóa tài khoản chính mình
        if (currentUser.getId().equals(targetId)) {
            System.out.println("Lỗi: Bạn không thể tự xóa tài khoản của chính mình!");
            return false;
        }

        // Bước 1: Gọi ItemManager quét sạch các sản phẩm trong giỏ của người bán này (nếu có)
        itemManager.deleteItemsByUserId(targetId);

        // Bước 2: Thực hiện lệnh xóa dòng bản ghi trong bảng users của MySQL
        return userDao.deleteById(targetId);
    }
}