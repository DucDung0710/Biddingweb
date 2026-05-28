package com.bidding.service;

import com.bidding.dao.JdbcUserDAO;
import com.bidding.dao.UserDAO;
import com.bidding.shared.Users;
import java.util.UUID;

public class UserService {
    private final UserDAO userDao = new JdbcUserDAO();

    // Logic Đăng nhập
    public Users login(String emailOrUsername, String password) {
        if (emailOrUsername == null || password == null) return null;

        String input = emailOrUsername.trim();
        Users u = userDao.findByEmail(input);
        if (u == null) {
            u = userDao.findByUsername(input);
        }

        if (u != null && u.getPassword().equals(password)) {
            return u;
        }
        return null; // Sai mật khẩu hoặc không tồn tại
    }

    // Logic Đăng ký tài khoản mới
    public boolean register(String username, String email, String password, String confirmPassword, String role) {
        // Nghiệp vụ kiểm tra dữ liệu đầu vào
        if (username == null || username.isBlank()) return false;
        if (email == null || !email.contains("@")) return false;
        if (password == null || password.length() < 6) return false;
        if (!password.equals(confirmPassword)) return false;

        String trimmedName = username.trim();
        String trimmedEmail = email.trim();

        // Kiểm tra trùng lặp bản ghi trong dữ liệu SQLite
        if (userDao.existsByEmail(trimmedEmail) || userDao.existsByUsername(trimmedName)) {
            return false;
        }

        Users newUser = new Users(trimmedName, password, 0, trimmedEmail); // ID sẽ được tự động sinh bởi cơ sở dữ liệu
        newUser.setRole(role);
        newUser.setBalance(0.0); // Khởi tạo ví tiền bằng 0

        return userDao.insert(newUser);
    }
}