package com.bidding.controller.admin;

import com.bidding.shared.Item;
import com.bidding.shared.ItemManager;
import com.bidding.shared.Users;
import com.bidding.util.SceneManager;
import com.bidding.util.SessionStore;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import java.io.IOException;
import java.util.List;

public class AdminProductController {

    @FXML private HBox navOverview, navUsers, navAuctions, navProducts, navWallet, navAuctionHistory, navNotifications, btnLogout;
    @FXML private Label lblPendingBadge;
    @FXML private Label lblStatPendingProd, lblStatActiveProd;

    @FXML private TextField txtSearchProduct;
    @FXML private TableView<Item> tblProducts;

    @FXML private TableColumn<Item, String> colProdName;
    @FXML private TableColumn<Item, String> colProdCategory;
    @FXML private TableColumn<Item, String> colProdStatus;
    @FXML private TableColumn<Item, Double> colProdPrice;
    @FXML private TableColumn<Item, Void> colProdAction;

    private final ItemManager itemManager = new ItemManager();
    private final ObservableList<Item> tableData = FXCollections.observableArrayList();
    private FilteredList<Item> filteredData;


    @FXML
    public void initialize() {
        // 1. LIÊN KẾT SIDEBAR MENU
        navOverview.setOnMouseClicked(e -> { SceneManager.switchToAdminDashboard(); });
        navUsers.setOnMouseClicked(e -> { SceneManager.switchToAdminUserManagement(); });
        navAuctions.setOnMouseClicked(e -> { SceneManager.switchToAdminAuctionManagement(); });
        navWallet.setOnMouseClicked(e -> { SceneManager.switchToAdminWalletManagement(); });
        navProducts.setOnMouseClicked(e -> { SceneManager.switchToAdminProductManagement(); });
        navAuctionHistory.setOnMouseClicked(e -> { SceneManager.switchToAdminAuctionHistory(); });
        navNotifications.setOnMouseClicked(e -> { SceneManager.switchToAdminNotifications(); });
        btnLogout.setOnMouseClicked(e -> { SceneManager.switchToLogin(); });

        // 2. ĐỒNG BỘ CÁC CỘT
        colProdName.setCellValueFactory(new PropertyValueFactory<>("ItemName"));
        colProdStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colProdPrice.setCellValueFactory(new PropertyValueFactory<>("firstprice"));
        colProdCategory.setCellValueFactory(new PropertyValueFactory<>("description"));

        setupActionColumn();
        mockDataFromLogic();
        setupSearchFilter();

        lblPendingBadge.setText("3");
    }

    private void mockDataFromLogic() {
        itemManager.registerNewItem("PROD_001", "USER_KHANH", "iPhone 15 Pro Max", "Điện thoại Apple chính hãng", 25000000);
        itemManager.registerNewItem("PROD_002", "USER_LEMINH", "MacBook Pro M3", "Máy tính xách tay cấu hình cao", 45000000);
        itemManager.registerNewItem("PROD_003", "USER_TRAM", "Giày Jordan 1", "Thời trang Sneaker", 3500000);

        List<Item> tramItems = itemManager.getItemByUserId("USER_TRAM");
        if (tramItems != null && !tramItems.isEmpty()) {
            tramItems.get(0).setStatus("APPROVED");
        }
        refreshTableAndStats();
    }

    private void refreshTableAndStats() {
        tableData.clear();
        for (Item item : itemManager.getItemByUserId("USER_KHANH")) { /* Duyệt map mẫu */ }
        tableData.addAll(itemManager.getItemByUserId("USER_KHANH"));

        tblProducts.refresh();
    }

    private void setupSearchFilter() {
        filteredData = new FilteredList<>(tableData, p -> true);
        txtSearchProduct.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String filter = newValue.toLowerCase().trim();
                return item.getItemName().toLowerCase().contains(filter) || item.getDescription().toLowerCase().contains(filter);
            });
        });
        tblProducts.setItems(filteredData);
    }

    private void setupActionColumn() {
        colProdAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnApprove = new Button("Duyệt");
            private final Button btnReject = new Button("Từ chối");
            private final HBox container = new HBox(8, btnApprove, btnReject);

            {
                container.setAlignment(Pos.CENTER);
                btnApprove.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4;");
                btnReject.setStyle("-fx-background-color: #c62828; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4;");

                btnApprove.setOnAction(event -> {
                    Item currentItem = getTableView().getItems().get(getIndex());

                    Users activeAdmin = SessionStore.getCurrentUser();
                    if (activeAdmin != null) {
                        itemManager.reviewItem(activeAdmin, currentItem.getItemId(), true);
                        refreshTableAndStats();
                    }
                });

                btnReject.setOnAction(event -> {
                    Item currentItem = getTableView().getItems().get(getIndex());

                    // SỬA ĐÚNG: Lấy dữ liệu thật từ Session
                    Users activeAdmin = SessionStore.getCurrentUser();
                    if (activeAdmin != null) {
                        itemManager.reviewItem(activeAdmin, currentItem.getItemId(), false);
                        refreshTableAndStats();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Item currentItem = getTableView().getItems().get(getIndex());
                    if (currentItem.getStatus().equalsIgnoreCase("Pending")) {
                        setGraphic(container);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }
}