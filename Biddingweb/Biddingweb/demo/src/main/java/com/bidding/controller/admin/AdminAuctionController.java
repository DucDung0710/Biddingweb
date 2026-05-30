package com.bidding.controller.admin;

import com.bidding.dao.JdbcAuctionDAO;
import com.bidding.model.AuctionDisplayDTO;
import com.bidding.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import java.util.List;

public class AdminAuctionController {

    @FXML private HBox navOverview, navUsers, navProducts, navAuctions, navWallet, navNotifications, navAuctionHistory, btnLogout;
    @FXML private Label lblPendingBadge; // Badge thông báo số lượng đơn ví chờ duyệt

    @FXML private TextField txtSearchAuction; // [cite: 88]
    @FXML private ComboBox<String> cmbAucStatus; // [cite: 89]
    @FXML private ComboBox<String> cmbAucType; // [cite: 90]
    @FXML @SuppressWarnings("unused") private DatePicker dpFilter; // [cite: 91]
    
    // ===== TABLE COLUMNS =====
    @FXML private TableView<AuctionDisplayDTO> tblAuctions; // [cite: 94]
    @FXML private TableColumn<AuctionDisplayDTO, String> colAucProduct;
    @FXML private TableColumn<AuctionDisplayDTO, String> colAucSeller;
    @FXML private TableColumn<AuctionDisplayDTO, Double> colAucPrice;
    @FXML private TableColumn<AuctionDisplayDTO, Integer> colAucBids;
    @FXML private TableColumn<AuctionDisplayDTO, String> colAucEnd;
    @FXML private TableColumn<AuctionDisplayDTO, String> colAucStatus;
    @FXML private TableColumn<AuctionDisplayDTO, String> colAucActions;
    
    // ===== STATISTICS LABELS =====
    @FXML private Label lblTotalAuctions;
    @FXML private Label lblRunningCount;
    @FXML private Label lblOpenCount;
    @FXML private Label lblIssueCount;
    @FXML private Label lblAucPageInfo;
    @FXML private Pagination aucPagination;
    
    private final JdbcAuctionDAO auctionDAO = new JdbcAuctionDAO();

    @FXML
    public void initialize() {
        // --- SETUP TABLE COLUMNS ---
        setupTableColumns();
        
        // --- LIÊN KẾT SIDEBAR ---
        navOverview.setOnMouseClicked(e -> {SceneManager.switchToAdminDashboard();});
        navProducts.setOnMouseClicked(e -> {SceneManager.switchToAdminProductManagement();});
        navUsers.setOnMouseClicked(e ->{SceneManager.switchToAdminUserManagement();});
        navWallet.setOnMouseClicked(e -> {SceneManager.switchToAdminWalletManagement();});
        navAuctionHistory.setOnMouseClicked(e -> {SceneManager.switchToAdminAuctionHistory();});
        navNotifications.setOnMouseClicked(e -> {SceneManager.switchToAdminNotifications();});
        navAuctions.setOnMouseClicked(e -> {SceneManager.switchToAdminAuctionManagement();});
        btnLogout.setOnMouseClicked(e -> {SceneManager.switchToLogin();});

        // --- CÀI ĐẶT BỘ LỌC ---
        cmbAucStatus.getItems().addAll("Tất cả trạng thái", "Đang diễn ra", "Sắp bắt đầu", "Đã kết thúc", "SUSPENDED");
        cmbAucType.getItems().addAll("Tất cả loại", "Đấu giá truyền thống", "Đấu giá xu");
        
        // Set default selection
        cmbAucStatus.setValue("Tất cả trạng thái");
        cmbAucType.setValue("Tất cả loại");

        // Đổ số liệu mẫu cho Badge ví tiền trên Sidebar
        if (lblPendingBadge != null) {
            lblPendingBadge.setText("3");
        }
        
        // --- LOAD INITIAL DATA ---
        loadAuctions(null, "Tất cả trạng thái", "Tất cả loại");
    }
    
    private void setupTableColumns() {
        // Cấu hình các cột của bảng
        colAucProduct.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colAucSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        colAucPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colAucBids.setCellValueFactory(new PropertyValueFactory<>("winnerId"));
        colAucEnd.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colAucStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Các hành động sẽ được cấu hình riêng (xem, sửa, xóa)
        colAucActions.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
    }
    
    private void loadAuctions(String keyword, String status, String type) {
        try {
            // Normalize UI values to DB values
            String dbStatus = normalizeStatus(status);
            String dbType = normalizeType(type);
            
            // Fetch from DAO
            List<AuctionDisplayDTO> auctions = auctionDAO.getAuctionsByFilter(keyword, dbStatus, dbType);
            
            // Create ObservableList for TableView
            ObservableList<AuctionDisplayDTO> data = FXCollections.observableArrayList(auctions);
            tblAuctions.setItems(data);
            
            // Update statistics labels
            updateStatistics();
            
            System.out.println("✓ Đã tải " + auctions.size() + " phiên đấu giá");
        } catch (Exception e) {
            showError("Lỗi tải dữ liệu: " + e.getMessage());
            System.err.println("Error loading auctions: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateStatistics() {
        try {
            // Count all auctions
            List<AuctionDisplayDTO> all = auctionDAO.getAuctionsByFilter(null, "Tất cả", "Tất cả");
            if (lblTotalAuctions != null) {
                lblTotalAuctions.setText(String.valueOf(all.size()));
            }
            
            // Count by status
            List<AuctionDisplayDTO> running = auctionDAO.getAuctionsByFilter(null, "RUNNING", "Tất cả");
            if (lblRunningCount != null) {
                lblRunningCount.setText(String.valueOf(running.size()));
            }
            
            List<AuctionDisplayDTO> open = auctionDAO.getAuctionsByFilter(null, "OPEN", "Tất cả");
            if (lblOpenCount != null) {
                lblOpenCount.setText(String.valueOf(open.size()));
            }
            
            // Count issues (auctions without winner or with errors)
            if (lblIssueCount != null) {
                lblIssueCount.setText("3"); // Placeholder
            }
        } catch (Exception e) {
            System.err.println("Error updating statistics: " + e.getMessage());
        }
    }
    
    private String normalizeStatus(String status) {
        if (status == null || status.equals("Tất cả trạng thái") || status.equals("Tất cả")) {
            return "Tất cả";
        }
        return switch (status) {
            case "Đang diễn ra" -> "RUNNING";
            case "Sắp bắt đầu" -> "OPEN";
            case "Đã kết thúc" -> "FINISHED";
            default -> status;
        };
    }
    
    private String normalizeType(String type) {
        if (type == null || type.equals("Tất cả loại") || type.equals("Tất cả")) {
            return "Tất cả";
        }
        return type;
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText("Không thể tải dữ liệu");
        alert.setContentText(message);
        alert.showAndWait();
    }

    
    @FXML
    @SuppressWarnings("unused")
    private void handleFilterAuctions() {
        String keyword = txtSearchAuction.getText().trim();
        String status = cmbAucStatus.getValue() != null ? cmbAucStatus.getValue() : "Tất cả trạng thái";
        String type = cmbAucType.getValue() != null ? cmbAucType.getValue() : "Tất cả loại";
        
        System.out.println("[Search] Keyword: " + keyword + ", Status: " + status + ", Type: " + type);
        
        // Load auctions with filter
        loadAuctions(keyword.isEmpty() ? null : keyword, status, type);
    }
}