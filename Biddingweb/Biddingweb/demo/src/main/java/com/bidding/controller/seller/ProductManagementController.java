package com.bidding.controller.seller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
