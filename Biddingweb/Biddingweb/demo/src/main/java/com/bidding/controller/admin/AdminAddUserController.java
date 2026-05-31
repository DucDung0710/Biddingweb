package com.bidding.controller.admin;

import com.bidding.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AdminAddUserController {

    @FXML
    private TextField txtFullName;

    @FXML
    private TextField txtEmail;

    @FXML
    private TextField txtPhone;

    @FXML
    private ComboBox<String> cmbRole;

    @FXML
    private ComboBox<String> cmbStatus;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnCancel;

    @FXML
    private Button btnSave;
    
    @FXML
    private Label lblError;
    
    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        if (cmbStatus.getValue() == null) {
            cmbStatus.setValue("Hoạt động");
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleCancel(ActionEvent event) {
        // Lấy Stage hiện tại của cửa sổ phụ và đóng nó lại
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleSaveUser(ActionEvent event) {
        // 1. Thu thập dữ liệu từ các ô nhập liệu
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String role = cmbRole.getValue();
        String password = txtPassword.getText();

        // 2. Kiểm tra dữ liệu hợp lệ (Validation)
        if (fullName.isEmpty() || email.isEmpty() || role == null || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ các trường có dấu *");
            return;
        }
        
        if (password.length() < 6) {
            showError("Mật khẩu phải tối thiểu 6 ký tự.");
            return;
        }
        
        // 3. Tạo user qua UserService (chỉ Admin được phép)
        boolean success = userService.register(fullName, email, password, password, role);
        
        if (success) {
            System.out.println("✓ Đã tạo người dùng: " + fullName + " (" + email + ") với vai trò " + role);
            Stage stage = (Stage) btnSave.getScene().getWindow();
            stage.close();
        } else {
            showError("Email đã tồn tại hoặc dữ liệu không hợp lệ.");
        }
    }
    
    private void showError(String message) {
        if (lblError != null) {
            lblError.setText(message);
            lblError.setVisible(true);
            lblError.setManaged(true);
        } else {
            System.out.println("Lỗi: " + message);
        }
    }
}