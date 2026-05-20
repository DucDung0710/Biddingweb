package com.bidding.controller.bidder;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;


import com.bidonline.model.Auction;
import com.bidonline.model.BidTransaction;
import com.bidonline.session.UserSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
        import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class RealtimeBiddingController implements Initializable {

    // ── TopBar ────────────────────────────────────────────────────
    @FXML
    private Label lblAuctionTitle;
    @FXML private Label lblLiveDot;
    @FXML private Label lblLiveCount;

    // ── Giá + Đồng hồ ────────────────────────────────────────────
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLeader;
    @FXML private Label lblCountdown;
    @FXML private Label lblEndTime;
    @FXML private Label lblBidCount;
    @FXML private Label lblBidCountSmall;
    @FXML private Label lblStatus;

    // ── Biểu đồ ──────────────────────────────────────────────────
    @FXML private LineChart<String, Number> priceChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    // ── Form đặt giá ─────────────────────────────────────────────
    @FXML private TextField txtBidAmount;
    @FXML private Button btnBid;
    @FXML private Label     lblMinBid;
    @FXML private Label     lblBidError;

    // ── Auto-bid ──────────────────────────────────────────────────
    @FXML private CheckBox chkAutoBid;
    @FXML private TextField txtMaxBid;
    @FXML private TextField txtIncrement;
    @FXML private Button    btnSetAutoBid;

    // ── Lịch sử bid (inject động) ─────────────────────────────────
    @FXML private VBox bidHistoryContainer;

    // ── Thông tin sản phẩm ────────────────────────────────────────
    @FXML private Label lblStartPrice;
    @FXML private Label lblType;
    @FXML private Label lblSeller;

    // ── Dữ liệu ───────────────────────────────────────────────────
    private Auction currentAuction;
    private long    currentPrice   = 28_500_000L;
    private long    minBidStep     = 500_000L;
    private int     secondsLeft    = 5025; // 01:23:45
    private int     totalBids      = 14;
    private final NumberFormat moneyFmt = NumberFormat.getNumberInstance(new Locale("vi","VN"));

    private Timeline countdownTimer;
    private XYChart.Series<String, Number> priceSeries;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupChart();
        setupCountdown();
        updateBidUI();
        // TODO: kết nối WebSocket/Socket để nhận bid realtime
        // socketService.subscribe(auctionId, this::onNewBid);
    }

    /** Được gọi từ màn hình trước khi chuyển sang */
    public void setAuction(Auction auction) {
        this.currentAuction = auction;
        lblAuctionTitle.setText(auction.getProductName() + " — Đấu giá trực tiếp");
        lblStartPrice.setText(moneyFmt.format(auction.getStartPrice()) + " ₫");
        lblType.setText(auction.getType());
        lblSeller.setText(auction.getSellerName());
        lblEndTime.setText("Kết thúc: " + auction.getEndTime());
        currentPrice = auction.getCurrentPrice();
        updateBidUI();
    }

    // ── Setup ─────────────────────────────────────────────────────

    private void setupChart() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá đấu");

        // Mock data lịch sử giá
        long[] prices = {20_000_000L, 21_500_000L, 22_000_000L, 24_000_000L,
                25_500_000L, 27_000_000L, 28_500_000L};
        String[] times = {"08:00","10:00","12:00","14:00","16:00","17:30","18:45"};
        for (int i = 0; i < prices.length; i++) {
            priceSeries.getData().add(new XYChart.Data<>(times[i], prices[i] / 1_000_000.0));
        }

        priceChart.getData().add(priceSeries);
        priceChart.setCreateSymbols(true);
        yAxis.setLabel("Giá (triệu ₫)");
        xAxis.setLabel("Thời gian");
    }

    private void setupCountdown() {
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (secondsLeft > 0) {
                secondsLeft--;
                updateCountdownLabel();
                // Anti-sniping: nếu còn <= 30 giây thì tô đỏ đậm
                if (secondsLeft <= 30) {
                    lblCountdown.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #791F1F;");
                }
            } else {
                countdownTimer.stop();
                lblStatus.setText("● FINISHED");
                lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #5F5E5A; -fx-font-weight: bold;");
                btnBid.setDisable(true);
                txtBidAmount.setDisable(true);
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void updateCountdownLabel() {
        int h = secondsLeft / 3600;
        int m = (secondsLeft % 3600) / 60;
        int s = secondsLeft % 60;
        lblCountdown.setText(String.format("%02d:%02d:%02d", h, m, s));
    }

    private void updateBidUI() {
        long minBid = currentPrice + minBidStep;
        lblCurrentPrice.setText(moneyFmt.format(currentPrice) + " ₫");
        lblMinBid.setText("Giá tối thiểu: " + moneyFmt.format(minBid) + " ₫  (bước giá: "
                + moneyFmt.format(minBidStep) + " ₫)");
        txtBidAmount.setPromptText(moneyFmt.format(minBid));
        lblBidCount.setText(totalBids + " lượt đặt giá");
        lblBidCountSmall.setText(totalBids + " lượt");
    }

    // ── Xử lý bid ─────────────────────────────────────────────────

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        String input = txtBidAmount.getText().trim().replaceAll("[^0-9]", "");
        if (input.isEmpty()) {
            showBidError("Vui lòng nhập giá đấu.");
            return;
        }

        long bidAmount;
        try {
            bidAmount = Long.parseLong(input);
        } catch (NumberFormatException e) {
            showBidError("Giá đấu không hợp lệ.");
            return;
        }

        long minBid = currentPrice + minBidStep;
        if (bidAmount < minBid) {
            showBidError("Giá đấu phải ≥ " + moneyFmt.format(minBid) + " ₫");
            return;
        }

        // Hợp lệ → gửi bid lên Server
        hideBidError();
        // TODO: apiService.placeBid(currentAuction.getId(), bidAmount, token)
        onNewBid(UserSession.getInstance().getCurrentUser().getUsername(), bidAmount);
    }

    /** Được gọi khi có bid mới (từ Socket hoặc từ chính user này) */
    public void onNewBid(String username, long price) {
        Platform.runLater(() -> {
            currentPrice = price;
            totalBids++;
            lblLeader.setText("bởi " + username + "***");
            updateBidUI();

            // Cập nhật biểu đồ
            String nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            priceSeries.getData().add(new XYChart.Data<>(nowTime, price / 1_000_000.0));

            // Cập nhật live count
            lblLiveCount.setText("Live · " + totalBids + " bidders");

            // Thêm vào lịch sử bid UI
            addBidHistoryRow(username, price, "Vừa xong");
        });
    }

    private void addBidHistoryRow(String username, long price, String time) {
        HBox row = new HBox(8);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 7 12; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;");

        Label userLbl = new Label(username + "***");
        userLbl.setStyle("-fx-font-size: 12px;");
        HBox.setHgrow(userLbl, Priority.ALWAYS);

        Label priceLbl = new Label(moneyFmt.format(price));
        priceLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #185FA5;");

        Label timeLbl = new Label(time);
        timeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaaaaa;");

        row.getChildren().addAll(userLbl, priceLbl, timeLbl);

        // Thêm vào đầu danh sách
        bidHistoryContainer.getChildren().add(0, row);
    }

    // ── Nút tăng nhanh ────────────────────────────────────────────

    @FXML private void handleQuickBid500(ActionEvent e) { setQuickBid(500_000L); }
    @FXML private void handleQuickBid1M(ActionEvent e)  { setQuickBid(1_000_000L); }
    @FXML private void handleQuickBid2M(ActionEvent e)  { setQuickBid(2_000_000L); }

    private void setQuickBid(long increment) {
        long suggested = currentPrice + increment;
        txtBidAmount.setText(moneyFmt.format(suggested));
    }

    // ── Auto-bid ──────────────────────────────────────────────────

    @FXML
    private void handleToggleAutoBid(ActionEvent e) {
        boolean enabled = chkAutoBid.isSelected();
        txtMaxBid.setDisable(!enabled);
        txtIncrement.setDisable(!enabled);
        btnSetAutoBid.setDisable(!enabled);
    }

    @FXML
    private void handleSetAutoBid(ActionEvent e) {
        try {
            long maxBid   = Long.parseLong(txtMaxBid.getText().trim().replaceAll("[^0-9]",""));
            long increment = Long.parseLong(txtIncrement.getText().trim().replaceAll("[^0-9]",""));

            if (maxBid <= currentPrice) {
                showBidError("Giá tối đa phải lớn hơn giá hiện tại.");
                return;
            }
            // TODO: gửi lên server: POST /api/auctions/{id}/auto-bid { maxBid, increment }
            hideBidError();
            Alert ok = new Alert(Alert.AlertType.INFORMATION,
                    "Auto-bid đã được kích hoạt!\nGiá tối đa: " + moneyFmt.format(maxBid) +
                            " ₫\nBước giá: " + moneyFmt.format(increment) + " ₫",
                    ButtonType.OK);
            ok.showAndWait();
        } catch (NumberFormatException ex) {
            showBidError("Vui lòng nhập đúng định dạng số.");
        }
    }

    // ── Back ──────────────────────────────────────────────────────

    @FXML
    private void handleBack(ActionEvent e) {
        countdownTimer.stop();
        // TODO: quay lại auction_list.fxml
    }

    // ── Helpers ───────────────────────────────────────────────────

    private void showBidError(String msg) {
        lblBidError.setText("⚠ " + msg);
        lblBidError.setVisible(true);
        lblBidError.setManaged(true);
    }

    private void hideBidError() {
        lblBidError.setVisible(false);
        lblBidError.setManaged(false);
    }
}
