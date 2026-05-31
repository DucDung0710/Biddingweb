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
    @FXML private Label lblPendingBadge;

    @FXML private TextField txtSearchAuction;
    @FXML private ComboBox<String> cmbAucStatus;
    @FXML private ComboBox<String> cmbAucType;
    @FXML @SuppressWarnings("unused") private DatePicker dpFilter;

    // ===== TABLE COLUMNS =====
    @FXML private TableView<AuctionDisplayDTO> tblAuctions;
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
        setupTableColumns();

        navOverview.setOnMouseClicked(e -> SceneManager.switchToAdminDashboard());
        navProducts.setOnMouseClicked(e -> SceneManager.switchToAdminProductManagement());
        navUsers.setOnMouseClicked(e -> SceneManager.switchToAdminUserManagement());
        navWallet.setOnMouseClicked(e -> SceneManager.switchToAdminWalletManagement());
        navAuctionHistory.setOnMouseClicked(e -> SceneManager.switchToAdminAuctionHistory());
        navNotifications.setOnMouseClicked(e -> SceneManager.switchToAdminNotifications());
        navAuctions.setOnMouseClicked(e -> SceneManager.switchToAdminAuctionManagement());
        btnLogout.setOnMouseClicked(e -> SceneManager.switchToLogin());

        cmbAucStatus.getItems().addAll("Tất cả trạng thái", "Đang diễn ra", "Sắp bắt đầu", "Đã kết thúc", "SUSPENDED");
        cmbAucType.getItems().addAll("Tất cả loại", "Đấu giá truyền thống", "Đấu giá xu");

        cmbAucStatus.setValue("Tất cả trạng thái");
        cmbAucType.setValue("Tất cả loại");

        if (lblPendingBadge != null) {
            lblPendingBadge.setText("3");
        }

        loadAuctions(null, "Tất cả trạng thái", "Tất cả loại");
    }

    private void setupTableColumns() {
        colAucProduct.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colAucSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        colAucPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colAucBids.setCellValueFactory(new PropertyValueFactory<>("winnerId"));
        colAucEnd.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colAucStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAucActions.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
    }

    private void loadAuctions(String keyword, String status, String type) {
        try {
            String dbStatus = normalizeStatus(status);
            String dbType = normalizeType(type);

            List<AuctionDisplayDTO> auctions = auctionDAO.getAuctionsByFilter(keyword, dbStatus, dbType);
            ObservableList<AuctionDisplayDTO> data = FXCollections.observableArrayList(auctions);
            tblAuctions.setItems(data);
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
            List<AuctionDisplayDTO> all = auctionDAO.getAuctionsByFilter(null, "Tất cả", "Tất cả");
            if (lblTotalAuctions != null) {
                lblTotalAuctions.setText(String.valueOf(all.size()));
            }

            List<AuctionDisplayDTO> running = auctionDAO.getAuctionsByFilter(null, "Đang diễn ra", "Tất cả");
            if (lblRunningCount != null) {
                lblRunningCount.setText(String.valueOf(running.size()));
            }

            List<AuctionDisplayDTO> open = auctionDAO.getAuctionsByFilter(null, "Sắp bắt đầu", "Tất cả");
            if (lblOpenCount != null) {
                lblOpenCount.setText(String.valueOf(open.size()));
            }

            if (lblIssueCount != null) {
                lblIssueCount.setText("3");
            }
        } catch (Exception e) {
            System.err.println("Error updating statistics: " + e.getMessage());
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.equals("Tất cả trạng thái") || status.equals("Tất cả")) {
            return "Tất cả";
        }
        switch (status) {
            case "Đang diễn ra": return "RUNNING";
            case "Sắp bắt đầu": return "OPEN";
            case "Đã kết thúc": return "FINISHED";
            default: return status;
        }
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
        loadAuctions(keyword.isEmpty() ? null : keyword, status, type);
    }
}
