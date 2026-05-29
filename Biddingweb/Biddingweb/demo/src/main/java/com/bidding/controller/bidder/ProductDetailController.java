package com.bidding.controller.bidder;

import com.bidding.model.AuctionDisplayDTO;
import com.bidding.util.DataContext;
import com.bidding.util.SceneManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class ProductDetailController extends BaseBidderController {

    // --- THÀNH PHẦN UI ĐƯỢC INJECT TỪ FXML ---
    @FXML private Label lblProductName;
    @FXML @SuppressWarnings("unused") private Label lblProductImage;
    @FXML private Label lblStartPrice;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblStartTime;
    @FXML private Label lblEndTime;
    @FXML private Label lblSellerName;
    @FXML private Label lblAuctionStatus;
    @FXML private Label lblType;

    @FXML private TextArea txtDescription;
    @FXML private Button btnJoinRealtime;

    /**
     * Hàm khởi tạo tự động chạy khi màn hình được nạp
     */
    @FXML
    public void initialize() {
        super.setupSidebarBehavior();

        // Đọc dữ liệu thật từ DataContext chuyển sang
        AuctionDisplayDTO currentAuction = DataContext.getInstance().getCurrentAuction();

        if (currentAuction != null) {
            // 1. Đổ dữ liệu đấu giá và thông tin cơ bản sản phẩm
            lblProductName.setText(currentAuction.getItemName());
            lblCurrentPrice.setText(String.format("%,.0f ₫", currentAuction.getCurrentPrice()));
            lblStartPrice.setText(String.format("%,.0f ₫", currentAuction.getStartPrice()));
            lblStartTime.setText("Bắt đầu: " + currentAuction.getStartTime());
            lblEndTime.setText( currentAuction.getEndTime());

            // 2. Đổ dữ liệu chi tiết đồng bộ từ DB
            lblType.setText(currentAuction.getType());
            lblSellerName.setText(currentAuction.getSellerName() != null ? currentAuction.getSellerName() : "Chưa cập nhật");
            txtDescription.setText(currentAuction.getDescription() != null ? currentAuction.getDescription() : "Không có mô tả sản phẩm.");

            // 3. Định dạng chuỗi hiển thị trạng thái trực quan
            String state = currentAuction.getStatus();
            if ("OPEN".equalsIgnoreCase(state)) {
                lblAuctionStatus.setText("🔵 SẮP BẮT ĐẦU");
            } else if ("FINISHED".equalsIgnoreCase(state)) {
                lblAuctionStatus.setText("⚫ ĐÃ KẾT THÚC");
            } else {
                lblAuctionStatus.setText("🔴 ĐANG DIỄN RA");
            }
        }

        // 4. Kiểm tra trạng thái để bật/tắt nút vào phòng
        checkAuctionStatus();
    }

    private void checkAuctionStatus() {
        String status = lblAuctionStatus.getText();
        if (status != null && (status.contains("SẮP BẮT ĐẦU") || status.contains("ĐÃ KẾT THÚC"))) {
            btnJoinRealtime.setDisable(true);
            btnJoinRealtime.setText("Phiên đấu giá chưa mở hoặc đã đóng");
            btnJoinRealtime.setStyle("-fx-background-color: #cccccc; -fx-text-fill: white;");
        } else {
            btnJoinRealtime.setDisable(false);
            btnJoinRealtime.setText("Vào phòng đấu giá trực tiếp");
            btnJoinRealtime.setStyle("-fx-background-color: #185FA5; -fx-text-fill: white;");
        }
    }

    /**
     * Xử lý quay lại danh sách sản phẩm
     */
    @FXML
    @SuppressWarnings("unused")
    private void handleBack() {
        SceneManager.switchToAuctionList();
    }

    /**
     * Chuyển hướng người dùng vào phòng đấu giá trực tiếp (Realtime Bidding)
     */
    @FXML
    @SuppressWarnings("unused")
    private void handleJoinRealtime() {
        System.out.println("Đang kết nối vào phòng đấu giá trực tiếp...");
        SceneManager.switchToRealtimeBidding();
    }
}