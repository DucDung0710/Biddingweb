package com.bidding.controller.bidder;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class AuctionHistoryController extends BaseBidderController {

    @FXML private TextField txtSearchHistory;
    @FXML private ComboBox<String> cmbHistoryStatus;

    @FXML private TableView<Object> tblAuctionHistory;
    @FXML private TableColumn<Object, String> colItemName;
    @FXML private TableColumn<Object, Double> colMyLastBid;
    @FXML private TableColumn<Object, Double> colCurrentHighest;
    @FXML private TableColumn<Object, String> colStatus;
    @FXML private TableColumn<Object, String> colResult;
    @FXML private TableColumn<Object, Void> colAction;

    @FXML
    public void initialize() {
        super.setupSidebarBehavior();
        setupTableColumns();
        loadHistoryData();
    }

    private void setupTableColumns() {
        // Cấu hình CellValueFactory cho từng cột dữ liệu để đảm bảo đóng gói
    }

    private void loadHistoryData() {
        // Lấy dữ liệu lịch sử đặt giá từ database thông qua Controller
    }
}