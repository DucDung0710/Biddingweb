package com.bidding.controller.bidder;

import com.bidding.shared.WalletManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

//import static com.bidding.util.SessionStore.currentUser;

public class DashboardController {
    // Sidebar elements
    @FXML private VBox navMenu;
    @FXML private HBox navDashboard;
    @FXML private HBox navAuctions;
    @FXML private HBox btnLogout;
    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;
    @FXML private Label lblNotifBadge;

    // Statistics
    @FXML private Label lblStatActive;
    @FXML private Label lblStatLeading;
    @FXML private Label lblStatTotal;
    @FXML private Label lblStatWon;

    // Auction cards
    @FXML private HBox auctionContainer;
    @FXML private Label lblPriceMacbook;
    @FXML private Label lblTimerMacbook;
    @FXML private Label lblPriceHonda;
    @FXML private Label lblTimerHonda;
    @FXML private Label lblPriceTranh;

    // Chart area
    @FXML private StackPane chartPlaceholder;

    @FXML
    public void initialize() {
        // Khởi tạo dashboard
    }

    @FXML
    private void handleNavWallet() {
        try {
            // 1. Load file FXML của màn hình ví
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/wallet_bidder.fxml")
            );
            Parent walletView = loader.load();

            // 2. Lấy Controller của màn hình vừa load
            //WalletBidderController walletController = loader.getController();

            // 3. Truyền dữ liệu cần thiết vào Controller
            //    (walletManager và userId phải đã có sẵn trong DashboardController)
            //walletController.setData(WalletManager.getInstance(), currentUser.getId());

            // 4. Hiển thị màn hình ví vào vùng content chính
           // mainContent.getChildren().setAll(walletView);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}