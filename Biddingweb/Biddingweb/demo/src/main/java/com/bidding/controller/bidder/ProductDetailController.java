package com.bidding.controller.bidder;

import com.bidding.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class ProductDetailController extends BaseBidderController {

    // --- THÀNH PHẦN UI ĐƯỢC INJECT TỪ FXML ---
    @FXML private Label lblProductName;
    @FXML private Label lblProductImage;
    @FXML private Label lblStartPrice;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblStartTime;
    @FXML private Label lblEndTime;
    @FXML private Label lblCategory;
    @FXML private Label lblSellerName;
    @FXML private Label lblAuctionStatus;

    @FXML private TextArea txtDescription;
    @FXML private Button btnJoinRealtime;

    /**
     * Hàm khởi tạo tự động chạy khi màn hình được nạp
     */
    @FXML
    public void initialize() {
        // 1. Kích hoạt logic Sidebar từ lớp cha
        super.setupSidebarBehavior();

        // 2. Kiểm tra trạng thái phiên để ẩn/hiện nút "Vào phòng đấu giá"
        checkAuctionStatus();

        // 3. Giả lập hoặc nạp dữ liệu sản phẩm (Thực tế sẽ nhận dữ liệu từ ListController)
        loadSampleData();
    }

    /**
     * Logic kiểm tra trạng thái: Chỉ cho phép vào phòng nếu đang "ĐANG DIỄN RA"
     */
    private void checkAuctionStatus() {
        String status = lblAuctionStatus.getText();
        if (status.contains("SẮP BẮT ĐẦU") || status.contains("ĐÃ KẾT THÚC")) {
            btnJoinRealtime.setDisable(true);
            btnJoinRealtime.setText("Phiên đấu giá chưa mở");
            btnJoinRealtime.setStyle("-fx-background-color: #cccccc; -fx-text-fill: white;");
        }
    }

    /**
     * Nạp dữ liệu mẫu (Sẽ được thay thế bằng logic truyền dữ liệu đối tượng Item)
     */
    private void loadSampleData() {
        // Bạn có thể tạo 1 hàm public setProduct(Item item) để ListController gọi khi chuyển trang
        lblProductName.setText("MacBook Pro M3 14 inch 16GB/512GB");
        lblCurrentPrice.setText("28,500,000 ₫");
        lblAuctionStatus.setText("🔴 ĐANG DIỄN RA");
        txtDescription.setText("Thiết bị mới 99% không một vết xước...");
    }

    /**
     * Xử lý quay lại danh sách sản phẩm
     */
    @FXML
    private void handleBack() {
        SceneManager.switchToAuctionList();
    }

    /**
     * Chuyển hướng người dùng vào phòng đấu giá trực tiếp (Realtime Bidding)
     */
    @FXML
    private void handleJoinRealtime() {
        System.out.println("Đang kết nối vào phòng đấu giá trực tiếp...");
        SceneManager.switchToRealtimeBidding();
    }
}