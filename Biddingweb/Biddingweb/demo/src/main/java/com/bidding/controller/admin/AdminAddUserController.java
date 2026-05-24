package com.bidding.controller.admin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
    public void initialize() {
        if (cmbStatus.getValue() == null) {
            cmbStatus.setValue("Hoạt động");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        // Lấy Stage hiện tại của cửa sổ phụ và đóng nó lại
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSaveUser(ActionEvent event) {
        // 1. Thu thập dữ liệu từ các ô nhập liệu
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String role = cmbRole.getValue();
        String status = cmbStatus.getValue();
        String password = txtPassword.getText();

        // 2. Kiểm tra dữ liệu hợp lệ (Validation)
        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || role == null || password.isEmpty()) {
            System.out.println("Vui lòng nhập đầy đủ các trường có dấu *");
            return;
        }
        System.out.println("Đang lưu người dùng: " + fullName + " với vai trò " + role);

        // 4. Sau khi lưu thành công, đóng cửa sổ phụ
        Stage stage = (Stage) btnSave.getScene().getWindow();
        stage.close();
    }
}