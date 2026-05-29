package com.bidding.controller.bidder;

import com.bidding.model.AuctionDisplayDTO;
import com.bidding.util.DataContext;
import com.bidding.util.SceneManager;
import com.bidding.util.TimeUtils;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ItemCardController {
    @FXML private VBox itemRoot;
    @FXML private Label lblTitle;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTimeRemaining;
    @FXML private Button btnBid;
    private Timeline countdownTimeline;

    public void setAuctionData(AuctionDisplayDTO auction) {
        if (auction == null) return;

        lblTitle.setText(auction.getItemName() != null ? auction.getItemName() : "Sản phẩm không tên");
        lblCurrentPrice.setText(String.format("%,.0f ₫", auction.getCurrentPrice()));

        String status = (auction.getStatus() != null) ? auction.getStatus().trim().toUpperCase() : "";

        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        // MẶC ĐỊNH GÁN SỰ KIỆN CHUYỂN MÀN HÌNH BAN ĐẦU CHO NÚT (Tránh nút bị chết)
        btnBid.setOnAction(e -> {
            DataContext.getInstance().setCurrentAuction(auction);
            SceneManager.switchToProductDetail();
        });

        if ("RUNNING".equals(status)) {
            lblTimeRemaining.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            btnBid.setText("Đặt giá nhanh");
            btnBid.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand;");

            countdownTimeline = new Timeline(new KeyFrame(Duration.millis(1000), event -> {
                try {
                    String remainingTime = TimeUtils.calculateRemainingTime(auction.getEndTime());
                    lblTimeRemaining.setText(remainingTime);

                    if ("00:00:00".equals(remainingTime) || remainingTime == null || remainingTime.startsWith("-")) {
                        lblTimeRemaining.setText("Phiên đã đóng");
                        lblTimeRemaining.setStyle("-fx-font-size: 12px; -fx-font-style: italic; -fx-text-fill: #7f8c8d;");

                        btnBid.setText("Thông tin");
                        btnBid.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand;");

                        countdownTimeline.stop(); // Dừng bộ đếm an toàn
                    }
                } catch (Exception ex) {
                    System.err.println("Lỗi thực thi trong Timeline Card: " + ex.getMessage());
                    if (countdownTimeline != null) countdownTimeline.stop();
                }
            }));
            countdownTimeline.setCycleCount(Animation.INDEFINITE);
            countdownTimeline.play();

        } else if ("OPEN".equals(status)) {
            lblTimeRemaining.setText("Bắt đầu: " + auction.getEndTime());
            lblTimeRemaining.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2ecc71;");
            btnBid.setText("Thông tin");
            btnBid.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand;");

        } else {
            lblTimeRemaining.setText("Phiên đã đóng");
            lblTimeRemaining.setStyle("-fx-font-size: 12px; -fx-font-style: italic; -fx-text-fill: #7f8c8d;");
            btnBid.setText("Thông tin");
            btnBid.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand;");
        }

        itemRoot.setOnMouseClicked(event -> {
            DataContext.getInstance().setCurrentAuction(auction);
            SceneManager.switchToProductDetail();
        });
    }
}