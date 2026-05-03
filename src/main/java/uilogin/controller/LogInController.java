package uilogin.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import uilogin.DBUtils;
import uilogin.SceneManager;

import java.io.IOException;

public class LogInController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button signUpButton;


    @FXML
    private void onLogin() {
        String email = usernameField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            DBUtils.showErrorAlert("Input Error", "Please enter email and password!");
            return;
        }

        // In thông báo đăng nhập thành công hoặc thất bại trong console, đồng thời hiển thị lỗi nếu đăng nhập thất bại
        if (DBUtils.loginUser(email, password)) {
            System.out.println("Login successful: " + email);
            clearFields();

        }
    }
    // Chuyển sang màn hình đăng ký khi người dùng nhấn nút "Sign Up", đồng thời xử lý lỗi nếu không thể chuyển
    @FXML
    private void onSignUp(ActionEvent event) {
        try {
            SceneManager.switchToSignUp();
        } catch (IOException e) {
            e.printStackTrace();
            DBUtils.showErrorAlert("Error", "Không thể thực hiện hành động!");
        }
    }

    private void clearFields() {
        usernameField.clear();
        passwordField.clear();
    }
}

