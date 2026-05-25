package com.bidding.util;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;

public class SceneManager {
    private static Stage stage;

    public static void setStage(Stage stage) {
        SceneManager.stage = stage;
    }

    /**
     * Hàm trợ giúp (Helper) dùng chung để chuyển màn hình.
     * Đã được bọc try-catch để các hàm gọi không cần viết 'throws IOException' chuẩn OOP & Clean Code.
     */
    private static void navigate(String fxmlPath) {
        try {
            if (stage == null) {
                System.err.println("Lỗi: Stage chưa được thiết lập trong SceneManager!");
                return;
            }
            FXMLLoader fxmlLoader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = fxmlLoader.load();
            Scene scene = stage.getScene();

            if (scene == null) {
                scene = new Scene(root);
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            stage.sizeToScene();
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("Không thể tải hoặc tìm thấy file FXML tại đường dẫn: " + fxmlPath);
            e.printStackTrace();
        }
    }

    // ==================== --- MÀN HÌNH CHUNG --- ====================

    public static void switchToLogin() {
        navigate("/uilogin.view/login.fxml");
    }

    public static void switchToSignUp() {
        navigate("/uilogin.view/sign-up.fxml");
    }

    // ==================== --- MÀN HÌNH BIDDER (NGƯỜI MUA) --- ====================

    /**
     * Chuyển đến màn hình Tổng quan của Bidder
     */
    public static void switchToDashboard() {
        navigate("/bidder.view/dashboard.fxml");
    }

    /**
     * Chuyển đến màn hình Danh sách phiên đấu giá
     */
    public static void switchToAuctionList() {
        navigate("/bidder.view/auction_list.fxml");
    }

    /**
     * Chuyển đến màn hình Chi tiết sản phẩm trước khi đấu giá
     */
    public static void switchToProductDetail() {
        navigate("/bidder.view/product_detail.fxml");
    }

    /**
     * Chuyển đến phòng Đấu giá trực tiếp Realtime
     */
    public static void switchToRealtimeBidding() {
        navigate("/bidder.view/realtime.bidding.fxml");
    }

    /**
     * Chuyển đến màn hình Lịch sử đấu giá của Bidder
     */
    public static void switchToAuctionHistory() {
        navigate("/bidder.view/auction_history.fxml");
    }

    /**
     *Chuyển đến màn hình Ví tiền cá nhân của Bidder
     */
    public static void switchToBidderWallet() {
        navigate("/bidder.view/wallet_bidder.fxml");
    }

    // ==================== --- CÁC MÀN HÌNH ADMIN --- ====================

    public static void switchToAdminDashboard() {
        navigate("/admin.view/admin_dashboard.fxml");
    }

    public static void switchToAdminUserManagement() {
        navigate("/admin.view/admin_user_management.fxml");
    }

    public static void switchToAdminAuctionManagement() {
        navigate("/admin.view/admin_auction_management.fxml");
    }

    public static void switchToAdminWalletManagement() {
        navigate("/admin.view/wallet_admin.fxml");
    }

    public static void switchToAdminProductManagement() {
        navigate("/admin.view/admin_product_management.fxml");
    }

    public static void switchToAdminNotifications() {
        navigate("/admin.view/admin_notification_management.fxml");
    }

    public static void switchToAdminAuctionHistory() {
        navigate("/admin.view/admin_auction_history.fxml");
    }

    // ==================== --- CÁC MÀN HÌNH SELLER (NGƯỜI BÁN) --- ====================

    public static void switchToSellerDashboard() {
        navigate("/seller.view/seller_dashboard.fxml");
    }

    public static void switchToSellerProductManagement() {
        navigate("/seller.view/seller_product_management.fxml");
    }
}