package com.bidding.controller.seller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class NotificationController implements Initializable {

    @FXML
    private Label lblUnreadCount;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (lblUnreadCount != null) {
            lblUnreadCount.setText("0 chưa đọc");
        }
    }

    @FXML
    private void handleMarkAllRead() {
        System.out.println("Mark all notifications as read");
        if (lblUnreadCount != null) {
            lblUnreadCount.setText("0 chưa đọc");
        }
    }

    @FXML
    private void handleTabAll() {
        System.out.println("Notification tab all selected");
    }

    @FXML
    private void handleTabUnread() {
        System.out.println("Notification tab unread selected");
    }

    @FXML
    private void handleTabBid() {
        System.out.println("Notification tab bid selected");
    }

    @FXML
    private void handleTabSystem() {
        System.out.println("Notification tab system selected");
    }
}
