package com.bidding.controller.admin;

import com.bidding.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import java.io.IOException;

public class AdminDashboardController {

    @FXML private HBox navOverview, navUsers, navAuctions, navProducts, navWallet, navAuctionsHistory, navNotifications, btnLogout;
    @FXML private Label lblPendingBadge; // Thêm badge thông báo số lượng đơn ví chờ duyệt

    @FXML private Label lblTotalUsers, lblActiveAuctions, lblIssues;
    @FXML private TableView<?> tblActiveAuctions;
    @FXML private TableView<?> tblNewUsers;
    @FXML private Button btnViewAllAuctions, btnViewAllUsers;

    @FXML
    public void initialize() {
        // --- LIÊN KẾT SIDEBAR ---
        // Mục navOverview (Dashboard) đang active ở màn hình này nên không cần bắt sự kiện tự chuyển cảnh

        navUsers.setOnMouseClicked(e -> handleSwitchUserManagement());
        navAuctions.setOnMouseClicked(e -> handleSwitchAuctionManagement());
        navProducts.setOnMouseClicked(e -> handleSwitchProductManagement());
        navWallet.setOnMouseClicked(e -> handleSwitchWalletManagement());
        navNotifications.setOnMouseClicked(e -> handleSwitchNotifications());
        navAuctionsHistory.setOnMouseClicked(e -> handleSwitchAuctionHistory());
        btnLogout.setOnMouseClicked(e -> handleLogout());

        // --- CÁC NÚT XEM CHI TIẾT TRÊN MAIN PANEL ---
        btnViewAllAuctions.setOnAction(e -> handleSwitchAuctionManagement());
        btnViewAllUsers.setOnAction(e -> handleSwitchUserManagement());

        // Đổ dữ liệu thống kê ban đầu (Có thể kết nối Service/Database tại đây)
        lblTotalUsers.setText("1,248");
        lblActiveAuctions.setText("18");
        lblIssues.setText("3");

        // Cài đặt số hiển thị mẫu cho Badge ví tiền (Ví dụ: Đang có 3 phiếu chờ xử lý)
        if (lblPendingBadge != null) {
            lblPendingBadge.setText("3");
        }
    }

    private void handleSwitchUserManagement() {
        try {
            SceneManager.switchToAdminUserManagement();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleSwitchAuctionManagement() {
        try {
            SceneManager.switchToAdminAuctionManagement();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleSwitchProductManagement() {
        try {
            SceneManager.switchToAdminProductManagement();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleSwitchWalletManagement() {
        try {
            SceneManager.switchToAdminWalletManagement();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleSwitchNotifications() {
        try {
            SceneManager.switchToAdminNotifications();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleSwitchAuctionHistory() {
        try {
            SceneManager.switchToAdminAuctionHistory();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleLogout() {
        try {
            SceneManager.switchToLogin();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}