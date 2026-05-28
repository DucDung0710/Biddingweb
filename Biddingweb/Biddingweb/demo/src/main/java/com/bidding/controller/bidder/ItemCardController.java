package com.bidding.controller.bidder;

import com.bidding.model.AuctionDisplayDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ItemCardController {
    @FXML private Label lblTitle;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTimeRemaining;
    @FXML private Button btnBid;

    public void setAuctionData(AuctionDisplayDTO auction) {
        // Đổ dữ liệu thô vào các thành phần View của thẻ
        lblTitle.setText(auction.getItemName());
        lblCurrentPrice.setText(String.format("%,.0f ₫", auction.getCurrentPrice()));
        lblTimeRemaining.setText(auction.getEndTime());

        // Cài đặt sự kiện click nút
        btnBid.setOnAction(e -> {
            System.out.println("Ủy nhiệm xử lý phiên đấu giá ID: " + auction.getAuctionId());
            // Logic xử lý bật khung đặt giá (Bidding Popup) qua Socket ở đây
        });
    }
}