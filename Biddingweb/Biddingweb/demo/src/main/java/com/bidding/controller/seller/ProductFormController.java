package com.bidding.controller.seller;

import com.bidding.shared.Item;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class ProductFormController implements Initializable {

    @FXML private Label     lblFormTitle;
    @FXML private TextField txtName;
    @FXML private ComboBox<String> cmbType;
    @FXML private TextArea  txtDescription;
    @FXML private TextField txtStartPrice;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private TextField txtEndHour;
    @FXML private TextField txtEndMinute;
    @FXML private Label     lblError;
    @FXML private Button    btnSave;

    private Item editingProduct;         // null = thêm mới, có giá trị = sửa
    private Runnable onSaveCallback;        // callback để reload bảng ở Seller Dashboard

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbType.setItems(FXCollections.observableArrayList(
                "Electronics", "Art", "Vehicle"
        ));
        dpStartDate.setValue(LocalDate.now());
        dpEndDate.setValue(LocalDate.now().plusDays(7));
        txtEndHour.setText("20");
        txtEndMinute.setText("00");
    }

    /**
     * Gọi từ SellerDashboardController.
     * product = null  → form thêm mới
     * product != null → điền sẵn data để sửa
     */
    public void setProduct(Item product) {
        this.editingProduct = product;

        if (product == null) {
            lblFormTitle.setText("Thêm sản phẩm mới");
            btnSave.setText("Lưu sản phẩm");
        } else {
            lblFormTitle.setText("Sửa sản phẩm");
            btnSave.setText("Cập nhật");

            // Điền data vào form
            txtName.setText(product.getName());
            cmbType.setValue(product.getType());
            txtStartPrice.setText(String.valueOf(product.getStartPrice()));
            // txtDescription, dpStartDate, dpEndDate, txtEndHour, txtEndMinute
            // → điền tương tự khi Product có đủ field
        }
    }

    /** Callback để SellerDashboardController reload bảng sau khi lưu */
    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    // ── Validate ─────────────────────────────────────────────────

    private boolean validate() {
        // Tên sản phẩm
        if (txtName.getText().trim().isEmpty()) {
            showError("Vui lòng nhập tên sản phẩm.");
            txtName.requestFocus();
            return false;
        }

        // Loại sản phẩm
        if (cmbType.getValue() == null) {
            showError("Vui lòng chọn loại sản phẩm.");
            return false;
        }

        // Giá khởi điểm
        try {
            long price = Long.parseLong(txtStartPrice.getText().trim());
            if (price <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Giá khởi điểm phải là số nguyên dương.");
            txtStartPrice.requestFocus();
            return false;
        }

        // Ngày kết thúc phải sau ngày bắt đầu
        if (dpEndDate.getValue() != null && dpStartDate.getValue() != null) {
            if (!dpEndDate.getValue().isAfter(dpStartDate.getValue())) {
                showError("Ngày kết thúc phải sau ngày bắt đầu.");
                return false;
            }
        }

        // Giờ hợp lệ
        try {
            int h = Integer.parseInt(txtEndHour.getText().trim());
            int m = Integer.parseInt(txtEndMinute.getText().trim());
            if (h < 0 || h > 23 || m < 0 || m > 59) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Giờ kết thúc không hợp lệ (HH: 0-23, MM: 0-59).");
            return false;
        }

        hideError();
        return true;
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    // ── Event Handlers ───────────────────────────────────────────

    @FXML
    private void handleSave(ActionEvent event) {
        if (!validate()) return;

        String name       = txtName.getText().trim();
        String type       = cmbType.getValue();
        long   startPrice = Long.parseLong(txtStartPrice.getText().trim());
        String desc       = txtDescription.getText().trim();
        String endTime    = dpEndDate.getValue() + " "
                + txtEndHour.getText() + ":" + txtEndMinute.getText();

        if (editingProduct == null) {
            // TODO: gọi API POST /api/products  { name, type, startPrice, description, endTime }
            System.out.println("Thêm mới: " + name + " | " + type + " | " + startPrice);
        } else {
            // TODO: gọi API PUT /api/products/{id}  { ... }
            System.out.println("Cập nhật ID=" + editingProduct.getId() + " → " + name);
        }

        // Gọi callback để Seller Dashboard reload bảng
        if (onSaveCallback != null) onSaveCallback.run();

        closeForm();
    }

    @FXML
    private void handleClose(ActionEvent event) {
        closeForm();
    }

    private void closeForm() {
        Stage stage = (Stage) btnSave.getScene().getWindow();
        stage.close();
    }
}

