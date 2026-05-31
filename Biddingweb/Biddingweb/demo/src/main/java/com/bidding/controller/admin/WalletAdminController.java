package com.bidding.controller.admin;

import javafx.scene.control.TableCell;
import javafx.util.Callback;

import com.bidding.util.SceneManager;
import com.bidding.util.SessionStore;
import com.bidding.service.WalletService;
import com.bidding.dao.WalletDepositRequestDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Callback;

import java.util.List;

public class WalletAdminController {

    @FXML private HBox navOverview, navUsers, navAuctions, navProducts, navNotifications, btnLogout;
    @FXML @SuppressWarnings("unused") private HBox navWallet;
    @FXML @SuppressWarnings("unused") private HBox navAuctionHistory;
    @FXML private Label lblPendingBadge;
    @FXML private Label lblStatPending, lblStatApproved, lblStatTotalApproved, lblStatRejected;
    @FXML private Button btnRefresh;
    @FXML private TableView<DepositRequestRow> tblPendingRequests;
    @FXML private TableColumn<DepositRequestRow, Integer> colReqId;
    @FXML private TableColumn<DepositRequestRow, String> colReqUser;
    @FXML private TableColumn<DepositRequestRow, String> colReqAmount;
    @FXML private TableColumn<DepositRequestRow, String> colReqTime;
    @FXML private TableColumn<DepositRequestRow, String> colReqStatus;
    @FXML private TableColumn<DepositRequestRow, String> colReqAction;

    private WalletService walletService = new WalletService();

    @FXML
    public void initialize() {
        // --- LIÊN KẾT SIDEBAR ---
        navOverview.setOnMouseClicked(e -> { SceneManager.switchToAdminDashboard(); });
        navUsers.setOnMouseClicked(e -> { SceneManager.switchToAdminUserManagement(); });
        navAuctions.setOnMouseClicked(e -> { SceneManager.switchToAdminAuctionManagement(); });
        navNotifications.setOnMouseClicked(e -> { SceneManager.switchToAdminNotifications(); });
        navProducts.setOnMouseClicked(e -> { SceneManager.switchToAdminProductManagement(); });
        navAuctionHistory.setOnMouseClicked(e -> { SceneManager.switchToAdminAuctionHistory(); });
        btnLogout.setOnMouseClicked(e -> { SceneManager.switchToLogin(); });

        // --- SETUP TABLE ---
        setupTable();

        // --- CÁC CHỨC NĂNG CHÍNH ---
        btnRefresh.setOnAction(e -> handleRefreshRequests());

        // Load dữ liệu ban đầu
        loadWalletData();
    }
    private void setupTable() {
        colReqId.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        colReqUser.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colReqAmount.setCellValueFactory(new PropertyValueFactory<>("amountFormatted"));
        colReqTime.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colReqStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Tạo nút Duyệt và Từ chối cho cột Hành động
        colReqAction.setCellFactory(new Callback<TableColumn<DepositRequestRow, String>, TableCell<DepositRequestRow, String>>() {
            @Override
            public TableCell<DepositRequestRow, String> call(final TableColumn<DepositRequestRow, String> param) {
                final TableCell<DepositRequestRow, String> cell = new TableCell<DepositRequestRow, String>() {

                    private final Button btnApprove = new Button("✓ Duyệt");
                    private final Button btnReject = new Button("✗ Từ chối");
                    private final HBox pane = new HBox(10, btnApprove, btnReject); // Khoảng cách giữa 2 nút là 10px

                    {
                        // Style cơ bản cho các nút (tùy chọn)
                        btnApprove.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-cursor: hand;");
                        btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");

                        // Sự kiện khi bấm nút Duyệt
                        btnApprove.setOnAction(event -> {
                            DepositRequestRow rowData = getTableView().getItems().get(getIndex());
                            approveRequest(rowData.getRequestId()); // Gọi hàm duyệt có sẵn
                        });

                        // Sự kiện khi bấm nút Từ chối
                        btnReject.setOnAction(event -> {
                            DepositRequestRow rowData = getTableView().getItems().get(getIndex());
                            rejectRequest(rowData.getRequestId()); // Gọi hàm từ chối có sẵn
                        });
                    }

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(pane); // Hiển thị HBox chứa 2 nút lên table cell
                        }
                    }
                };
                return cell;
            }
        });
    }

    private void loadWalletData() {
        List<WalletDepositRequestDAO.DepositRequest> pendingRequests = walletService.getPendingDepositRequests();
        
        int pending = pendingRequests.size();
        int approved = walletService.getDepositRequestsByStatus("APPROVED").size();
        int rejected = walletService.getDepositRequestsByStatus("REJECTED").size();
        
        double totalApproved = walletService.getDepositRequestsByStatus("APPROVED").stream()
                .mapToDouble(WalletDepositRequestDAO.DepositRequest::getAmount)
                .sum();

        lblPendingBadge.setText(String.valueOf(pending));
        lblStatPending.setText(String.valueOf(pending));
        lblStatApproved.setText(String.valueOf(approved));
        lblStatTotalApproved.setText(String.format("%,.0f ₫", totalApproved));
        lblStatRejected.setText(String.valueOf(rejected));

        // Load table data
        ObservableList<DepositRequestRow> tableData = FXCollections.observableArrayList();
        for (WalletDepositRequestDAO.DepositRequest req : pendingRequests) {
            tableData.add(new DepositRequestRow(req.getId(), req.getUserName(), req.getAmount(), req.getCreatedAt(), req.getStatus(), req.getId()));
        }
        tblPendingRequests.setItems(tableData);
    }

    private void handleRefreshRequests() {
        System.out.println("Đang làm mới danh sách yêu cầu nạp tiền...");
        loadWalletData();
    }

    public void approveRequest(int requestId) {
        if (walletService.approveDepositRequest(requestId)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thành công");
            alert.setContentText("Đã duyệt yêu cầu nạp tiền!");
            alert.showAndWait();
            loadWalletData();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setContentText("Không thể duyệt yêu cầu này!");
            alert.showAndWait();
        }
    }

    public void rejectRequest(int requestId) {
        if (walletService.rejectDepositRequest(requestId)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thành công");
            alert.setContentText("Đã từ chối yêu cầu nạp tiền!");
            alert.showAndWait();
            loadWalletData();
        }
    }

    public static class DepositRequestRow {
        private int requestId;
        private String userName;
        private double amount;
        private String createdAt;
        private String status;
        private int id;

        public DepositRequestRow(int requestId, String userName, double amount, String createdAt, String status, int id) {
            this.requestId = requestId;
            this.userName = userName;
            this.amount = amount;
            this.createdAt = createdAt;
            this.status = status;
            this.id = id;
        }

        public int getRequestId() { return requestId; }
        public String getUserName() { return userName; }
        public String getAmountFormatted() { return String.format("%,.0f ₫", amount); }
        public String getCreatedAt() { return createdAt; }
        public String getStatus() { return status; }
    }
}