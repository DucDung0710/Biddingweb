package com.bidding.controller.bidder;

import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class RealtimeBiddingController extends BaseBidderController {

    // Khối Thông tin chung & Trạng thái Live
    @FXML private Label lblAuctionTitle;
    @FXML private Label lblLiveDot;
    @FXML private Label lblLiveCount;

    // Khối Thông số giá và thời gian
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLeader;
    @FXML private Label lblCountdown;
    @FXML private Label lblEndTime;
    @FXML private Label lblBidCount;
    @FXML private Label lblStatus;

    // Line Chart Realtime
    @FXML private LineChart<String, Number> priceChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    // Bidding Form controls
    @FXML private TextField txtBidAmount;
    @FXML private Button btnBid;
    @FXML private Label lblMinBid;

    // Auto-bid controls
    @FXML private CheckBox chkAutoBid;
    @FXML private TextField txtMaxBid;
    @FXML private TextField txtIncrement;
    @FXML private Button btnSetAutoBid;

    // Khối thông báo lỗi và lịch sử bên phải
    @FXML private Label lblBidError;
    @FXML private Label lblBidCountSmall;
    @FXML private VBox bidHistoryContainer;

    // Khối thông tin sản phẩm tóm tắt ở góc dưới
    @FXML private Label lblStartPrice;
    @FXML private Label lblType;
    @FXML private Label lblSeller;

    @FXML
    public void initialize() {
        super.setupSidebarBehavior();
        setupRealtimeChart();
        initSocketConnection();
    }

    private void setupRealtimeChart() {
        priceChart.setAnimated(true);
        xAxis.setLabel("Thời gian");
        yAxis.setLabel("Giá (₫)");
    }

    @FXML
    private void handlePlaceBid() {
        try {
            double amount = Double.parseDouble(txtBidAmount.getText());
            if (amount <= 0) {
                throw new IllegalArgumentException("Giá đấu phải lớn hơn 0!");
            }
        } catch (NumberFormatException e) {
            showError("Vui lòng nhập số tiền hợp lệ!");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleToggleAutoBid() {
        boolean isSelected = chkAutoBid.isSelected();
        txtMaxBid.setDisable(!isSelected);
        txtIncrement.setDisable(!isSelected);
        btnSetAutoBid.setDisable(!isSelected); // Bật/tắt nút Lưu Auto-Bid đồng bộ
    }

    @FXML
    private void handleSetAutoBid() {
        // Thiết lập cấu hình Đấu giá tự động
    }

    @FXML
    private void handleBack() {
        com.bidding.util.SceneManager.switchToAuctionList();
    }

    private void initSocketConnection() {
        // Kết nối TCP Socket với Server nhận dữ liệu Realtime
    }

    private void showError(String msg) {
        lblBidError.setText(msg);
        lblBidError.setVisible(true);
        lblBidError.setManaged(true);
    }
}