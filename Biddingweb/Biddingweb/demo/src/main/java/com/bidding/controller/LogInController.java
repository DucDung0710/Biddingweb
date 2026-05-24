package com.bidding.controller;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import com.bidding.service.UserService;
import com.bidding.util.DBUtils;
import com.bidding.util.SceneManager;
import com.bidding.util.SessionStore;

public class LogInController {

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnLogin;

    @FXML
    private Button btnSignUp;

    @FXML
    private CheckBox chkRemember;

    @FXML
    private HBox boxError;

    @FXML
    private Label lblError;

    @FXML
    private Region spacerError;

    // Service layer
    private final UserService userService = new UserService();

    @FXML
    private void handleLogIn() {
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showInlineError("Vui lòng nhập email và mật khẩu!");
            return;
        }

        com.bidding.shared.Users user = userService.login(email, password);
        if (user != null) {
            System.out.println("Login successful: " + email);
            // store in session (use SessionStore to avoid touching shared.UserSession)
            SessionStore.setCurrentUser(user);
            clearFields();
            hideInlineError();
                String role = user.getRole();
                if (role == null) role = "Bidder";
                if (role.equalsIgnoreCase("Admin")) {
                    SceneManager.switchToAdminDashboard();
                } else if (role.equalsIgnoreCase("Seller")) {
                    SceneManager.switchToSellerDashboard();
                } else {
                    SceneManager.switchToDashboard();
                }

        } else {
            showInlineError("Email hoặc mật khẩu không đúng!");
        }
    }

    // Chuyển sang màn hình đăng ký khi người dùng nhấn nút "Sign Up", đồng thời xử lý lỗi nếu không thể chuyển
    @FXML
    private void handleSignUp(ActionEvent event) {
        try {
            SceneManager.switchToSignUp();
        } catch (Exception e) {
            System.err.println("Lỗi chuyển sang màn hình đăng ký: " + e.getMessage());
            e.printStackTrace();}
    }

    private void clearFields() {
        txtEmail.clear();
        txtPassword.clear();
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

    private void hideInlineError() {
        if (lblError != null && boxError != null && spacerError != null) {
            lblError.setText("");
            boxError.setVisible(false);
            spacerError.setVisible(false);
            boxError.setManaged(false);
            spacerError.setManaged(false);
        }
    }
}
