package com.bidding.controller.bidder;

import com.bidding.service.WalletService;
import com.bidding.dao.WalletTransactionDAO;
import com.bidding.shared.UserSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.List;

public class WalletBidderController extends BaseBidderController {

    @FXML private Label lblCurrentBalance;
    @FXML private Label lblLockedBalance;
    @FXML private Label lblTotalBalance;

    @FXML private ComboBox<String> cmbDepositMethod;
    @FXML private VBox paneCardMethod;
    @FXML private VBox paneAdminMethod;
    @FXML private TextField txtCardCode;
    @FXML private TextField txtDepositAmount;
    @FXML private Label lblDepositResult;
    @FXML private Button btnDeposit;

    @FXML private TableView<TransactionRow> tblTransactions;
    @FXML private TableColumn<TransactionRow, String> colTxTime;
    @FXML private TableColumn<TransactionRow, String> colTxType;
    @FXML private TableColumn<TransactionRow, String> colTxAmount;
    @FXML private TableColumn<TransactionRow, String> colTxStatus;
    @FXML private TableColumn<TransactionRow, String> colTxNote;

    private WalletService walletService = new WalletService();
    private int currentUserId;

    @FXML
    public void initialize() {
        // 1. Kế thừa hành vi Sidebar từ lớp cha
        super.setupSidebarBehavior();

        // 2. Lấy user hiện tại từ Session
        currentUserId = UserSession.getInstance().getLoggedInUser().getId();
        // 3. Khởi tạo bộ lọc và phương thức nạp tiền
        initWalletComponents();

        // 4. Cấu hình các cột của TableView
        colTxTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colTxType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colTxAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colTxStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colTxNote.setCellValueFactory(new PropertyValueFactory<>("note"));

        // 5. Load dữ liệu tài chính
        loadWalletBalances();
        loadTransactionHistory();

        // 6. Setup deposit button
        btnDeposit.setOnAction(e -> handleDepositSubmit());
    }

    private void initWalletComponents() {
        // Khởi tạo ComboBox phương thức nạp tiền
        cmbDepositMethod.getItems().addAll("Yêu cầu Admin nạp");

        // Tạo sự kiện tương tác ẩn/hiện Form động
        cmbDepositMethod.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if ("Yêu cầu Admin nạp".equals(newValue)) {
                paneCardMethod.setVisible(false); paneCardMethod.setManaged(false);
                paneAdminMethod.setVisible(true);  paneAdminMethod.setManaged(true);
            }
        });
    }

    private void loadWalletBalances() {
        // 1. Lấy số dư khả dụng hiện tại từ DB
        double availableBalance = walletService.getBalance(currentUserId);

        // 2. Tính toán số tiền đang bị khóa từ lịch sử giao dịch
        List<WalletTransactionDAO.WalletTransaction> transactions = walletService.getTransactionHistory(currentUserId, 100);
        double lockedBalance = 0;

        for (WalletTransactionDAO.WalletTransaction tx : transactions) {
            if ("HOLD".equals(tx.getType())) {
                lockedBalance += tx.getAmount();
            } else if ("RELEASE".equals(tx.getType())) {
                lockedBalance -= tx.getAmount();
            }
        }

        // 3. Tổng tài sản = Khả dụng + Bị khóa
        double totalBalance = availableBalance + lockedBalance;

        // 4. Hiển thị lên giao diện
        lblCurrentBalance.setText(String.format("%,.0f ₫", availableBalance));
        lblLockedBalance.setText(String.format("%,.0f ₫", lockedBalance));
        lblTotalBalance.setText(String.format("%,.0f ₫", totalBalance));
    }

    private void loadTransactionHistory() {
        List<WalletTransactionDAO.WalletTransaction> transactions = walletService.getTransactionHistory(currentUserId, 20);

        ObservableList<TransactionRow> items = FXCollections.observableArrayList();
        for (WalletTransactionDAO.WalletTransaction tx : transactions) {
            items.add(new TransactionRow(tx.getCreatedAt(), tx.getType(), tx.getAmount(), "Thành công", tx.getNote()));
        }
        tblTransactions.setItems(items);
    }

    @FXML
    private void handleDepositSubmit() {
        String method = cmbDepositMethod.getValue();
        if (method == null) {
            showResult("❌ Vui lòng chọn phương thức nạp tiền!", false);
            return;
        }

        String amtStr = txtDepositAmount.getText().trim();
        if (amtStr.isEmpty()) {
            showResult("❌ Vui lòng nhập số tiền!", false);
            return;
        }

        try {
            double amount = Double.parseDouble(amtStr.replace(",", ""));
            if (amount <= 0) {
                showResult("❌ Số tiền phải lớn hơn 0!", false);
                return;
            }

            if (walletService.createDepositRequest(currentUserId, amount)) {
                showResult("✅ Đã gửi yêu cầu nạp " + String.format("%,.0f ₫", amount) + " cho Admin duyệt!", true);
                txtDepositAmount.clear();
                loadWalletBalances();
                loadTransactionHistory();
            } else {
                showResult("❌ Không thể tạo yêu cầu nạp tiền!", false);
            }
        } catch (NumberFormatException ex) {
            showResult("❌ Số tiền không hợp lệ!", false);
        }
    }

    private void showResult(String msg, boolean success) {
        lblDepositResult.setText(msg);
        lblDepositResult.setStyle(success
                ? "-fx-text-fill: #3B6D11; -fx-font-size: 12px;"
                : "-fx-text-fill: #A32D2D; -fx-font-size: 12px;");
    }

    public static class TransactionRow {
        private String time;
        private String type;
        private double amount;
        private String status;
        private String note;

        public TransactionRow(String time, String type, double amount, String status, String note) {
            this.time = time;
            this.type = type;
            this.amount = amount;
            this.status = status;
            this.note = note;
        }

        public String getTime() { return time; }
        public String getType() { return getTypeDisplay(type); }
        public String getAmount() { return String.format("%,.0f ₫", amount); }
        public String getStatus() { return status; }
        public String getNote() { return note; }

        private static String getTypeDisplay(String type) {
            return switch (type) {
                case "DEPOSIT" -> "Nạp tiền";
                case "WITHDRAW" -> "Rút tiền";
                case "HOLD" -> "Tạm giữ";
                case "RELEASE" -> "Giải phóng";
                case "PAYMENT" -> "Thanh toán";
                default -> type;
            };
        }
    }
}