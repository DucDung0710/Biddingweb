package com.bidding.controller.admin;

import com.bidding.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import java.io.IOException;

public class WalletAdminController {

    @FXML private HBox navOverview, navUsers, navAuctions, navProducts, navWallet, navAuctionHistory, navNotifications, btnLogout;
    @FXML private Label lblPendingBadge;
    @FXML private Label lblStatPending, lblStatApproved, lblStatTotalApproved, lblStatRejected;
    @FXML private Button btnRefresh;
    @FXML private TableView<?> tblPendingRequests;
    @FXML
    public void initialize() {
        // --- LIÊN KẾT SIDEBAR ---
        navOverview.setOnMouseClicked(e -> { try { SceneManager.switchToAdminDashboard(); } catch (IOException ex) { ex.printStackTrace(); } });
        navUsers.setOnMouseClicked(e -> { try { SceneManager.switchToAdminUserManagement(); } catch (IOException ex) { ex.printStackTrace(); } });
        navAuctions.setOnMouseClicked(e -> { try { SceneManager.switchToAdminAuctionManagement(); } catch (IOException ex) { ex.printStackTrace(); } });
        navNotifications.setOnMouseClicked(e -> { try { SceneManager.switchToAdminNotifications(); } catch (IOException ex) { ex.printStackTrace(); } });
        navProducts.setOnMouseClicked(e -> { try { SceneManager.switchToAdminProductManagement(); } catch (IOException ex) { ex.printStackTrace(); } });
        navAuctionHistory.setOnMouseClicked(e -> { try { SceneManager.switchToAdminAuctionHistory(); } catch (IOException ex) { ex.printStackTrace(); } });
        btnLogout.setOnMouseClicked(e -> { try { SceneManager.switchToLogin(); } catch (IOException ex) { ex.printStackTrace(); } });

        // Mục navWallet đang active ở màn hình này nên không cần gán sự kiện click chuyển cảnh

        btnLogout.setOnMouseClicked(e -> {
            try { SceneManager.switchToLogin(); } catch (IOException ex) { ex.printStackTrace(); }
        });

        // --- CÁC CHỨC NĂNG CHÍNH ---
        btnRefresh.setOnAction(e -> handleRefreshRequests()); // [cite: 44]

        // Load dữ liệu ban đầu làm mẫu
        loadWalletData();
    }

    private void loadWalletData() {
        // Đã sửa: Đồng bộ số lượng Badge thông báo "3" đơn chờ duyệt giống như các màn hình khác
        lblPendingBadge.setText("3"); // [cite: 13]
        lblStatPending.setText("3"); // [cite: 28]

        lblStatApproved.setText("15"); // [cite: 32]
        lblStatTotalApproved.setText("42,500,000 ₫"); // [cite: 36]
        lblStatRejected.setText("2"); // [cite: 40]
    }

    private void handleRefreshRequests() {
        System.out.println("Đang kết nối database làm mới danh sách duyệt ví...");
        loadWalletData();
    }
}