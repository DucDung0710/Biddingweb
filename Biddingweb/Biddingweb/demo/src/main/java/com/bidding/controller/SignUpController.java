package com.bidding.controller;

import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import com.bidding.service.UserService;
import com.bidding.util.DBUtils;
import com.bidding.util.SceneManager;

// SignUpController updated to follow MVC: uses UserService and fx:id names match sign-up.fxml
public class SignUpController {

    @FXML
    private TextField txtFullName;

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private PasswordField txtConfirmPassword;

    @FXML
    private Button btnSignUp;

    @FXML
    private Button btnLogIn;

    @FXML
    private ComboBox<String> cmbRole;

    @FXML
    private Label lblError;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        ObservableList<String> roles = FXCollections.observableArrayList("Bidder", "Seller");
        cmbRole.setItems(roles);
        cmbRole.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        String fullName = txtFullName.getText();
        String email = txtEmail.getText();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        String selectedRole = cmbRole.getValue();

        // basic validation
        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không khớp!");
            return;
        }

        boolean ok = userService.register(fullName,email, password, confirmPassword, selectedRole);
        if (ok) {
            clearFields();
            try {
                SceneManager.switchToLogin();
            } catch (IOException e) {
                e.printStackTrace();
                DBUtils.showErrorAlert("Error", "Không thể thực hiện hành động!");
            }
        } else {
            showError("Đăng ký thất bại. Vui lòng thử lại.");
        }
    }

    @FXML
    private void handleLogIn(ActionEvent event) {
        try {
            SceneManager.switchToLogin();
        } catch (IOException e) {
            e.printStackTrace();
            DBUtils.showErrorAlert("Error", "Không thể thực hiện hành động!");
        }
    }

    private void clearFields() {
        if (txtFullName != null) txtFullName.clear();
        if (txtEmail != null) txtEmail.clear();
        if (txtPassword != null) txtPassword.clear();
        if (txtConfirmPassword != null) txtConfirmPassword.clear();
    }

    private void showError(String message) {
        if (lblError != null) {
            lblError.setText(message);
            lblError.setVisible(true);
            lblError.setManaged(true);
        }
    }
}