package com.bidding.shared;

import com.bidding.dao.JdbcUserDAO;
import com.bidding.dao.UserDAO;
import java.util.Arrays;
import java.util.List;

public class UserManager {

    // Gọi sang tầng DAO để xử lý SQLite
    private final UserDAO userDao = new JdbcUserDAO();

    // Danh sách các email "quyền lực" được phép đăng ký Admin
    private final List<String> AUTHORIZED_ADMIN_EMAILS = Arrays.asList(
            "25023196@vnu.edu.vn",
            "25023427@vnu.edu.vn",
            "25023249@vnu.edu.vn",
            "bbuoi812@gmail.com"
    );

    // --- 1. SIGN UP (Đăng ký) ---
    public boolean signUp(int id, String username, String password, String email, String roleType) {
        // Kiểm tra xem tên tài khoản đã tồn tại dưới SQLite chưa
        if (userDao.existsByUsername(username)) {
            System.out.println("Lỗi: Tên đăng nhập đã tồn tại!");
            return false;
        }

        // Kiểm tra quyền Admin dựa trên Whitelist Email
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

        // Đẩy thẳng thực thể xuống lưu trữ cố định trong file .db
        return userDao.insert(newUser);
    }

    // --- 2. SIGN IN (Đăng nhập) ---
    public Users signIn(String username, String password) {
        // Tìm kiếm thông tin trực tiếp từ database SQLite
        Users user = userDao.findByUsername(username);

        if (user != null && user.getPassword().equals(password)) {
            System.out.println("Đăng nhập thành công! Chào " + username);
            return user;
        }

        System.out.println("Lỗi: Sai tài khoản hoặc mật khẩu!");
        return null;
    }

    // --- 3. THĂNG CHỨC ADMIN ---
    public void makeAdmin(Users currentUser, String targetUsername) {
        if (currentUser == null || !currentUser.getRole().equalsIgnoreCase("Admin")) {
            System.out.println("Lỗi: Chỉ Admin mới có quyền thực hiện!");
            return;
        }

        Users user = userDao.findByUsername(targetUsername);
        if (user != null) {
            user.setRole("Admin");
            // Thực thi lệnh cập nhật dòng dữ liệu trong SQLite
            userDao.insert(user); // Hoặc bạn có thể viết thêm hàm userDao.update(user) nếu cần
            System.out.println("Đã thăng chức Admin cho: " + targetUsername);
        } else {
            System.out.println("Lỗi: Không tìm thấy người dùng này.");
        }
    }

    // 4. XÓA NGƯỜI DÙNG
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

        // Không cho phép Admin tự xóa chính mình
        if (currentUser.getId() == targetIdInt) {
            System.out.println("Lỗi: Bạn không thể tự xóa tài khoản của chính mình!");
            return;
        }

        //Tìm kiếm user cần xóa trực tiếp theo ID từ hàm của DAO SQLite
        Users user = userDao.findByUsername(targetId);

        // Đoạn này ta tạm thời quét kiểm tra quyền từ email/username của user nhận về
        if (user == null) {
            System.out.println("Lỗi: Người dùng không tồn tại.");
            return;
        }

        if (user.getRole().equalsIgnoreCase("Admin")) {
            System.out.println("Lỗi: Admin không có quyền xóa các Admin khác.");
            return;
        }

        // Quét sạch sản phẩm của người dùng này thông qua itemManager trước khi xóa dòng trong DB
        if (itemManager != null) {
            itemManager.deleteItemsByUserId(targetId);
        }

        // Thực thi lệnh DELETE hoàn toàn khỏi bảng dữ liệu SQLite
        boolean isDeleted = userDao.deleteById(targetId);
        if (isDeleted) {
            System.out.println("Đã xóa người dùng thành công khỏi hệ thống.");
        } else {
            System.out.println("Lỗi hệ thống: Xóa thất bại.");
        }
    }
}