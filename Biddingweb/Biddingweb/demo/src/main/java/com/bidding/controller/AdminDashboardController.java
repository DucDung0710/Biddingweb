package com.bidding.controller;

import com.bidding.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import java.io.IOException;

/**
 * Controller for admin_dashboard.fxml
 * Provides navigation between admin panels (users, auctions, products)
 */
public class AdminDashboardController {
    @FXML private HBox navOverview;
    @FXML private HBox navUsers;
    @FXML private HBox navAuctions;
    @FXML private HBox navProducts;
    @FXML private HBox btnLogout;

    @FXML
    public void initialize() {
        setupNavigation();
    }

    private void setupNavigation() {
        navOverview.setOnMouseClicked(e -> System.out.println("Already on Dashboard"));
        navUsers.setOnMouseClicked(e -> navigateTo("users"));
        navAuctions.setOnMouseClicked(e -> navigateTo("auctions"));
        navProducts.setOnMouseClicked(e -> navigateTo("products"));
        btnLogout.setOnMouseClicked(e -> handleLogout());
    }

    private void navigateTo(String page) {
        try {
            switch (page) {
                case "users":
                    System.out.println("Navigate to User Management");
                    // SceneManager.switchToAdminUsers();
                    break;
                case "auctions":
                    System.out.println("Navigate to Auction Management");
                    // SceneManager.switchToAdminAuctions();
                    break;
                case "products":
                    System.out.println("Navigate to Product Management");
                    // SceneManager.switchToAdminProducts();
                    break;
                default:
                    System.out.println("Unknown page: " + page);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleLogout() {
        System.out.println("Admin logout clicked");
        try {
            // SceneManager.switchToLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

