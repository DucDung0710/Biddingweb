package com.bidding.uilogin;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
}