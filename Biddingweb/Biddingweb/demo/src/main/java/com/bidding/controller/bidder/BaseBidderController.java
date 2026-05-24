package com.bidding.controller.bidder;

import com.bidding.util.SceneManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public abstract class BaseBidderController {

    // Các thành phần Sidebar dùng chung được inject từ FXML
    @FXML protected HBox navDashboard;
    @FXML protected HBox navAuctions;
    @FXML protected HBox navAuctionHistory;
    @FXML protected HBox navNotifications;
    @FXML protected HBox navWallet;
    @FXML protected HBox btnLogout;
    @FXML protected Label lblUserName;
    @FXML protected Label lblUserRole;
    @FXML protected Label lblNotifBadge;

    /**
     * Hàm khởi tạo giao diện chung (Encapsulation & Template Method)
     * Được gọi ở hàm initialize() của các lớp con
     */
    protected void setupSidebarBehavior() {
        if (navDashboard != null) navDashboard.setOnMouseClicked(e -> SceneManager.switchToDashboard());
        if (navAuctions != null) navAuctions.setOnMouseClicked(e -> SceneManager.switchToAuctionList());
        if (navAuctionHistory != null) navAuctionHistory.setOnMouseClicked(e -> SceneManager.switchToAuctionHistory());
        if (navWallet != null) navWallet.setOnMouseClicked(e -> SceneManager.switchToBidderWallet());
        if (btnLogout != null) btnLogout.setOnMouseClicked(e -> SceneManager.switchToLogin());
        if (navNotifications != null) {
            navNotifications.setOnMouseClicked(e -> showNotificationPopup());
        }

        // Load thông tin User hiện tại từ Session (Giả lập hoặc từ AuthManager Singleton)
        loadCurrentUserInfo();
    }

    private void loadCurrentUserInfo() {
        // Đóng gói logic lấy dữ liệu người dùng
        if (lblUserName != null) lblUserName.setText("Nguyễn Văn A"); // Lấy từ Session class
        if (lblUserRole != null) lblUserRole.setText("Bidder");
    }

    /**
     * Phương thức chuyển đổi màn hình dùng chung (Polymorphism / Reusability)
     */
    protected void navigateTo(String fxmlPath) {
        try {
            Stage stage = (Stage) lblUserName.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi chuyển màn hình: " + fxmlPath);
            e.printStackTrace();
        }
    }
    /**
     * Logic hiển thị Popup thông báo dưới dạng một cửa sổ nhỏ (Pop-up Window)
     */
    private void showNotificationPopup() {
        try {
            // 1. Nạp file FXML của popup
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/bidder.view/notification_popup.fxml"));
            Parent root = loader.load();

            // 2. Tạo một Stage mới độc lập cho Popup
            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL); // Ngăn tương tác với màn hình chính khi đang mở popup (tùy chọn)
            popupStage.initStyle(StageStyle.UTILITY);            // Ẩn các nút thu nhỏ/phóng to của hệ điều hành
            popupStage.setTitle("Thông báo");

            // 3. Gắn giao diện vào Stage và hiển thị
            Scene scene = new Scene(root);
            popupStage.setScene(scene);

            // Định vị popup hiển thị ở giữa màn hình chính của ứng dụng
            Stage mainStage = (Stage) navNotifications.getScene().getWindow();
            popupStage.setX(mainStage.getX() + mainStage.getWidth() / 2 - 160); // 160 là một nửa chiều rộng file fxml (320)
            popupStage.setY(mainStage.getY() + 100);

            popupStage.show();

            // 4. (Tùy chọn) Khi người dùng mở xem thông báo, ẩn badge số lượng đi
            if (lblNotifBadge != null) {
                lblNotifBadge.setVisible(false);
            }

        } catch (IOException e) {
            System.err.println("Không thể mở file notification_popup.fxml. Vui lòng kiểm tra lại đường dẫn!");
            e.printStackTrace();
        }
    }

    protected void handleLogout() {
        // Xử lý xóa Session và quay lại màn hình Login
        navigateTo("/fxml/login.fxml");
    }
}