package uilogin.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import uilogin.DBUtils;
import uilogin.SceneManager;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SignUpController implements Initializable {

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Button signUpButton;

    @FXML
    private Button loginButton;

    @FXML
    private void onSignUp(ActionEvent event) {
        String name = nameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (DBUtils.signUpUser(name, email, password, confirmPassword)) {
            clearFields();
            // Chuyển sang màn hình đăng nhập sau khi đăng ký thành công, đồng thời xử lý lỗi nếu không thể chuyển
            try {
                SceneManager.switchToLogin();
            } catch (IOException e) {
                e.printStackTrace();
                DBUtils.showErrorAlert("Error", "Không thể thực hiện hành động!");
            }
        }
    }

    // Chuyển sang màn hình đăng nhập khi người dùng nhấn nút "Login", đồng thời xử lý lỗi nếu không thể chuyển
    @FXML
    private void onLogin(ActionEvent event) {
        try {
            SceneManager.switchToLogin();
        } catch (IOException e) {
            e.printStackTrace();
            DBUtils.showErrorAlert("Error", "Không thể thực hiện hành động!");
        }
    }
    // Bắt buộc phải triển khai phương thức initialize để có thể sử dụng Initializable
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize dùng để thiết lập các giá trị mặc định hoặc cấu hình ban đầu cho các thành phần giao diện nếu cần
    }

    private void clearFields() {
        nameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }
}
