package uilogin;

import javafx.scene.control.Alert;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DBUtils {

    private static final String URL = "jdbc:mysql://localhost:3306/login?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";
    /*
     Login bằng cách kiểm tra email và password trong database, nếu đúng trả về true, ngược lại trả về false và hiển thị lỗi
     */

    public static boolean loginUser(String email, String password) {

        String query = "SELECT * FROM users WHERE email = ? AND password = ?";


        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);

             PreparedStatement ps = connection.prepareStatement(query)) {


            ps.setString(1, email);

            ps.setString(2, password);


            try (ResultSet resultSet = ps.executeQuery()) {

                if (resultSet.next()) {

                    System.out.println("User logged in successfully!");

                    return true;

                } else {

                    showErrorAlert("Login Failed", "Email hoặc mật khẩu không đúng!");

                    return false;

                }

            }

        } catch (Exception e) {

            e.printStackTrace();

            showErrorAlert("Error", "Database connection error!");

            return false;

        }

    }

    public static boolean signUpUser(String name, String email, String password, String confirmPassword) {
        // Xử lý bỏ trống và kiểm tra mật khẩu xác nhận
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showErrorAlert("Lỗi", "Vui lòng điền đầy đủ thông tin!");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            showErrorAlert("Lỗi", "Mật khẩu xác nhận không khớp!");
            return false;
        }
        // Kiểm tra email đã tồn tại chưa và chèn dữ liệu mới nếu chưa tồn tại
        String checkQuery = "SELECT email FROM users WHERE email = ?";
        String insertQuery = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {

            // Bước 1: Kiểm tra email tồn tại chưa
            try (PreparedStatement psCheck = connection.prepareStatement(checkQuery)) {
                psCheck.setString(1, email);
                ResultSet rs = psCheck.executeQuery();
                if (rs.next()) {
                    showErrorAlert("Lỗi", "Email này đã được đăng ký!");
                    return false;
                }
            }

            // Bước 2: Chèn dữ liệu mới
            try (PreparedStatement psInsert = connection.prepareStatement(insertQuery)) {
                psInsert.setString(1, name);
                psInsert.setString(2, email);
                psInsert.setString(3, password);

                int rows = psInsert.executeUpdate();
                if (rows > 0) {
                    showSuccessAlert("Thành công", "Đăng ký tài khoản thành công!");
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Lỗi Database", "Không thể kết nối hoặc lưu dữ liệu: " + e.getMessage());
        }
        return false;
    }

    /**
     * Show thông báo thành công
     */

    public static void showSuccessAlert(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);

        alert.setHeaderText(null);

        alert.setContentText(message);

        alert.showAndWait();

    }


    /**
     * Show thông báo lỗi
     */

    public static void showErrorAlert(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);

        alert.setTitle(title);

        alert.setHeaderText(null);

        alert.setContentText(message);

        alert.showAndWait();
    }
}