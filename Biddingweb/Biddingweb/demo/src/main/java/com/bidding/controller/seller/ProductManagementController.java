package com.bidding.controller.seller;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.bidding.shared.Item;
import com.bidding.shared.UserSession;
import com.bidding.shared.Users;
import com.bidding.util.SocketClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ProductManagementController implements Initializable {

    @FXML
    private HBox navOverview;

    @FXML
    private HBox btnNavAddProduct;

    @FXML
    private HBox navTransactionHistory;

    @FXML
    private HBox navNotification;

    @FXML
    private HBox btnLogout;

    @FXML
    private VBox tabAll;

    @FXML
    private VBox tabActive;

    @FXML
    private VBox tabUpcoming;

    @FXML
    private VBox tabEnded;

    @FXML
    private Button btnAddNewProduct;

    @FXML
    private Label lblProductCount;

    @FXML
    private VBox productCardsContainer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (navOverview != null) {
            navOverview.setOnMouseClicked(e -> handleNavOverview());
        }
        if (btnNavAddProduct != null) {
            btnNavAddProduct.setOnMouseClicked(e -> handleAddNewProduct(null));
        }
        if (navTransactionHistory != null) {
            navTransactionHistory.setOnMouseClicked(e -> com.bidding.util.SceneManager.switchToSellerTransactionHistory());
        }
        if (navNotification != null) {
            navNotification.setOnMouseClicked(e -> com.bidding.util.SceneManager.switchToSellerNotifications());
        }
        if (tabAll != null) {
            tabAll.setOnMouseClicked(e -> activateTab("ALL"));
        }
        if (tabActive != null) {
            tabActive.setOnMouseClicked(e -> activateTab("ACTIVE"));
        }
        if (tabUpcoming != null) {
            tabUpcoming.setOnMouseClicked(e -> activateTab("UPCOMING"));
        }
        if (tabEnded != null) {
            tabEnded.setOnMouseClicked(e -> activateTab("ENDED"));
        }
        if (btnLogout != null) {
            btnLogout.setOnMouseClicked(e -> handleLogout());
        }
        activateTab("ALL");
        loadSellerProducts();
    }

    private void loadSellerProducts() {
        if (productCardsContainer == null) {
            return;
        }

        productCardsContainer.getChildren().clear();
        if (lblProductCount != null) {
            lblProductCount.setText("Đang tải...");
        }

        try {
            Users currentUser = UserSession.getInstance().getLoggedInUser();
            if (currentUser == null) {
                if (lblProductCount != null) {
                    lblProductCount.setText("0 sản phẩm");
                }
                return;
            }

            JsonObject request = new JsonObject();
            request.addProperty("action", "GET_ALL_ITEMS");

            JsonObject response = SocketClient.getInstance().sendRequest(request);
            List<Item> items = new ArrayList<>();
            if (response != null && response.has("status") && "OK".equals(response.get("status").getAsString())) {
                JsonArray array = response.getAsJsonArray("data");
                for (JsonElement elem : array) {
                    JsonObject obj = elem.getAsJsonObject();
                    if (obj.has("userId") && obj.get("userId").getAsInt() != currentUser.getId()) {
                        continue;
                    }
                    Item item = new Item();
                    item.setItemId(obj.has("itemId") ? obj.get("itemId").getAsInt() : 0);
                    item.setUserId(obj.has("userId") ? obj.get("userId").getAsInt() : 0);
                    item.setItemName(obj.has("itemName") ? obj.get("itemName").getAsString() : "");
                    item.setType(obj.has("type") ? obj.get("type").getAsString() : "Khác");
                    item.setDescription(obj.has("description") ? obj.get("description").getAsString() : "");
                    item.setStatus(obj.has("status") ? obj.get("status").getAsString() : Item.STATUS_PENDING);
                    if (obj.has("firstprice")) {
                        item.setFirstprice(java.math.BigDecimal.valueOf(obj.get("firstprice").getAsDouble()));
                    }
                    items.add(item);
                }
            }

            for (Item item : items) {
                productCardsContainer.getChildren().add(createProductCard(item));
            }

            if (lblProductCount != null) {
                lblProductCount.setText(items.size() + " sản phẩm");
            }

            if (items.isEmpty() && lblProductCount != null) {
                lblProductCount.setText("Không có sản phẩm nào");
            }
        } catch (Exception e) {
            System.err.println("Lỗi tải sản phẩm seller: " + e.getMessage());
            e.printStackTrace();
            if (lblProductCount != null) {
                lblProductCount.setText("0 sản phẩm");
            }
        }
    }

    private HBox createProductCard(Item item) {
        HBox card = new HBox();
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setSpacing(16);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-padding: 16;");

        VBox imageBox = new VBox();
        imageBox.setAlignment(javafx.geometry.Pos.CENTER);
        imageBox.setMinSize(80, 80);
        imageBox.setMaxSize(80, 80);
        imageBox.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 6; -fx-padding: 10;");
        Label imgLabel = new Label("[Ảnh]");
        imgLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        imageBox.getChildren().add(imgLabel);

        VBox details = new VBox();
        details.setSpacing(6);
        details.setStyle("-fx-padding: 0 0 0 0;");
        details.setMaxWidth(Double.MAX_VALUE);

        HBox titleRow = new HBox();
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        titleRow.setSpacing(8);
        Label title = new Label(item.getItemName());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label status = new Label(item.getStatus() != null ? item.getStatus() : "Đang chờ");
        status.setStyle("-fx-background-color: #e6f4ea; -fx-text-fill: #0b5ed7; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 12;");
        titleRow.getChildren().addAll(title, status);

        Label description = new Label(item.getDescription() != null ? item.getDescription() : "Không có mô tả");
        description.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        description.setWrapText(true);
        description.setMaxWidth(520);

        HBox statsRow = new HBox();
        statsRow.setSpacing(24);

        VBox priceBox = new VBox();
        priceBox.setSpacing(2);
        Label startLabel = new Label("GIÁ KHỞI ĐIỂM");
        startLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        Label startValue = new Label(item.getFirstprice() != null ? item.getFirstprice().toString() : "0");
        startValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        priceBox.getChildren().addAll(startLabel, startValue);

        VBox typeBox = new VBox();
        typeBox.setSpacing(2);
        Label typeLabel = new Label("LOẠI");
        typeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        Label typeValue = new Label(item.getType() != null ? item.getType() : "Khác");
        typeValue.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569; -fx-font-weight: bold;");
        typeBox.getChildren().addAll(typeLabel, typeValue);

        statsRow.getChildren().addAll(priceBox, typeBox);

        details.getChildren().addAll(titleRow, description, statsRow);

        HBox actions = new HBox();
        actions.setAlignment(javafx.geometry.Pos.CENTER);
        actions.setSpacing(6);
        Button btnView = createActionButton("Xem");
        Button btnEdit = createActionButton("Sửa");
        Button btnDelete = createActionButton("Xóa");
        btnView.setOnAction(e -> showInfo("Xem sản phẩm", item.getItemName()));
        btnEdit.setOnAction(e -> showInfo("Sửa sản phẩm", "Chức năng sửa sản phẩm đang được phát triển."));
        btnDelete.setOnAction(e -> showInfo("Xóa sản phẩm", "Chức năng xóa sản phẩm đang được phát triển."));
        actions.getChildren().addAll(btnView, btnEdit, btnDelete);

        card.getChildren().addAll(imageBox, details, actions);
        HBox.setHgrow(details, javafx.scene.layout.Priority.ALWAYS);
        return card;
    }

    private Button createActionButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 12; -fx-cursor: hand;");
        return button;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void activateTab(String tab) {
        String activeStyle = "-fx-padding: 0 0 8 0; -fx-border-color: #0b5ed7; -fx-border-width: 0 0 2 0; -fx-cursor: hand;";
        String inactiveStyle = "-fx-padding: 0 0 8 0; -fx-border-color: transparent; -fx-cursor: hand;";

        if (tabAll != null) tabAll.setStyle(activeStyle);
        if (tabActive != null) tabActive.setStyle(inactiveStyle);
        if (tabUpcoming != null) tabUpcoming.setStyle(inactiveStyle);
        if (tabEnded != null) tabEnded.setStyle(inactiveStyle);

        switch (tab) {
            case "ACTIVE" -> {
                if (tabActive != null) tabActive.setStyle(activeStyle);
            }
            case "UPCOMING" -> {
                if (tabUpcoming != null) tabUpcoming.setStyle(activeStyle);
            }
            case "ENDED" -> {
                if (tabEnded != null) tabEnded.setStyle(activeStyle);
            }
        }
    }

    @FXML
    private void handleProductCardAction(ActionEvent event) {
        if (!(event.getSource() instanceof Button button)) {
            return;
        }
        String action = button.getText();
        String title = "Tương tác sản phẩm";
        String message = switch (action) {
            case "Xem" -> "Mở xem chi tiết sản phẩm...";
            case "Sửa" -> "Mở form sửa sản phẩm...";
            case "Xóa" -> "Xóa sản phẩm (giả lập)...";
            default -> "Đã thực hiện hành động: " + action;
        };

        if ("Xóa".equals(action)) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Bạn có chắc muốn xóa sản phẩm này?", ButtonType.YES, ButtonType.NO);
            confirm.setTitle(title);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    Alert info = new Alert(Alert.AlertType.INFORMATION, "Đã xóa sản phẩm (giả lập).", ButtonType.OK);
                    info.setTitle(title);
                    info.setHeaderText(null);
                    info.showAndWait();
                }
            });
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML
    private void handleAddNewProduct(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/seller.view/product_form.fxml"));
            Parent root = loader.load();

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Đăng sản phẩm mới");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (Exception e) {
            System.err.println("Lỗi mở form đăng sản phẩm mới: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleNavOverview() {
        // Điều hướng thực sự tới Seller Dashboard
        com.bidding.util.SceneManager.switchToSellerDashboard();
    }

    private void handleLogout() {
        com.bidding.shared.UserSession.getInstance().logout();
        com.bidding.util.SceneManager.switchToLogin();
    }
}
