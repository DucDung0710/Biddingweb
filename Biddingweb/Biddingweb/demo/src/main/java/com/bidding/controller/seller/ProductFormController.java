package com.bidonline.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class ProductFormController implements Initializable {

    @FXML private Label lblFormTitle;
    @FXML private Button btnClose;
    @FXML private Button btnCancel;
    @FXML private Button btnSave;
    
    // Khai báo điều khiển Phần Ảnh
    @FXML private Button btnUploadImage;
    @FXML private ImageView imgProductPreview;
    @FXML private Label lblUploadPlaceholder;

    // Khai báo điều khiển thông tin Form
    @FXML private TextField txtName;
    @FXML private ComboBox<String> cmbType; 
    @FXML private TextArea txtDescription;
    @FXML private TextField txtStartPrice;
    @FXML private DatePicker dpStartDate;
    
    // PHẦN THÊM MỚI: Đồng bộ điều khiển Giờ/Phút Bắt đầu từ FXML
    @FXML private TextField txtStartHour;
    @FXML private TextField txtStartMinute;
    
    @FXML private DatePicker dpEndDate;
    @FXML private TextField txtEndHour;
    @FXML private TextField txtEndMinute;
    @FXML private Label lblError;

    // Biến lưu trữ file ảnh vật lý được chọn phục vụ chuyển giao Backend
    private File selectedImageFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Đổ dữ liệu tĩnh vào ComboBox phân loại sản phẩm
        cmbType.getItems().addAll("Điện tử", "Thời trang", "Gia dụng", "Sách & Thiết bị giải trí", "Khác");

        // Ràng buộc bảo vệ form cho Giờ và Phút BẮT ĐẦU (Mới bổ sung)
        addNumberValidation(txtStartHour, 23);     // Giờ bắt đầu tối đa là 23
        addNumberValidation(txtStartMinute, 59);   // Phút bắt đầu tối đa là 59

        // Ràng buộc bảo vệ form cho Giờ và Phút KẾT THÚC
        addNumberValidation(txtEndHour, 23);       // Giờ kết thúc tối đa là 23
        addNumberValidation(txtEndMinute, 59);     // Phút kết thúc tối đa là 59
        
        // Chặn gõ chữ vào ô giá
        addPriceValidation(txtStartPrice);        
    }

    /**
     * Logic bấm nút "Chọn hình ảnh" để mở thư mục máy tính
     */
    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn hình ảnh sản phẩm");
        
        // Tạo bộ lọc tệp tin đảm bảo người bán chọn đúng định dạng hình ảnh
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        Stage stage = (Stage) btnUploadImage.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            this.selectedImageFile = file;
            
            // Đọc tệp tin và nạp trực tiếp vào khung hiển thị Preview
            Image image = new Image(file.toURI().toString());
            imgProductPreview.setImage(image);
            
            // Ẩn văn bản gợi ý "Chưa có hình ảnh"
            lblUploadPlaceholder.setVisible(false);
        }
    }

    /**
     * Kiểm tra hợp lệ dữ liệu và tiến hành Lưu sản phẩm
     */
    @FXML
    private void handleSave() {
        // Kiểm tra các trường bắt buộc nhập văn bản/lựa chọn gốc
        if (txtName.getText().isEmpty() || cmbType.getValue() == null || txtStartPrice.getText().isEmpty()) {
            showError("Vui lòng điền đầy đủ các thông tin bắt buộc (*)");
            return;
        }

        // Bổ sung kiểm tra Ngày và Giờ bắt đầu/kết thúc xem đã điền đầy đủ chưa
        if (dpStartDate.getValue() == null || txtStartHour.getText().isEmpty() || txtStartMinute.getText().isEmpty() ||
            dpEndDate.getValue() == null || txtEndHour.getText().isEmpty() || txtEndMinute.getText().isEmpty()) {
            showError("Vui lòng nhập đầy đủ mốc thời gian bắt đầu và kết thúc (*)");
            return;
        }

        // Kiểm tra xem người bán đã chọn ảnh hay chưa
        if (selectedImageFile == null) {
            showError("Vui lòng đăng tải hình ảnh cho sản phẩm");
            return;
        }

        // => Tại đây: Viết code đẩy 'selectedImageFile' và thông tin chữ lên API Backend của bạn.
        System.out.println("Sẵn sàng lưu sản phẩm: " + txtName.getText());
        System.out.println("Thời gian bắt đầu: " + dpStartDate.getValue() + " " + txtStartHour.getText() + ":" + txtStartMinute.getText());
        System.out.println("Thời gian kết thúc: " + dpEndDate.getValue() + " " + txtEndHour.getText() + ":" + txtEndMinute.getText());
        System.out.println("File ảnh truyền đi: " + selectedImageFile.getAbsolutePath());

        handleClose();
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