package com.bidding.controller;

import com.google.gson.JsonObject;
import com.bidding.util.SocketClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import com.bidding.util.SceneManager;

public class LogInController {

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField txtPassword;

    @FXML
    @SuppressWarnings("unused")
    private Button btnLogin;

    @FXML
    @SuppressWarnings("unused")
    private Button btnSignUp;

    @FXML
    @SuppressWarnings("unused")
    private CheckBox chkRemember;

    @FXML
    private HBox boxError;

    @FXML
    private Label lblError;

    @FXML
    private Region spacerError;

    @FXML
    @SuppressWarnings("unused")
    private void handleLogIn() {
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showInlineError("Vui lòng nhập email và mật khẩu!");
            return;
        }

        // Tạo request JSON gửi lên server
        JsonObject request = new JsonObject();
        request.addProperty("action",   "LOGIN");
        request.addProperty("email",    email);
        request.addProperty("password", password);

        JsonObject response = SocketClient.getInstance().sendRequest(request);
        if ("OK".equals(response.get("status").getAsString())) {
            com.bidding.shared.Users user = new com.bidding.shared.Users();
            user.setId(response.get("id").getAsInt());
            user.setUsername(response.get("username").getAsString());
            user.setEmail(response.get("email").getAsString());
            user.setRole(response.get("role").getAsString());
            user.setBalance(response.get("balance").getAsDouble());

            com.bidding.shared.UserSession.getInstance().setLoggedInUser(user);

            String role = user.getRole();
            if (role.equalsIgnoreCase("Admin"))       SceneManager.switchToAdminDashboard();
            else if (role.equalsIgnoreCase("Seller")) SceneManager.switchToSellerDashboard();
            else                                      SceneManager.switchToDashboard();
        } else {
            showInlineError(response.get("message").getAsString());
        }
    }
    @FXML
    @SuppressWarnings("unused")
    private void handleSignUp(ActionEvent event) {
        try {
            SceneManager.switchToSignUp();
        } catch (Exception e) {
            System.err.println("Lỗi chuyển sang màn hình đăng ký: " + e.getMessage());
            // Log exception instead of printing stack trace
        }
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
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
