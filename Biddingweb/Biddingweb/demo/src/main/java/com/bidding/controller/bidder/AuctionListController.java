package com.bidding.controller.bidder;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

public class AuctionListController extends BaseBidderController {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private ComboBox<String> cmbType;

    @FXML private FlowPane paneRunning;
    @FXML private FlowPane paneOpen;
    @FXML private FlowPane paneFinished;

    @FXML
    public void initialize() {
        // 1. Gọi thiết lập Sidebar chung của lớp cha
        super.setupSidebarBehavior();

        // 2. TỐI ƯU UX: Đang ở trang danh sách thì vô hiệu hóa sự kiện click lại chính trang này
        if (navAuctions != null) {
            navAuctions.setOnMouseClicked(null); // Xóa bỏ sự kiện click chuyển trang trùng lặp
        }

        initFilterComboboxes();
        loadAllAuctionItems();
    }

    private void initFilterComboboxes() {
        cmbStatus.getItems().addAll("Tất cả", "Đang diễn ra", "Sắp bắt đầu", "Đã kết thúc");
        cmbStatus.setValue("Tất cả"); // Đặt giá trị mặc định tránh lỗi Null

        cmbType.getItems().addAll("Tất cả", "Electronics", "Vehicles", "Art");
        cmbType.setValue("Tất cả");
    }

    @FXML
    private void handleFilter() {
        String keyword = txtSearch.getText();
        String status = cmbStatus.getValue();
        String type = cmbType.getValue();

        System.out.println("Đang lọc theo: " + keyword + " | " + status + " | " + type);
        // Gọi xuống tầng Service/DAO để render lại dữ liệu động vào các FlowPane
    }

    private void loadAllAuctionItems() {
        // Render động danh sách sản phẩm đấu giá từ Server lên các ô FlowPane tương ứng
    }
}