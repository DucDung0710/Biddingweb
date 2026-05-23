package com.bidding.controller.admin; // Đã đồng bộ đúng package quản lý admin của bạn

import com.bidding.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import java.io.IOException;

public class AdminUserController {

    @FXML private HBox navOverview, navUsers, navAuctions, navProducts, navWallet, navAuctionHistory, navNotifications, btnLogout;
    @FXML private Label lblPendingBadge; // Badge thông báo số lượng đơn ví chờ duyệt

    @FXML private Button btnAddUser;
    @FXML private TextField txtSearchUser;
    @FXML private ComboBox<String> cmbRoleFilter;
    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private TableView<?> tblUsers;
    @FXML private Pagination pagination;

    @FXML
    public void initialize() {
        // --- LIÊN KẾT SIDEBAR ---

        navOverview.setOnMouseClicked(e -> {
            try {
                SceneManager.switchToAdminDashboard();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        // Mục navUsers đang active ở màn hình này nên không cần bắt sự kiện tự chuyển cảnh [cite: 219, 220, 221, 222]

        navAuctions.setOnMouseClicked(e -> {
            try {
                SceneManager.switchToAdminAuctionManagement();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        navProducts.setOnMouseClicked(e -> {
            try {
                SceneManager.switchToAdminProductManagement();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        navUsers.setOnMouseClicked(e -> {
            try {
                SceneManager.switchToAdminUserManagement();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        navAuctionHistory.setOnMouseClicked(e -> {
            try {
                SceneManager.switchToAdminAuctionHistory();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        navNotifications.setOnMouseClicked(e -> {
            try {
                SceneManager.switchToAdminNotifications();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });


        navWallet.setOnMouseClicked(e -> {
            try {
                SceneManager.switchToAdminWalletManagement();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        btnLogout.setOnMouseClicked(e -> {
            try {
                SceneManager.switchToLogin();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        // --- CÀI ĐẶT BỘ LỌC DỮ LIỆU ---
        cmbRoleFilter.getItems().addAll("Tất cả vai trò", "Bidder", "Seller", "Admin"); // [cite: 220]
        cmbStatusFilter.getItems().addAll("Tất cả trạng thái", "Hoạt động", "Bị khóa"); // [cite: 221]

        // Đổ số liệu mẫu cho Badge ví tiền trên Sidebar [cite: 219, 220, 221, 222]
        if (lblPendingBadge != null) {
            lblPendingBadge.setText("3");
        }
    }

    @FXML
    private void handleFilterUsers() {
        String search = txtSearchUser.getText(); // [cite: 219]
        String role = cmbRoleFilter.getValue(); // [cite: 220]
        String status = cmbStatusFilter.getValue(); // [cite: 221]

        System.out.println("Thực hiện tìm kiếm User: " + search + " | " + role + " | " + status);
        // Thêm code lọc dữ liệu từ CSDL của bạn ở đây
    }

    @FXML
    private void handleAddUser() {
        System.out.println("Mở Form/Dialog thêm User mới"); // [cite: 198]
        // Có thể dùng một Alert hoặc Stage phụ để làm form popup
    }
}