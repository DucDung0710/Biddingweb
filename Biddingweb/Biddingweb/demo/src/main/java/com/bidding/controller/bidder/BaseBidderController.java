package com.bidding.controller.bidder;

import com.bidding.shared.UserSession;
import com.bidding.shared.Users;
import com.bidding.util.SceneManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.io.IOException;

public abstract class BaseBidderController {

    @FXML protected HBox navDashboard;
    @FXML protected HBox navAuctions;
    @FXML protected HBox navAuctionHistory;
    @FXML protected HBox navWallet;
    @FXML protected HBox btnLogout;
    @FXML protected Label lblUserName;
    @FXML protected Label lblUserRole;

    protected void setupSidebarBehavior() {
        if (navDashboard != null) navDashboard.setOnMouseClicked(e -> SceneManager.switchToDashboard());
        if (navAuctions != null) navAuctions.setOnMouseClicked(e -> SceneManager.switchToAuctionList());
        if (navAuctionHistory != null) navAuctionHistory.setOnMouseClicked(e -> SceneManager.switchToAuctionHistory());
        if (navWallet != null) navWallet.setOnMouseClicked(e -> SceneManager.switchToBidderWallet());
        if (btnLogout != null) btnLogout.setOnMouseClicked(e -> handleLogout());

        // Load thông tin User hiện tại từ Session đưa lên giao diện
        loadCurrentUserInfo();
    }

    private void loadCurrentUserInfo() {
        // Lấy dữ liệu từ tầng Model (Session)
        Users currentUser = UserSession.getInstance().getLoggedInUser();

        // Cập nhật hiển thị ra tầng View (Kiểm tra null phòng thủ)
        if (currentUser != null) {
            if (lblUserName != null) {
                lblUserName.setText(currentUser.getUsername());
            }
            if (lblUserRole != null) {
                lblUserRole.setText(currentUser.getRole());
            }
        } else {
            if (lblUserName != null) lblUserName.setText("Khách");
            if (lblUserRole != null) lblUserRole.setText("GUEST");
            System.err.println("Cảnh báo: Hiện tại chưa có phiên đăng nhập của người dùng!");
        }
    }

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

    protected void handleLogout() {
        UserSession.getInstance().clear();
        SceneManager.switchToLogin();
    }
}