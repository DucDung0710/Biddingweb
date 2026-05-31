package com.bidding.controller.admin;

import java.util.ArrayList;
import java.util.List;

import com.bidding.shared.Item;
import com.bidding.shared.UserSession;
import com.bidding.shared.Users;
import com.bidding.util.SceneManager;
import com.bidding.util.SocketClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

public class AdminProductController {
    @FXML private HBox navOverview, navUsers, navAuctions, navWallet, navProducts, navAuctionHistory, navNotifications, btnLogout;

    @FXML private Label lblPendingBadge;
    @FXML private Label lblStatPendingProd, lblStatActiveProd;
    @FXML private TextField txtSearchProduct;
    @FXML private TableView<Item> tblProducts;

    @FXML private TableColumn<Item, String> colProdName;
    @FXML private TableColumn<Item, String> colProdCategory;
    @FXML private TableColumn<Item, String> colProdStatus;
    @FXML private TableColumn<Item, Double> colProdPrice;
    @FXML private TableColumn<Item, Void> colProdAction;

    private final ObservableList<Item> masterData = FXCollections.observableArrayList();

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


        // 1. Khởi tạo cấu trúc các cột dữ liệu
        colProdName.setCellValueFactory(new PropertyValueFactory<>("ItemName"));
        colProdCategory.setCellValueFactory(new PropertyValueFactory<>("description"));
        colProdPrice.setCellValueFactory(new PropertyValueFactory<>("firstprice"));
        colProdStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // 2. Cấu hình cột nút chức năng duyệt/từ chối
        addButtonToTable();

        // 3. Tải dữ liệu thật từ Server lên
        refreshTableAndStats();

        // 4. Cấu hình bộ lọc tìm kiếm thời gian thực (Search filter)
        FilteredList<Item> filteredData = new FilteredList<>(masterData, p -> true);
        txtSearchProduct.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();

                if (item.getItemName() != null && item.getItemName().toLowerCase().contains(lowerCaseFilter)) return true;
                return item.getDescription() != null && item.getDescription().toLowerCase().contains(lowerCaseFilter);
            });
        });
        tblProducts.setItems(filteredData);
    }

    private void refreshTableAndStats() {
        // Gửi request lấy dữ liệu thật qua Socket mạng
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_ALL_ITEMS");

        JsonObject response = SocketClient.getInstance().sendRequest(request);

        if (response != null && "OK".equals(response.get("status").getAsString())) {
            JsonArray array = response.getAsJsonArray("data");
            List<Item> serverItems = new ArrayList<>();

            int pendingCount = 0;
            int activeCount = 0;

            for (JsonElement elem : array) {
                JsonObject obj = elem.getAsJsonObject();
                Item item = new Item();
                item.setItemId(obj.get("itemId").getAsInt());
                item.setUserId(obj.get("userId").getAsInt());
                item.setItemName(obj.get("itemName").getAsString());
                item.setType(obj.get("type").getAsString());
                item.setDescription(obj.get("description").getAsString());
                item.setStatus(obj.get("status").getAsString());
                item.setFirstprice(java.math.BigDecimal.valueOf(obj.get("firstprice").getAsDouble()));

                serverItems.add(item);

                // Tính toán nhanh số liệu thống kê badge
                if (item.getStatus().equalsIgnoreCase("Pending")) pendingCount++;
                else if (item.getStatus().equalsIgnoreCase("Approved")) activeCount++;
            }

            // Đổ dữ liệu mới vào TableView
            masterData.setAll(serverItems);

            // Cập nhật giao diện số liệu
            if (lblStatPendingProd != null) lblStatPendingProd.setText(String.valueOf(pendingCount));
            if (lblStatActiveProd != null) lblStatActiveProd.setText(String.valueOf(activeCount));
            if (lblPendingBadge != null) {
                lblPendingBadge.setText(String.valueOf(pendingCount));
                lblPendingBadge.setVisible(pendingCount > 0);
            }
        } else {
            System.err.println("Không thể lấy danh sách sản phẩm từ Server");
        }
    }

    private void addButtonToTable() {
        colProdAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnApprove = new Button("Duyệt");
            private final Button btnReject = new Button("Từ chối");
            private final HBox container = new HBox(8, btnApprove, btnReject);

            {
                btnApprove.setStyle("-fx-background-color: #2ec4b6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");
                btnReject.setStyle("-fx-background-color: #e71d36; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");
                container.setAlignment(Pos.CENTER);

                btnApprove.setOnAction(event -> {
                    Item currentItem = getTableView().getItems().get(getIndex());
                    sendReviewRequest(currentItem.getItemId(), true);
                });

                btnReject.setOnAction(event -> {
                    Item currentItem = getTableView().getItems().get(getIndex());
                    sendReviewRequest(currentItem.getItemId(), false);
                });
            }

            private void sendReviewRequest(int itemId, boolean isApproved) {
                Users activeAdmin = UserSession.getInstance().getLoggedInUser();
                if (activeAdmin == null) {
                    System.err.println("Lỗi: Không tìm thấy phiên đăng nhập của Admin.");
                    return;
                }

                // Đóng gói lệnh gửi lên Server duyệt dữ liệu SQLite thật
                JsonObject reviewReq = new JsonObject();
                reviewReq.addProperty("action", "REVIEW_ITEM");
                reviewReq.addProperty("itemId", itemId);
                reviewReq.addProperty("approved", isApproved);
                reviewReq.addProperty("adminId", activeAdmin.getId());

                JsonObject response = SocketClient.getInstance().sendRequest(reviewReq);
                if (response != null && "OK".equals(response.get("status").getAsString())) {
                    refreshTableAndStats(); // Duyệt xong load lại bảng ngay lập tức
                } else {
                    System.err.println("Lỗi xử lý duyệt sản phẩm từ Server");
                }
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Item currentItem = getTableView().getItems().get(getIndex());
                    // Chỉ hiện nút duyệt nếu sản phẩm đang ở trạng thái chờ duyệt (Pending)
                    if (currentItem != null && "Pending".equalsIgnoreCase(currentItem.getStatus())) {
                        setGraphic(container);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }
}