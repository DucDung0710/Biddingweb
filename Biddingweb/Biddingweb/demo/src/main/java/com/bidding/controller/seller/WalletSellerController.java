package com.bidding.controller.seller;

import com.bidding.service.WalletService;
import com.bidding.dao.WalletTransactionDAO;
import com.bidding.shared.UserSession;
import com.bidding.util.SessionStore;
import com.bidding.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class WalletSellerController implements Initializable {

    // ─── FXML Injections ────────────────────────────────────────────────────
    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;

    // Balance cards
    @FXML private Label lblCurrentBalance;
    @FXML private Label lblLockedBalance;
    @FXML private Label lblTotalRevenue;

    // Tab buttons
    @FXML private Button btnTabDeposit;
    @FXML private Button btnTabWithdraw;

    // Deposit pane
    @FXML private VBox paneDeposit;
    @FXML private ComboBox<String> cmbDepositMethod;
    @FXML private VBox paneCardMethod;
    @FXML private VBox paneAdminMethod;
    @FXML private TextField txtCardCode;
    @FXML private TextField txtDepositAmount;
    @FXML private Button btnDeposit;

    // Withdraw pane
    @FXML private VBox paneWithdraw;
    @FXML private TextField txtWithdrawAmount;
    @FXML private TextField txtBankAccount;
    @FXML private Button btnWithdraw;

    // Shared result label
    @FXML private Label lblActionResult;

    // Transaction table
    @FXML private TableView<TransactionRow> tblTransactions;
    @FXML private TableColumn<TransactionRow, String> colTxTime;
    @FXML private TableColumn<TransactionRow, String> colTxType;
    @FXML private TableColumn<TransactionRow, String> colTxAmount;
    @FXML private TableColumn<TransactionRow, String> colTxStatus;
    @FXML private TableColumn<TransactionRow, String> colTxNote;
    @FXML private ComboBox<String> cmbTxFilter;

    // Navigation
    @FXML private HBox navDashboard;
    @FXML private HBox btnLogout;

    // ─── State ──────────────────────────────────────────────────────────────
    private WalletService walletService = new WalletService();
    private int currentUserId;
    private double totalRevenue = 0;

    private final ObservableList<TransactionRow> txList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentUserId = UserSession.getInstance().getLoggedInUser().getId();
        lblUserName.setText(UserSession.getInstance().getLoggedInUser().getUsername());
        lblUserRole.setText("Seller");

        setupTable();
        setupDepositCombo();
        setupTxFilterCombo();
        setupTabs();
        setupNavigation();

        cmbDepositMethod.valueProperty().addListener((obs, o, newVal) -> handleMethodChange(newVal));
        btnDeposit.setOnAction(e -> handleDeposit());
        btnWithdraw.setOnAction(e -> handleWithdraw());

        loadWalletData();
    }

    private void setupTable() {
        colTxTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colTxType.setCellValueFactory(new PropertyValueFactory<>("typeName"));
        colTxAmount.setCellValueFactory(new PropertyValueFactory<>("amountStr"));
        colTxStatus.setCellValueFactory(new PropertyValueFactory<>("statusName"));
        colTxNote.setCellValueFactory(new PropertyValueFactory<>("note"));
        tblTransactions.setItems(txList);
        tblTransactions.setPlaceholder(new Label("Chưa có giao dịch nào"));
    }

    private void setupDepositCombo() {
        cmbDepositMethod.setItems(FXCollections.observableArrayList(
                "💳 Yêu cầu Admin nạp tiền"
        ));
    }

    private void setupTxFilterCombo() {
        cmbTxFilter.setItems(FXCollections.observableArrayList(
                "Tất cả", "Nạp tiền", "Rút tiền"
        ));
        cmbTxFilter.setValue("Tất cả");
    }

    private void setupTabs() {
        // Tab Nạp tiền mặc định active
        setActiveTab(true);

        btnTabDeposit.setOnAction(e -> setActiveTab(true));
        btnTabWithdraw.setOnAction(e -> setActiveTab(false));
    }

    private void setActiveTab(boolean isDeposit) {
        paneDeposit.setVisible(isDeposit);
        paneDeposit.setManaged(isDeposit);
        paneWithdraw.setVisible(!isDeposit);
        paneWithdraw.setManaged(!isDeposit);

        String activeStyle = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: white; -fx-background-radius: 6; -fx-padding: 6 0; -fx-cursor: hand;";
        String inactiveStyle = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: transparent; -fx-text-fill: #888888; -fx-background-radius: 6; -fx-padding: 6 0; -fx-cursor: hand;";

        if (isDeposit) {
            btnTabDeposit.setStyle(activeStyle + " -fx-text-fill: #3B6D11;");
            btnTabWithdraw.setStyle(inactiveStyle);
        } else {
            btnTabDeposit.setStyle(inactiveStyle);
            btnTabWithdraw.setStyle(activeStyle + " -fx-text-fill: #854F0B;");
        }
        lblActionResult.setText("");
    }

    private void setupNavigation() {
        navDashboard.setOnMouseClicked(e -> SceneManager.switchToSellerDashboard());
        btnLogout.setOnMouseClicked(e -> SceneManager.switchToLogin());
    }

    private void handleMethodChange(String selected) {
        if (selected == null) return;
        boolean isAdmin = selected.contains("Admin");
        paneCardMethod.setVisible(false);
        paneCardMethod.setManaged(false);
        paneAdminMethod.setVisible(isAdmin);
        paneAdminMethod.setManaged(isAdmin);
    }

    private void handleDeposit() {
        String method = cmbDepositMethod.getValue();
        if (method == null) {
            showResult("⚠ Chọn phương thức.", false);
            return;
        }

        String amtStr = txtDepositAmount.getText().trim();
        if (amtStr.isEmpty()) {
            showResult("⚠ Nhập số tiền.", false);
            return;
        }
        try {
            double amount = Double.parseDouble(amtStr.replace(",", ""));
            if (amount <= 0) throw new NumberFormatException();
            
            if (walletService.createDepositRequest(currentUserId, amount)) {
                txList.add(0, new TransactionRow("DEPOSIT", amount, "Chờ duyệt", "Yêu cầu nạp " + fmt(amount)));
                txtDepositAmount.clear();
                loadWalletData();
                showResult("📨 Yêu cầu nạp " + fmt(amount) + " đã gửi.", true);
            } else {
                showResult("❌ Không thể tạo yêu cầu nạp tiền.", false);
            }
        } catch (NumberFormatException ex) {
            showResult("⚠ Số tiền không hợp lệ.", false);
        }
    }

    private void handleWithdraw() {
        String amtStr = txtWithdrawAmount.getText().trim();
        String account = txtBankAccount.getText().trim();

        if (amtStr.isEmpty()) {
            showResult("⚠ Nhập số tiền cần rút.", false);
            return;
        }
        if (account.isEmpty()) {
            showResult("⚠ Nhập số tài khoản.", false);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amtStr.replace(",", ""));
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showResult("⚠ Số tiền không hợp lệ.", false);
            return;
        }

        double currentBalance = walletService.getBalance(currentUserId);
        if (currentBalance < amount) {
            showResult("❌ Số dư không đủ. Khả dụng: " + fmt(currentBalance), false);
            return;
        }

        if (walletService.withdraw(currentUserId, amount, "Rút tiền sang → TK: " + account)) {
            txList.add(0, new TransactionRow("WITHDRAW", amount, "Chờ xử lý", "→ TK: " + account));
            txtWithdrawAmount.clear();
            txtBankAccount.clear();
            loadWalletData();
            showResult("📤 Yêu cầu rút " + fmt(amount) + " đã gửi. Xử lý trong 1-3 ngày làm việc.", true);
        } else {
            showResult("❌ Không thể tạo yêu cầu rút tiền.", false);
        }
    }

    private void loadWalletData() {
        double balance = walletService.getBalance(currentUserId);
        lblCurrentBalance.setText(fmt(balance));
        lblLockedBalance.setText("0 ₫");
        lblTotalRevenue.setText(fmt(totalRevenue));

        txList.clear();
        List<WalletTransactionDAO.WalletTransaction> txs = walletService.getTransactionHistory(currentUserId, 20);
        for (WalletTransactionDAO.WalletTransaction tx : txs) {
            txList.add(new TransactionRow(tx.getType(), tx.getAmount(), "Thành công", tx.getNote()));
        }
    }

    private void showResult(String msg, boolean success) {
        lblActionResult.setText(msg);
        lblActionResult.setStyle(success
                ? "-fx-text-fill: #3B6D11; -fx-font-size: 12px;"
                : "-fx-text-fill: #A32D2D; -fx-font-size: 12px;");
    }

    private String fmt(double amount) {
        return String.format("%,.0f ₫", amount);
    }

    public static class TransactionRow {
        private String type;
        private double amount;
        private String status;
        private String note;

        public TransactionRow(String type, double amount, String status, String note) {
            this.type = type;
            this.amount = amount;
            this.status = status;
            this.note = note;
        }

        public String getTypeName() {
            return switch (type) {
                case "DEPOSIT" -> "Nạp tiền";
                case "WITHDRAW" -> "Rút tiền";
                case "RECEIVED" -> "Nhận doanh thu";
                case "PAYMENT" -> "Thanh toán";
                default -> type;
            };
        }

        public String getAmountStr() {
            return String.format("%,.0f ₫", amount);
        }

        public String getStatusName() {
            return status;
        }

        public String getTime() {
            return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        public String getNote() {
            return note;
        }
    }
}
