package com.bidding.controller.seller;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class NotificationController implements Initializable {

    @FXML private VBox notifContainer;
    @FXML private Label lblUnreadCount;
    @FXML private Button btnTabAll;
    @FXML private Button btnTabUnread;
    @FXML private Button btnTabBid;
    @FXML private Button btnTabSystem;

    private final List<NotificationItem> allNotifications = new ArrayList<>();
    private List<NotificationItem> currentNotifications = List.of();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadNotifications();
        updateUnreadCount();
        showTab("ALL");
    }

    private void loadNotifications() {
        allNotifications.clear();
        allNotifications.add(new NotificationItem("ALL", "09:00", "Người dùng leminh đã đặt giá trên sản phẩm của bạn."));
        allNotifications.add(new NotificationItem("BID", "08:25", "Sản phẩm của bạn đã được đặt giá mới."));
        allNotifications.add(new NotificationItem("SYSTEM", "07:10", "Hệ thống xác nhận yêu cầu nạp tiền đang xử lý."));
        currentNotifications = new ArrayList<>(allNotifications);
        renderNotifications(currentNotifications);
    }

    @FXML
    private void handleMarkAllRead() {
        allNotifications.forEach(item -> item.isRead = true);
        updateUnreadCount();
        renderNotifications(currentNotifications);
    }

    @FXML
    private void handleTabAll() {
        showTab("ALL");
    }

    @FXML
    private void handleTabUnread() {
        showTab("UNREAD");
    }

    @FXML
    private void handleTabBid() {
        showTab("BID");
    }

    @FXML
    private void handleTabSystem() {
        showTab("SYSTEM");
    }

    private void showTab(String tab) {
        switch (tab) {
            case "UNREAD" -> currentNotifications = allNotifications.stream().filter(item -> !item.isRead).collect(Collectors.toList());
            case "BID" -> currentNotifications = allNotifications.stream().filter(item -> "BID".equals(item.type)).collect(Collectors.toList());
            case "SYSTEM" -> currentNotifications = allNotifications.stream().filter(item -> "SYSTEM".equals(item.type)).collect(Collectors.toList());
            default -> currentNotifications = new ArrayList<>(allNotifications);
        }
        renderNotifications(currentNotifications);
    }

    private void updateUnreadCount() {
        long unreadCount = allNotifications.stream().filter(item -> !item.isRead).count();
        if (lblUnreadCount != null) {
            lblUnreadCount.setText(unreadCount + " chưa đọc");
        }
    }

    private void renderNotifications(List<NotificationItem> notifications) {
        notifContainer.getChildren().clear();
        if (notifications.isEmpty()) {
            Label empty = new Label("Không có thông báo nào.");
            empty.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 13px; -fx-padding: 40; -fx-alignment: center;");
            notifContainer.getChildren().add(empty);
            return;
        }
        for (NotificationItem item : notifications) {
            HBox row = new HBox();
            row.setStyle("-fx-background-color: white; -fx-border-color: #e8e8e8; -fx-border-width: 0 0 1 0; -fx-padding: 14 20; -fx-alignment: top-left;");

            VBox content = new VBox();
            Label title = new Label(item.message);
            title.setStyle("-fx-font-size: 13px; -fx-text-fill: #1a1a1a;");
            Label meta = new Label(item.time + " • " + (item.isRead ? "Đã đọc" : "Chưa đọc"));
            meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888; -fx-padding: 4 0 0 0;");

            content.getChildren().addAll(title, meta);
            row.getChildren().add(content);
            notifContainer.getChildren().add(row);
        }
    }

    private static class NotificationItem {
        private final String type;
        private final String time;
        private final String message;
        private boolean isRead;

        private NotificationItem(String type, String time, String message) {
            this.type = type;
            this.time = time;
            this.message = message;
            this.isRead = false;
        }
    }
}
