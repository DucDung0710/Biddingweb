package com.bidding.controller;

import com.bidding.util.SocketClient;
import com.google.gson.JsonObject;
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
import com.bidding.util.SceneManager;

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
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Vui lòng điền đầy đủ tất cả các trường thông tin!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không khớp!");
            return;
        }

        // Tạo request JSON gửi lên server
        JsonObject request = new JsonObject();
        request.addProperty("action",          "REGISTER");
        request.addProperty("fullName",        fullName);
        request.addProperty("email",           email);
        request.addProperty("password",        password);
        request.addProperty("confirmPassword", confirmPassword);
        request.addProperty("role",            selectedRole);

        // Gửi qua socket
        JsonObject response = SocketClient.getInstance().sendRequest(request);

        if ("OK".equals(response.get("status").getAsString())) {
            clearFields();
            SceneManager.switchToLogin();
        } else {
            showError(response.get("message").getAsString());
        }
    }

    @FXML
    private void handleLogIn(ActionEvent event) {
            SceneManager.switchToLogin();
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