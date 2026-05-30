package com.bidding.controller.bidder;

import com.bidding.dao.JdbcAuctionDAO;
import com.bidding.dao.JdbcBidRecordDAO;
import com.bidding.model.AuctionDisplayDTO;
import com.bidding.model.BidRecord;
import com.bidding.service.BiddingService;
import com.bidding.shared.WalletManager;
import com.bidding.util.DataContext;
import com.bidding.util.SocketClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * RealtimeBiddingController - Quản lý giao diện đấu giá thời gian thực
 * Tuân thủ MVC: chỉ xử lý UI logic, gọi Service layer để xử lý business logic
 */
public class RealtimeBiddingController extends BaseBidderController {

    private int currentAuctionId;
    private Timeline countdownTimeline;
    private Timer refreshTimer;
    private final BiddingService biddingService = new BiddingService(new WalletManager(), null);
    private final JdbcAuctionDAO auctionDAO = new JdbcAuctionDAO();
    private final JdbcBidRecordDAO bidRecordDAO = new JdbcBidRecordDAO();
    private final SocketClient socketClient = SocketClient.getInstance();
    private final Gson gson = new Gson();

    public void setAuctionId(int auctionId) {
        this.currentAuctionId = auctionId;
    }

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
        
        AuctionDisplayDTO currentAuction = DataContext.getInstance().getCurrentAuction();
        if (currentAuction != null) {
            this.currentAuctionId = currentAuction.getAuctionId();
            updateAuctionUI(currentAuction);
            loadBidHistory();
            startCountdownTimer(currentAuction.getEndTime());
            startAutoRefresh();
        }
    }

    /**
     * Cập nhật UI với thông tin phiên đấu giá
     */
    private void updateAuctionUI(AuctionDisplayDTO auction) {
        lblAuctionTitle.setText(auction.getItemName() != null ? auction.getItemName() : "Sản phẩm không tên");
        lblStartPrice.setText(String.format("%,.0f ₫", auction.getStartPrice()));
        lblType.setText(auction.getType() != null ? auction.getType() : "N/A");
        lblSeller.setText(auction.getSellerName() != null ? auction.getSellerName() : "N/A");
        lblCurrentPrice.setText(String.format("%,.0f ₫", auction.getCurrentPrice()));
        lblEndTime.setText("Kết thúc: " + auction.getEndTime());
        lblStatus.setText("● " + (auction.getStatus() != null ? auction.getStatus() : "N/A"));
        
        int bidCount = bidRecordDAO.countBidsForAuction(currentAuctionId);
        lblBidCount.setText(bidCount + " lượt đặt giá");
        lblBidCountSmall.setText(bidCount + " lượt");
        
        // Cập nhật người dẫn đầu
        BidRecord highestBid = bidRecordDAO.getHighestBid(currentAuctionId);
        if (highestBid != null) {
            lblLeader.setText("Người dẫn đầu: " + highestBid.getBidderName());
        } else {
            lblLeader.setText("Chưa có người dẫn đầu");
        }
        
        // Tính giá tối thiểu (tăng 5%)
        double minBidPrice = auction.getCurrentPrice() * 1.05;
        lblMinBid.setText(String.format("Giá tối thiểu: %,.0f ₫", minBidPrice));
    }

    /**
     * Tải lịch sử đặt giá từ database và hiển thị lên UI
     */
    private void loadBidHistory() {
        List<BidRecord> bidRecords = bidRecordDAO.getByAuctionId(currentAuctionId);
        bidHistoryContainer.getChildren().clear();
        
        // Hiển thị từ mới nhất lên trước
        for (int i = bidRecords.size() - 1; i >= 0; i--) {
            BidRecord bid = bidRecords.get(i);
            HBox bidRow = createBidHistoryRow(bid);
            bidHistoryContainer.getChildren().add(bidRow);
        }
        
        // Cập nhật biểu đồ giá
        updatePriceChart(bidRecords);
    }

    /**
     * Tạo một hàng hiển thị bid history
     */
    private HBox createBidHistoryRow(BidRecord bid) {
        HBox row = new HBox();
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setSpacing(8);
        row.setStyle("-fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;");
        
        Label bidderLabel = new Label(bid.getBidderName());
        bidderLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        bidderLabel.setPrefWidth(120);
        
        Label amountLabel = new Label(String.format("%,.0f ₫", bid.getBidAmount().doubleValue()));
        amountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #185FA5;");
        amountLabel.setPrefWidth(100);
        
        Label timeLabel = new Label(bid.getBidTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");
        
        if (bid.isWinning()) {
            row.setStyle("-fx-background-color: #F0F8FF; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;");
            Label winLabel = new Label("🏆");
            row.getChildren().add(winLabel);
        }
        
        row.getChildren().addAll(bidderLabel, amountLabel, timeLabel);
        return row;
    }

    /**
     * Cập nhật biểu đồ giá dựa trên lịch sử bid
     */
    private void updatePriceChart(List<BidRecord> bidRecords) {
        priceChart.getData().clear();
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Giá");
        
        for (int i = 0; i < bidRecords.size(); i++) {
            BidRecord bid = bidRecords.get(i);
            String timeLabel = bid.getBidTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            series.getData().add(new XYChart.Data<>(timeLabel, bid.getBidAmount().doubleValue()));
        }
        
        priceChart.getData().add(series);
    }

    /**
     * Xử lý sự kiện đặt giá
     */
    @FXML
    private void handlePlaceBid() {
        try {
            String bidText = txtBidAmount.getText().trim();
            if (bidText.isEmpty()) {
                showError("Vui lòng nhập số tiền!");
                return;
            }
            
            double bidAmount = Double.parseDouble(bidText);
            
            if (bidAmount <= 0) {
                showError("Giá đấu phải lớn hơn 0!");
                return;
            }
            
            // Lấy thông tin bidder từ session
            // Lấy thông tin bidder từ UserSession (Khớp với màn hình LogIn)
            var currentUser = com.bidding.shared.UserSession.getInstance().getLoggedInUser();
            if (currentUser == null) {
                showError("Bạn chưa đăng nhập!");
                return;
            }
            
            // Gọi BiddingService để xử lý logic đặt giá
            BiddingService.BiddingResult result = biddingService.placeBid(currentAuctionId, currentUser, bidAmount);
            
            if (result.isSuccess()) {
                showSuccess(result.getMessage());
                txtBidAmount.clear();
                
                // Refresh UI
                refreshAuctionData();
            } else {
                showError(result.getMessage());
            }
            
        } catch (NumberFormatException e) {
            showError("Vui lòng nhập số tiền hợp lệ!");
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Làm mới dữ liệu phiên đấu giá
     */
    private void refreshAuctionData() {
        AuctionDisplayDTO auction = auctionDAO.getAuctionById(currentAuctionId);
        if (auction != null) {
            Platform.runLater(() -> {
                updateAuctionUI(auction);
                loadBidHistory();
            });
        }
    }

    /**
     * Bắt đầu bộ đếm ngược thời gian
     */
    private void startCountdownTimer(String endTimeStr) {
        // Parse end time và tính toán thời gian còn lại
        try {
            // Sử dụng đúng pattern khớp với chuỗi "yyyy-MM-dd HH:mm:ss" của cơ sở dữ liệu
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter);

            countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdown(endTime)));
            countdownTimeline.setCycleCount(Timeline.INDEFINITE);
            countdownTimeline.play();
        } catch (Exception e) {
            System.err.println("Lỗi parse thời gian kết thúc: " + e.getMessage());
        }
    }

    /**
     * Cập nhật đồng hồ đếm ngược
     */
    private void updateCountdown(LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
            lblCountdown.setText("00:00:00");
            if (countdownTimeline != null) {
                countdownTimeline.stop();
            }
            // Phiên đã kết thúc
            Platform.runLater(() -> {
                showSuccess("Phiên đấu giá đã kết thúc!");
                btnBid.setDisable(true);
                txtBidAmount.setDisable(true);
            });
        } else {
            long secondsRemaining = java.time.temporal.ChronoUnit.SECONDS.between(now, endTime);
            long hours = secondsRemaining / 3600;
            long minutes = (secondsRemaining % 3600) / 60;
            long seconds = secondsRemaining % 60;
            
            String countdown = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            lblCountdown.setText(countdown);
        }
    }

    /**
     * Bắt đầu tự động làm mới dữ liệu mỗi 5 giây
     */
    private void startAutoRefresh() {
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshAuctionData();
            }
        }, 0, 5000); // Refresh mỗi 5 giây
    }

    private void setupRealtimeChart() {
        priceChart.setAnimated(true);
        xAxis.setLabel("Thời gian");
        yAxis.setLabel("Giá (₫)");
    }

    @FXML
    private void handleToggleAutoBid() {
        boolean isSelected = chkAutoBid.isSelected();
        txtMaxBid.setDisable(!isSelected);
        txtIncrement.setDisable(!isSelected);
        btnSetAutoBid.setDisable(!isSelected);
    }

    @FXML
    private void handleSetAutoBid() {
        // TODO: Triển khai auto-bid functionality
        showSuccess("Auto-Bid đã được bật!");
    }

    @FXML
    private void handleBack() {
        // Dừng các timer trước khi quay lại
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
        com.bidding.util.SceneManager.switchToAuctionList();
    }

    private void showError(String msg) {
        lblBidError.setText(msg);
        lblBidError.setStyle("-fx-text-fill: #A32D2D;");
        lblBidError.setVisible(true);
        lblBidError.setManaged(true);
    }

    private void showSuccess(String msg) {
        lblBidError.setText(msg);
        lblBidError.setStyle("-fx-text-fill: #3B6D11;");
        lblBidError.setVisible(true);
        lblBidError.setManaged(true);
    }
}