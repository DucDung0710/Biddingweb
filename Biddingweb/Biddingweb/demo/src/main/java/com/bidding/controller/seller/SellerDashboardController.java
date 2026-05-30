package com.bidding.controller.seller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

public class SellerDashboardController {

    // ==========================================
    // 1. ÁNH XẠ THÀNH PHẦN SIDEBAR (GÓC TRÁI DƯỚI)
    // ==========================================
    @FXML
    private Label lblUserName;   // Hiển thị tên thực tế của Seller

    @FXML
    private Label lblUserRole;   // Hiển thị vai trò (Mặc định: Seller)

    @FXML
    private Label lblAvatar;     // Vòng tròn chứa chữ cái đầu của tên

    @FXML
    private HBox navOverview;

    @FXML
    private HBox btnLogout;

    @FXML
    private Label lblNotifBadge;

    // ==========================================
    // 2. ÁNH XẠ CÁC THÀNH PHẦN KHÁC TRÊN GIAO DIỆN
    // ==========================================
    @FXML
    private Button btnTopAddProduct;

    @FXML
    private TextField txtSearch;

    @FXML
    private ComboBox<?> cmbFilterStatus; 

    @FXML
    private Label lblStatActive;

    @FXML
    private Label lblStatFinished;

    @FXML
    private Label lblStatRevenue;

    @FXML
    private Label lblStatPending;

    @FXML
    private TableView<?> tblProducts;

    @FXML
    private TableColumn<?, ?> colName;

    @FXML
    private TableColumn<?, ?> colType;

    @FXML
    private TableColumn<?, ?> colStartPrice;

    @FXML
    private TableColumn<?, ?> colCurrentPrice;

    @FXML
    private TableColumn<?, ?> colEndTime;

    @FXML
    private TableColumn<?, ?> colStatus;

    @FXML
    private TableColumn<?, ?> colActions;

    // ==========================================
    // 3. LOGIC XỬ LÝ KHỞI TẠO & CẬP NHẬT THÔNG TIN
    // ==========================================
    
    @FXML
    public void initialize() {
        // Hàm tự động chạy khi file FXML được nạp thành công
        // Bạn có thể thêm logic lấy danh sách sản phẩm hoặc cấu hình bảng tại đây
    }

    /**
     * HÀM TỰ ĐỘNG CẬP NHẬT THÔNG TIN SELLER
     * Hàm này sẽ được gọi từ LoginController sau khi xác thực tài khoản thành công.
     * * @param fullName Họ và tên đầy đủ của Seller 
     */
    public void setSellerInfo(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            lblUserName.setText("Khách");
            if (lblAvatar != null) lblAvatar.setText("K");
            return;
        }

        // 1. Cập nhật chuỗi tên người bán lên giao diện góc trái dưới
        lblUserName.setText(fullName);
        lblUserRole.setText("Seller");

        // 2. Logic tự động bóc tách chữ cái đầu tiên của Tên chính để làm Avatar
        // Ví dụ: "Nguyễn Văn Hoàng" -> Tách chuỗi lấy từ cuối "Hoàng" -> Lấy chữ "H"
        try {
            String[] words = fullName.trim().split("\\s+");
            String firstName = words[words.length - 1]; // Lấy từ cuối cùng
            
            if (!firstName.isEmpty() && lblAvatar != null) {
                String firstLetter = firstName.substring(0, 1).toUpperCase();
                lblAvatar.setText(firstLetter); // Thay chữ 'S' tĩnh bằng chữ cái của tên họ
            }
        } catch (Exception e) {
            // Đề phòng trường hợp chuỗi tên bị lỗi định dạng bất ngờ
            if (lblAvatar != null) lblAvatar.setText("S");
        }
    }

    // ==========================================
    // 4. ĐỊNH NGHĨA CÁC HÀM SỰ KIỆN (EVENT HANDLERS)
    // ==========================================
    
    @FXML
    private void handleAddProduct(ActionEvent event) {
        // Xử lý sự kiện thêm sản phẩm mới
    }

    @FXML
    private void handleFilter(ActionEvent event) {
        // Xử lý sự kiện lọc sản phẩm
    }

    @FXML
    private void handleOut(MouseEvent event) {
        // Xử lý sự kiện đăng xuất khi click chuột vào chữ Đăng xuất
        System.out.println("Đang đăng xuất hệ thống...");
    }
}