package com.bidding.controller.bidder;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class WalletBidderController extends BaseBidderController {

    @FXML private Label lblCurrentBalance;
    @FXML private Label lblLockedBalance;
    @FXML private Label lblTotalBalance;

    // Các thành phần form nạp tiền
    @FXML private ComboBox<String> cmbDepositMethod;
    @FXML private VBox paneCardMethod;
    @FXML private VBox paneAdminMethod;
    @FXML private TextField txtCardCode;
    @FXML private TextField txtDepositAmount;
    @FXML private Label lblDepositResult;


    // TableView dữ liệu
    @FXML private TableView<Transaction> tblTransactions;
    @FXML private TableColumn<Transaction, String> colTxTime;
    @FXML private TableColumn<Transaction, String> colTxType;
    @FXML private TableColumn<Transaction, String> colTxAmount;
    @FXML private TableColumn<Transaction, String> colTxStatus;
    @FXML private TableColumn<Transaction, String> colTxNote;

    private ObservableList<Transaction> transactionList;

    @FXML
    public void initialize() {
        // 1. Kế thừa hành vi Sidebar từ lớp cha
        super.setupSidebarBehavior();

        // 2. Khởi tạo bộ lọc và phương thức nạp tiền
        initWalletComponents();

        // 3. Cấu hình các cột của TableView
        colTxTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colTxType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colTxAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colTxStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colTxNote.setCellValueFactory(new PropertyValueFactory<>("note"));

        // 4. Load dữ liệu tài chính
        loadWalletBalances();
        loadTransactionHistory();
    }

    private void initWalletComponents() {

        // Khởi tạo ComboBox phương thức nạp tiền
        cmbDepositMethod.getItems().addAll("Nạp qua thẻ cào", "Yêu cầu Admin nạp");

        // Tạo sự kiện tương tác ẩn/hiện Form động cực mượt theo phương thức được chọn
        cmbDepositMethod.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if ("Nạp qua thẻ cào".equals(newValue)) {
                paneCardMethod.setVisible(true);  paneCardMethod.setManaged(true);
                paneAdminMethod.setVisible(false); paneAdminMethod.setManaged(false);
            } else if ("Yêu cầu Admin nạp".equals(newValue)) {
                paneCardMethod.setVisible(false); paneCardMethod.setManaged(false);
                paneAdminMethod.setVisible(true);  paneAdminMethod.setManaged(true);
            }
        });
    }

    private void loadWalletBalances() {
        // Gán chuẩn xác giá trị vào các nhãn không còn lo lỗi NullPointer
        lblCurrentBalance.setText("15,000,000 ₫");
        lblLockedBalance.setText("5,000,000 ₫");
        lblTotalBalance.setText("20,000,000 ₫");
    }

    private void loadTransactionHistory() {
        transactionList = FXCollections.observableArrayList();
        transactionList.add(new Transaction("23/05/2026 14:20", "Nạp tiền", "+10,000,000 ₫", "Thành công", "Nạp tiền qua VNPay"));
        transactionList.add(new Transaction("22/05/2026 09:15", "Đóng băng", "-5,000,000 ₫", "Đang giữ", "Đặt cọc phiên MacBook Pro M3"));
        transactionList.add(new Transaction("20/05/2026 18:00", "Hoàn tiền", "+2,000,000 ₫", "Thành công", "Hoàn cọc phiên iPhone 15"));

        tblTransactions.setItems(transactionList);
    }

    @FXML
    private void handleDepositSubmit() {
        String method = cmbDepositMethod.getValue();
        if (method == null) {
            lblDepositResult.setText("❌ Vui lòng chọn phương thức nạp tiền!");
            return;
        }
        lblDepositResult.setText("✅ Gửi yêu cầu nạp tiền thành công!");
    }

    // Entity Class đóng gói dữ liệu
    public static class Transaction {
        private final String time;
        private final String type;
        private final String amount;
        private final String status;
        private final String note;

        public Transaction(String time, String type, String amount, String status, String note) {
            this.time = time;
            this.type = type;
            this.amount = amount;
            this.status = status;
            this.note = note;
        }

        public String getTime() { return time; }
        public String getType() { return type; }
        public String getAmount() { return amount; }
        public String getStatus() { return status; }
        public String getNote() { return note; }
    }
}