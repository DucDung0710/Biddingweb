package com.bidding.controller.admin;

import com.bidding.model.AuctionHistory;
import com.bidding.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

public class AdminHistoryController {

    // 1. Ánh xạ các thành phần Sidebar Menu
    @FXML private HBox navOverview, navUsers, navAuctions, navProducts, navWallet, navNotifications, btnLogout;
    @FXML @SuppressWarnings("unused") private HBox navAuctionHistory;
    @FXML private Label lblPendingBadge;

    // 2. Ánh xạ các thành phần lọc và bảng dữ liệu
    @FXML private DatePicker dpHistoryStart;
    @FXML private DatePicker dpHistoryEnd;
    @FXML private TableView<AuctionHistory> tblAuctionHistory;
    @FXML private TableColumn<AuctionHistory, String> colHisId;
    @FXML private TableColumn<AuctionHistory, String> colHisProduct;
    @FXML private TableColumn<AuctionHistory, String> colHisWinner;
    @FXML private TableColumn<AuctionHistory, String> colHisPrice;
    @FXML private TableColumn<AuctionHistory, String> colHisEndTime;

    // Danh sách lưu trữ dữ liệu lịch sử hiển thị
    private final ObservableList<AuctionHistory> historyList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // --- LIÊN KẾT ĐIỀU HƯỚNG SIDEBAR ---
        navOverview.setOnMouseClicked(e -> { SceneManager.switchToAdminDashboard();  });
        navUsers.setOnMouseClicked(e -> { SceneManager.switchToAdminUserManagement(); });
        navAuctions.setOnMouseClicked(e -> { SceneManager.switchToAdminAuctionManagement(); });
        navWallet.setOnMouseClicked(e -> { SceneManager.switchToAdminWalletManagement();});
        navNotifications.setOnMouseClicked(e -> { SceneManager.switchToAdminNotifications(); });
        btnLogout.setOnMouseClicked(e -> { SceneManager.switchToLogin(); });
        navProducts.setOnMouseClicked(e ->{ SceneManager.switchToAdminProductManagement(); });

        // --- ĐỒNG BỘ MAPPING CỘT BẢNG VỚI PROPERTIES CỦA MODEL ---
        colHisId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colHisProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colHisWinner.setCellValueFactory(new PropertyValueFactory<>("winner"));
        colHisPrice.setCellValueFactory(new PropertyValueFactory<>("finalPrice"));
        colHisEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        // Nạp dữ liệu mẫu ban đầu
        loadMockHistoryData();
    }

    /**
     * Nạp dữ liệu giả lập ban đầu khi vừa truy cập màn hình lịch sử
     */
    private void loadMockHistoryData() {
        historyList.clear();

        // Thêm một số bản ghi lịch sử demo
        historyList.add(new AuctionHistory("#AUC-9942", "Laptop ThinkPad X1 Gen 11", "lina", "32,500,000 ₫", "22/05/2026 11:30:00"));
        historyList.add(new AuctionHistory("#AUC-8812", "iPhone 15 Pro Max 256GB", "khanh", "26,200,000 ₫", "21/05/2026 18:15:22"));
        historyList.add(new AuctionHistory("#AUC-7741", "MacBook Pro M3 Max", "leminh", "68,000,000 ₫", "20/05/2026 09:05:10"));
        historyList.add(new AuctionHistory("#AUC-6102", "Giày Jordan 1 Retro High", "tram", "5,400,000 ₫", "19/05/2026 15:44:03"));

        tblAuctionHistory.setItems(historyList);
        lblPendingBadge.setText("3");
    }

    /**
     * Xử lý sự kiện bấm nút "Lọc dữ liệu"
     */
    @FXML
    @SuppressWarnings("unused")
    private void handleFilterHistory() {
        var startDate = dpHistoryStart.getValue();
        var endDate = dpHistoryEnd.getValue();

        if (startDate == null && endDate == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Vui lòng chọn khoảng thời gian cần lọc!", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        System.out.println("Đang thực hiện lọc lịch sử từ: " + startDate + " đến: " + endDate);
    }

    /**
     * Xử lý sự kiện bấm nút "Xuất dữ liệu Excel"
     */
    @FXML
    @SuppressWarnings("unused")
    private void handleExportExcel() {
        System.out.println("Đang xử lý xuất file Excel cho danh sách lịch sử đấu giá tài sản...");

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Xuất dữ liệu Excel thành công!", ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}