package com.bidding.util;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {
    private static Stage stage;

    public static void setStage(Stage stage) {
        SceneManager.stage = stage;
    }

    /**
     * Hàm trợ giúp (Helper) dùng chung để tránh lặp code cho mỗi lần chuyển màn hình
     */
    private static void navigate(String fxmlPath) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
        Scene scene = new Scene(fxmlLoader.load());
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.setScene(scene);
    }

    // --- MÀN HÌNH CHUNG ---

    public static void switchToLogin() throws IOException {
        navigate("/uilogin.view/login.fxml");
    }

    public static void switchToSignUp() throws IOException {
        navigate("/uilogin.view/sign-up.fxml");
    }

    // --- MÀN HÌNH BIDDER (NGƯỜI MUA) ---

    public static void switchToDashboard() throws IOException {
        navigate("/bidder.view/dashboard.fxml");
    }

    // --- CÁC MÀN HÌNH ADMIN ---

    public static void switchToAdminDashboard() throws IOException {
        navigate("/admin.view/admin_dashboard.fxml");
    }

    public static void switchToAdminUserManagement() throws IOException {
        navigate("/admin.view/admin_user_management.fxml");
    }

    public static void switchToAdminAuctionManagement() throws IOException {
        navigate("/admin.view/admin_auction_management.fxml");
    }

    public static void switchToAdminWalletManagement() throws IOException {
        navigate("/admin.view/wallet_admin.fxml");
    }

    public static void switchToAdminProductManagement() throws IOException {
        navigate("/admin.view/admin_product_management.fxml");
    }

    public static void switchToAdminNotifications() throws IOException {
        navigate("/admin.view/admin_notification_management.fxml");
    }

    public static void switchToAdminAuctionHistory() throws IOException {
        navigate("/admin.view/admin_auction_history.fxml");
    }

    // --- CÁC MÀN HÌNH SELLER (NGƯỜI BÁN) ---

    public static void switchToSellerDashboard() throws IOException {
        navigate("/seller.view/seller_dashboard.fxml");
    }

    /**
     * BỔ SUNG: Chuyển đến màn hình Quản lý sản phẩm dành cho Seller
     * Bạn nhớ kiểm tra lại tên thư mục và file fxml thực tế của bạn (ví dụ: seller_product_management.fxml)
     */
    public static void switchToSellerProductManagement() throws IOException {
        navigate("/seller.view/seller_product_management.fxml");
    }
}