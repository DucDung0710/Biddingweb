package com.bidding.controller.bidder;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import com.bidding.util.SceneManager;
import com.bidding.util.DataContext;

public class DashboardController extends BaseBidderController {
    @FXML private Button txtTopSearch;

    // --- CÁC THÀNH PHẦN THỐNG KÊ (STATS ROW) ---
    @FXML private Label lblStatActive;
    @FXML private Label lblStatLeading;
    @FXML private Label lblStatTotal;
    @FXML private Label lblStatWon;

    // --- PHẦN PHIÊN ĐẤU GIÁ NỔI BẬT ---
    @FXML private HBox auctionContainer;
    @FXML private Label lblPriceMacbook;
    @FXML private Label lblTimerMacbook;
    @FXML private Label lblPriceHonda;
    @FXML private Label lblTimerHonda;
    @FXML private Label lblPriceTranh;

    // --- KHỐI BIỂU ĐỒ 7 NGÀY ---
    @FXML private StackPane chartPlaceholder;

    @FXML
    private void handleTopSearch() {
        if (txtTopSearch != null) {
            String keyword = txtTopSearch.getText().trim();
            if (!keyword.isEmpty()) {
                // 1. Lưu từ khóa vào ngữ cảnh chung
                DataContext.setSearchKeyword(keyword);

                // 2. Điều hướng sang trang danh sách sản phẩm
                SceneManager.switchToAuctionList();
            }
        }
    }

    @FXML
    public void initialize() {
        // 1. Khởi tạo thanh điều hướng Sidebar kế thừa từ Base class
        super.setupSidebarBehavior();

        // 2. Nạp dữ liệu số liệu thống kê tổng quan
        loadDashboardStatistics();

        // 3. Kết nối và cập nhật giá/thời gian thực tế cho các thẻ nổi bật
        renderFeaturedAuctions();

        // 4. Khởi tạo và vẽ đồ thị hoạt động tuần
        initActivityChart();
    }

    /**
     * Đồng bộ số liệu thống kê từ Service/Cơ sở dữ liệu
     */
    private void loadDashboardStatistics() {
        lblStatActive.setText("4");
        lblStatLeading.setText("2");
        lblStatTotal.setText("12,500,000 ₫");
        lblStatWon.setText("7");
    }

    /**
     * Gắn dữ liệu động hoặc gán sự kiện Click trực tiếp cho các Card sản phẩm
     */
    private void renderFeaturedAuctions() {
        // Đồng bộ dữ liệu giả lập/thực tế từ database lên các Label trong card
        lblPriceMacbook.setText("28,500,000 ₫");
        lblTimerMacbook.setText("⏱ 01:23:45");

        lblPriceHonda.setText("520,000,000 ₫");
        lblTimerHonda.setText("⏱ 00:45:10");

        lblPriceTranh.setText("8,200,000 ₫");
        // Xử lý sự kiện khi người dùng click vào từng Card sản phẩm nổi bật để xem chi tiết
        if (auctionContainer != null) {
            auctionContainer.getChildren().forEach(node -> {
                node.setOnMouseClicked(event -> {
                    // Chuyển sang màn hình chi tiết sản phẩm
                    SceneManager.switchToProductDetail();
                });
            });
        }
    }

    /**
     * Khởi tạo đồ thị cột JavaFX BarChart thay thế vào vị trí hộp xám trống
     */
    private void initActivityChart() {
        if (chartPlaceholder == null) return;

        // Định nghĩa các trục tọa độ cho đồ thị cột
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);

        // Tinh chỉnh thẩm mỹ đồng bộ phong cách UI chung
        barChart.setLegendVisible(false);
        barChart.setAnimated(true);
        barChart.setStyle("-fx-background-color: transparent;");

        // Ẩn lưới nền và đường trục để giao diện trông phẳng và hiện đại
        xAxis.setTickLabelsVisible(true);
        yAxis.setTickLabelsVisible(false);
        yAxis.setOpacity(0);

        // Tạo tập dữ liệu số lượt đặt giá trong 7 ngày gần nhất (Thứ 2 -> Chủ Nhật)
        XYChart.Series<String, Number> dataSeries = new XYChart.Series<>();
        dataSeries.getData().add(new XYChart.Data<>("T2", 3));
        dataSeries.getData().add(new XYChart.Data<>("T3", 5));
        dataSeries.getData().add(new XYChart.Data<>("T4", 2));
        dataSeries.getData().add(new XYChart.Data<>("T5", 7));
        dataSeries.getData().add(new XYChart.Data<>("T6", 4));
        dataSeries.getData().add(new XYChart.Data<>("T7", 8));
        dataSeries.getData().add(new XYChart.Data<>("CN", 6));

        // Nạp dữ liệu vào đồ thị
        barChart.getData().add(dataSeries);

        // Xóa Label text giữ chỗ tĩnh cũ và nhúng đồ thị JavaFX thật vào giao diện
        chartPlaceholder.getChildren().clear();
        chartPlaceholder.getChildren().add(barChart);
    }

    /**
     * Trực quan hóa hành động khi click nút "Xem tất cả →"
     */
    @FXML
    private void handleViewAllAuctions() {
        SceneManager.switchToAuctionList();
    }
}