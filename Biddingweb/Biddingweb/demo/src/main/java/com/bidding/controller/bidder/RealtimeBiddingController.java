package com.bidding.controller.bidder;

import com.bidding.model.AuctionDisplayDTO;
import com.bidding.model.BidHistoryDTO;
import com.bidding.shared.UserSession;
import com.bidding.util.SceneManager;
import com.bidding.util.SocketClient;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RealtimeBiddingController extends BaseBidderController {

    @FXML private Label lblAuctionTitle;
    @FXML private Label lblLiveDot;
    @FXML private Label lblLiveCount;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLeader;
    @FXML private Label lblCountdown;
    @FXML private Label lblEndTime;
    @FXML private Label lblBidCount;
    @FXML private Label lblStatus;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnBid;
    @FXML private Label lblMinBid;
    @FXML private CheckBox chkAutoBid;
    @FXML private TextField txtMaxBid;
    @FXML private TextField txtIncrement;
    @FXML private Button btnSetAutoBid;
    @FXML private Label lblBidError;
    @FXML private VBox bidHistoryContainer;
    @FXML private Label lblStartPrice;
    @FXML private Label lblType;
    @FXML private Label lblSeller;

    private final Gson gson = new Gson();
    private AuctionDisplayDTO currentAuction;
    private XYChart.Series<String, Number> priceSeries;
    private Timer countdownTimer;
    private LocalDateTime endTime;
    private double currentPrice;

    @FXML
    public void initialize() {
        // Gọi hàm setup của lớp cha, các nút điều hướng sidebar từ giờ sẽ tự dọn luồng ngầm
        super.setupSidebarBehavior();
        setupRealtimeChart();

        int auctionId = com.bidding.util.DataContext.getInstance().getSelectedAuctionId();
        loadAuctionDetail(auctionId);
        subscribeToAuction(auctionId);
    }

    private void setupRealtimeChart() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá đấu");
        priceChart.getData().add(priceSeries);
        priceChart.setAnimated(false);
        xAxis.setLabel("Thời gian");
        yAxis.setLabel("Giá (₫)");
    }

    private void loadAuctionDetail(int auctionId) {
        JsonObject req = new JsonObject();
        req.addProperty("action",    "GET_AUCTION_DETAIL");
        req.addProperty("auctionId", auctionId);

        JsonObject res = SocketClient.getInstance().sendRequest(req);
        if (!"OK".equals(res.get("status").getAsString())) {
            showError("Không tải được thông tin phiên đấu giá");
            return;
        }

        currentAuction = gson.fromJson(res.get("auction"), AuctionDisplayDTO.class);
        currentPrice   = currentAuction.getCurrentPrice();

        Platform.runLater(() -> {
            lblAuctionTitle.setText(currentAuction.getItemName());
            lblCurrentPrice.setText(formatPrice(currentPrice));
            lblLeader.setText("bởi " + maskName(currentAuction.getLeaderName()));
            lblBidCount.setText(currentAuction.getBidCount() + " lượt đặt giá");
            lblStatus.setText(currentAuction.getStatus());
            lblStartPrice.setText(formatPrice(currentAuction.getStartPrice()));
            lblType.setText(currentAuction.getItemType());
            lblSeller.setText(currentAuction.getSellerName());

            lblMinBid.setText("Giá tối thiểu: " + formatPrice(currentPrice + 500_000));

            endTime = LocalDateTime.parse(
                    currentAuction.getEndTime(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            );
            lblEndTime.setText("Kết thúc: " +
                    endTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            startCountdown();
        });

        Type listType = new TypeToken<ArrayList<BidHistoryDTO>>(){}.getType();
        List<BidHistoryDTO> history = gson.fromJson(res.get("history"), listType);
        Platform.runLater(() -> renderBidHistory(history));
    }

    private void startCountdown() {
        if (countdownTimer != null) countdownTimer.cancel();
        countdownTimer = new Timer(true);
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long seconds = java.time.Duration.between(
                        LocalDateTime.now(), endTime).getSeconds();
                if (seconds <= 0) {
                    Platform.runLater(() -> {
                        lblCountdown.setText("00:00:00");
                        lblCountdown.setStyle("-fx-text-fill: #999999;");
                        lblStatus.setText("FINISHED");
                        btnBid.setDisable(true);
                    });
                    countdownTimer.cancel();
                    return;
                }
                long h = seconds / 3600;
                long m = (seconds % 3600) / 60;
                long s = seconds % 60;
                String timeStr = String.format("%02d:%02d:%02d", h, m, s);
                String color = seconds < 300 ? "#E53935" : "#1565C0";
                Platform.runLater(() -> {
                    lblCountdown.setText(timeStr);
                    lblCountdown.setStyle("-fx-text-fill: " + color + ";");
                });
            }
        }, 0, 1000);
    }

    private void subscribeToAuction(int auctionId) {
        JsonObject req = new JsonObject();
        req.addProperty("action",    "SUBSCRIBE_AUCTION");
        req.addProperty("auctionId", auctionId);
        SocketClient.getInstance().sendRequest(req);

        // Gắn listener vào bộ Dispatcher trung tâm thay vì tự tạo vòng lặp đọc riêng
        SocketClient.getInstance().setPushListener(msg -> {
            String type = msg.get("type").getAsString();
            if ("BID_UPDATE".equals(type) && msg.get("auctionId").getAsInt() == auctionId) {
                double newPrice = msg.get("newPrice").getAsDouble();
                String bidder   = msg.get("bidderId").getAsString();
                String time     = msg.get("timestamp").getAsString();
                onBidUpdate(newPrice, bidder, time);
            }
        });
    }

    private void onBidUpdate(double newPrice, String bidderName, String timestamp) {
        currentPrice = newPrice;
        Platform.runLater(() -> {
            lblCurrentPrice.setText(formatPrice(newPrice));
            lblLeader.setText("bởi " + maskName(bidderName));
            lblMinBid.setText("Giá tối thiểu: " + formatPrice(newPrice + 500_000));

            priceSeries.getData().add(new XYChart.Data<>(timestamp, newPrice / 1_000_000.0));
            if (priceSeries.getData().size() > 20) {
                priceSeries.getData().remove(0);
            }

            addBidHistoryRow(bidderName, newPrice, timestamp);

            try {
                int currentCount = Integer.parseInt(lblBidCount.getText().split(" ")[0]);
                lblBidCount.setText((currentCount + 1) + " lượt đặt giá");
            } catch (Exception ignored) {}
        });
    }

    @FXML
    private void handlePlaceBid() {
        hideBidError();
        String input = txtBidAmount.getText().trim();
        if (input.isEmpty()) {
            showError("Vui lòng nhập số tiền!");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(input.replace(",", "").replace(".", ""));
        } catch (NumberFormatException e) {
            showError("Số tiền không hợp lệ!");
            return;
        }

        if (amount <= currentPrice) {
            showError("Giá đấu phải cao hơn " + formatPrice(currentPrice));
            return;
        }

        int userId    = UserSession.getInstance().getLoggedInUser().getId();
        int auctionId = com.bidding.util.DataContext.getInstance().getSelectedAuctionId();

        JsonObject req = new JsonObject();
        req.addProperty("action",    "PLACE_BID");
        req.addProperty("auctionId", auctionId);
        req.addProperty("bidderId",  userId);
        req.addProperty("amount",    amount);

        JsonObject res = SocketClient.getInstance().sendRequest(req);
        if ("OK".equals(res.get("status").getAsString())) {
            txtBidAmount.clear();
        } else {
            showError(res.get("message").getAsString());
        }
    }

    @FXML
    private void handleToggleAutoBid() {
        boolean on = chkAutoBid.isSelected();
        txtMaxBid.setDisable(!on);
        txtIncrement.setDisable(!on);
        btnSetAutoBid.setDisable(!on);
    }

    @FXML
    private void handleSetAutoBid() {
        hideBidError();
        try {
            double maxBid    = Double.parseDouble(txtMaxBid.getText().replace(",", ""));
            double increment = Double.parseDouble(txtIncrement.getText().replace(",", ""));
            int userId    = UserSession.getInstance().getLoggedInUser().getId();
            int auctionId = com.bidding.util.DataContext.getInstance().getSelectedAuctionId();

            JsonObject req = new JsonObject();
            req.addProperty("action",    "SET_AUTO_BID");
            req.addProperty("auctionId", auctionId);
            req.addProperty("bidderId",  userId);
            req.addProperty("maxBid",    maxBid);
            req.addProperty("increment", increment);

            JsonObject res = SocketClient.getInstance().sendRequest(req);
            if ("OK".equals(res.get("status").getAsString())) {
                showSuccess("Auto-Bid đã được thiết lập!");
            } else {
                showError(res.get("message").getAsString());
            }
        } catch (NumberFormatException e) {
            showError("Vui lòng nhập đúng định dạng số tiền!");
        }
    }

    private void renderBidHistory(List<BidHistoryDTO> list) {
        bidHistoryContainer.getChildren().clear();
        if (list == null) return;
        for (BidHistoryDTO bid : list) {
            addBidHistoryRow(bid.getBidderName(), bid.getAmount(), bid.getBidTime());
        }
    }

    private void addBidHistoryRow(String name, double amount, String time) {
        HBox row = new HBox();
        row.setStyle("-fx-padding: 8 12 8 12; -fx-border-color: #F0F0F0; -fx-border-width: 0 0 1 0;");

        Label lblName   = new Label(maskName(name));
        Label lblAmount = new Label(formatPrice(amount));
        Label lblTime   = new Label(time);

        lblName.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");
        lblAmount.setStyle("-fx-font-size: 12px; -fx-text-fill: #1565C0; -fx-font-weight: bold;");
        lblTime.setStyle("-fx-font-size: 11px; -fx-text-fill: #999;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        row.getChildren().addAll(lblName, spacer, lblAmount, new Label("  "), lblTime);
        bidHistoryContainer.getChildren().add(0, row);
    }

    private String formatPrice(double price) {
        return String.format("%,.0f ₫", price);
    }

    private String maskName(String name) {
        if (name == null || name.isBlank() || "Chưa có".equals(name)) return "Chưa có";
        String[] parts = name.split(" ");
        return parts[0] + "***";
    }

    private void showError(String msg) {
        if (lblBidError != null) {
            lblBidError.setText(msg);
            lblBidError.setStyle("-fx-text-fill: #E53935;");
            lblBidError.setVisible(true);
            lblBidError.setManaged(true);
        }
    }

    private void showSuccess(String msg) {
        if (lblBidError != null) {
            lblBidError.setText(msg);
            lblBidError.setStyle("-fx-text-fill: #2E7D32;");
            lblBidError.setVisible(true);
            lblBidError.setManaged(true);
        }
    }

    private void hideBidError() {
        if (lblBidError != null) {
            lblBidError.setVisible(false);
            lblBidError.setManaged(false);
        }
    }

    @FXML
    private void handleBack() {
        // Ngắt Listener realtime khi chủ động nhấn nút Back quay lại danh sách
        SocketClient.getInstance().clearPushListener();
        if (countdownTimer != null) countdownTimer.cancel();
        SceneManager.switchToAuctionList();
    }
}