package com.bidding.controller.seller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import com.bidding.shared.Item;
import com.bidding.shared.UserSession;
import com.bidding.shared.Users;
import com.bidding.util.SocketClient;
import com.google.gson.JsonObject;
import java.net.URL;
import java.util.ResourceBundle;

public class ProductFormController implements Initializable {

    @FXML private Label lblFormTitle;
    @FXML private Button btnClose;
    @FXML private Button btnCancel;
    @FXML private Button btnSave;

    @FXML private Button btnUploadImage;
    @FXML private ImageView imgProductPreview;
    @FXML private Label lblUploadPlaceholder;

    @FXML private TextField txtName;
    @FXML private ComboBox<String> cmbType;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtStartPrice;
    @FXML private DatePicker dpStartDate;
    @FXML private TextField txtStartHour;
    @FXML private TextField txtStartMinute;
    @FXML private DatePicker dpEndDate;
    @FXML private TextField txtEndHour;
    @FXML private TextField txtEndMinute;
    @FXML private Label lblError;

    private File selectedImageFile;
    private Runnable onSaveCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbType.getItems().addAll("Điện tử", "Thời trang", "Gia dụng", "Sách & Thiết bị giải trí", "Khác");
        lblFormTitle.setText("Thêm sản phẩm mới");
        btnSave.setDefaultButton(true);
        btnClose.setCancelButton(true);
        txtDescription.setWrapText(true);

        addNumberValidation(txtStartHour, 23);
        addNumberValidation(txtStartMinute, 59);
        addNumberValidation(txtEndHour, 23);
        addNumberValidation(txtEndMinute, 59);
        addPriceValidation(txtStartPrice);
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn hình ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        Stage stage = (Stage) btnUploadImage.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            this.selectedImageFile = file;
            Image image = new Image(file.toURI().toString());
            imgProductPreview.setImage(image);
            lblUploadPlaceholder.setVisible(false);
        }
    }

    @FXML
    private void handleSave() {
        if (txtName.getText().isEmpty() || cmbType.getValue() == null || txtStartPrice.getText().isEmpty()) {
            showError("Vui lòng điền đầy đủ các thông tin bắt buộc (*)");
            return;
        }

        System.out.println("Sẵn sàng lưu sản phẩm: " + txtName.getText());

        // 1) Lấy user hiện tại từ session
        Users currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null) {
            showError("Bạn phải đăng nhập để đăng sản phẩm.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(txtStartPrice.getText());
        } catch (NumberFormatException nfe) {
            showError("Giá không hợp lệ.");
            return;
        }

        try {
            JsonObject request = new JsonObject();
            request.addProperty("action", "CREATE_ITEM");
            request.addProperty("sellerId", currentUser.getId());
            request.addProperty("itemName", txtName.getText().trim());
            request.addProperty("type", cmbType.getValue());
            request.addProperty("description", txtDescription.getText().trim());
            request.addProperty("firstprice", price);

            JsonObject response = SocketClient.getInstance().sendRequest(request);
            if (response == null || !"OK".equals(response.get("status").getAsString())) {
                String message = response != null && response.has("message") ? response.get("message").getAsString() : "Lỗi kết nối tới Server.";
                showError(message);
                return;
            }

            System.out.println("Sản phẩm đã được đăng và đang chờ Admin duyệt.");
        } catch (Exception ex) {
            System.err.println("Lỗi khi đăng sản phẩm: " + ex.getMessage());
            showError("Lỗi hệ thống khi lưu sản phẩm.");
            return;
        }

        if (onSaveCallback != null) {
            onSaveCallback.run();
        }

        handleClose();
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public void setProduct(Item item) {
        if (item == null) return;
        lblFormTitle.setText("Sửa sản phẩm");
        txtName.setText(item.getItemName());
        cmbType.setValue(item.getType());
        txtDescription.setText(item.getDescription());
        if (item.getFirstprice() != null) {
            txtStartPrice.setText(item.getFirstprice().toPlainString());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void addNumberValidation(TextField textField, int maxVal) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textField.setText(newValue.replaceAll("[^\\d]", ""));
            }
            if (!textField.getText().isEmpty()) {
                int val = Integer.parseInt(textField.getText());
                if (val > maxVal) {
                    textField.setText(String.valueOf(maxVal));
                }
            }
        });
    }

    private void addPriceValidation(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }
}

