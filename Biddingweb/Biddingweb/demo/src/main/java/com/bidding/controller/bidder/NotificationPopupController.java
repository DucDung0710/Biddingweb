package com.bidding.controller.bidder;

import javafx.fxml.FXML;

public class NotificationPopupController extends BaseBidderController {

    @FXML
    public void initialize() {
        // Khởi tạo hoặc nạp danh sách thông báo từ Service/Database nếu cần
        System.out.println("Popup thông báo đã được khởi tạo.");
    }

    @FXML
    private void handleMarkAllAsRead() {
        // Logic cập nhật trạng thái đã đọc trong database hoặc hệ thống
        System.out.println("Đã đánh dấu đọc tất cả thông báo.");

    }
}