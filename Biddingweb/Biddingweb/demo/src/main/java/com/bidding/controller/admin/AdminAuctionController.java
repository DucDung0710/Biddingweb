package com.bidding.controller.admin;

import com.bidding.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class AdminAuctionController {

    @FXML private HBox navOverview, navUsers, navProducts, navAuctions, navWallet, navNotifications, navAuctionHistory, btnLogout;
    @FXML private Label lblPendingBadge; // Badge thông báo số lượng đơn ví chờ duyệt

    @FXML private TextField txtSearchAuction; // [cite: 88]
    @FXML private ComboBox<String> cmbAucStatus; // [cite: 89]
    @FXML private ComboBox<String> cmbAucType; // [cite: 90]
    @FXML @SuppressWarnings("unused") private DatePicker dpFilter; // [cite: 91]

    @FXML
    public void initialize() {
        // --- LIÊN KẾT SIDEBAR ---

        navOverview.setOnMouseClicked(e -> {SceneManager.switchToAdminDashboard();});

        navProducts.setOnMouseClicked(e -> {SceneManager.switchToAdminProductManagement();});

        // Cài đặt nút bấm bổ sung trên dashboard (Xem tất cả)
        //btnViewAllUsers.setOnAction(e -> { try { SceneManager.switchToAdminUserManagement(); } catch (IOException ex) {} });
        //btnViewAllAuctions.setOnAction(e -> { try { SceneManager.switchToAdminAuctionManagement(); } catch (IOException ex) {} });


        navUsers.setOnMouseClicked(e ->{SceneManager.switchToAdminUserManagement();
        });

        // Mục navAuctions đang active ở màn hình này nên không cần bắt sự kiện tự chuyển cảnh

        navWallet.setOnMouseClicked(e -> {SceneManager.switchToAdminWalletManagement();});

        navAuctionHistory.setOnMouseClicked(e -> {SceneManager.switchToAdminAuctionHistory();});

        navNotifications.setOnMouseClicked(e -> {SceneManager.switchToAdminNotifications();});

        navAuctions.setOnMouseClicked(e -> {SceneManager.switchToAdminAuctionManagement();});



        btnLogout.setOnMouseClicked(e -> {SceneManager.switchToLogin();});

        // --- CÀI ĐẶT BỘ LỌC ---
        cmbAucStatus.getItems().addAll("Tất cả trạng thái", "OPEN", "RUNNING", "FINISHED", "SUSPENDED");
        cmbAucType.getItems().addAll("Tất cả loại", "Đấu giá truyền thống", "Đấu giá xu");

        // Đổ số liệu mẫu cho Badge ví tiền trên Sidebar
        if (lblPendingBadge != null) {
            lblPendingBadge.setText("3");
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleFilterAuctions() {
        System.out.println("Đang lọc danh sách phiên đấu giá theo yêu cầu: " + txtSearchAuction.getText()); // [cite: 88, 94]
        // Thực hiện cập nhật bảng tblAuctions [cite: 94]
    }
}