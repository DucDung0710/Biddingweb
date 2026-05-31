package com.bidding.controller;

import com.google.gson.JsonObject;
import com.bidding.util.SocketClient;
import com.bidding.util.SceneManager;
import com.bidding.shared.UserSession;
import com.bidding.shared.Users;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

public class LogInController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Button btnSignUp;
    @FXML private CheckBox chkRemember;
    @FXML private HBox boxError;
    @FXML private Label lblError;
    @FXML private Region spacerError;

    @FXML
    private void handleLogIn() {
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showInlineError("Vui lòng nhập email và mật khẩu!");
            return;
        }

        // Tạo gói tin Request gửi lên Server
        JsonObject request = new JsonObject();
        request.addProperty("action", "LOGIN");
        request.addProperty("email", email);
        request.addProperty("password", password);

        // Gửi và nhận phản hồi từ Server qua Socket
        JsonObject response = SocketClient.getInstance().sendRequest(request);

        // 🔥 Đã sửa: Kiểm tra đúng trạng thái "OK" giống như RequestRouter của Server trả về
        if (response != null && "OK".equals(response.get("status").getAsString())) {

            // Đóng gói trực tiếp từ gói tin 'response' phẳng, không qua đối tượng lồng "data" nữa
            Users user = new Users();
            user.setId(response.has("id") ? response.get("id").getAsInt() : 0);
            user.setUsername(response.get("username").getAsString());
            user.setEmail(response.get("email").getAsString());
            user.setRole(response.get("role").getAsString());
            if (response.has("balance")) {
                user.setBalance(response.get("balance").getAsDouble());
            }

            // Lưu Model vào bộ nhớ phiên dùng chung
            UserSession.getInstance().setLoggedInUser(user);

            // Điều hướng màn hình dựa trên vai trò (Role)
            String role = user.getRole();

            // Ép chữ thường/chữ hoa về chung một chuẩn so sánh không phân biệt hoa thường
            if ("admin".equalsIgnoreCase(role)) {
                SceneManager.switchToAdminDashboard();
            } else if ("seller".equalsIgnoreCase(role)) {
                SceneManager.switchToSellerDashboard();
            } else {
                SceneManager.switchToDashboard();
            }
        } else {
            String errorMsg = (response != null && response.has("message"))
                    ? response.get("message").getAsString()
                    : "Đăng nhập thất bại. Vui lòng kiểm tra lại!";
            showInlineError(errorMsg);
        }
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        try {
            SceneManager.switchToSignUp();
        } catch (Exception e) {
            System.err.println("Lỗi chuyển sang màn hình đăng ký: " + e.getMessage());
        }
    }

    private void showInlineError(String message) {
        if (lblError != null && boxError != null && spacerError != null) {
            lblError.setText(message);
            boxError.setVisible(true);
            spacerError.setVisible(true);
            boxError.setManaged(true);
            spacerError.setManaged(true);
        }
    }
}