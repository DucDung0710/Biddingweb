package com.bidding.controller.seller;

import com.bidding.shared.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller cho màn hình Ví tiền của Seller.
 * File FXML: wallet_seller.fxml
 *
 * Chức năng:
 * - Hiển thị số dư, số tiền chờ thanh toán, tổng doanh thu
 * - Tab Nạp tiền (thẻ cào hoặc Admin)
 * - Tab Rút tiền (trừ số dư, ghi lịch sử)
 * - Lịch sử giao dịch
 */
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
    @FXML private VBox      paneDeposit;
    @FXML private ComboBox<String> cmbDepositMethod;
    @FXML private VBox      paneCardMethod;
    @FXML private VBox      paneAdminMethod;
    @FXML private TextField txtCardCode;
    @FXML private TextField txtDepositAmount;
    @FXML private Button    btnDeposit;

    // Withdraw pane
    @FXML private VBox      paneWithdraw;
    @FXML private TextField txtWithdrawAmount;
    @FXML private TextField txtBankAccount;
    @FXML private Button    btnWithdraw;

    // Shared result label
    @FXML private Label lblActionResult;

    // Transaction table
    @FXML private TableView<TransactionRecord>          tblTransactions;
    @FXML private TableColumn<TransactionRecord, String>  colTxTime;
    @FXML private TableColumn<TransactionRecord, String>  colTxType;
    @FXML private TableColumn<TransactionRecord, String>  colTxAmount;
    @FXML private TableColumn<TransactionRecord, String>  colTxStatus;
    @FXML private TableColumn<TransactionRecord, String>  colTxNote;
    @FXML private ComboBox<String>                        cmbTxFilter;

    // Navigation
    @FXML private HBox navDashboard;
    @FXML private HBox btnLogout;

    // ─── State ──────────────────────────────────────────────────────────────
    private WalletManager walletManager;
    private Users         currentUser;
    private CardMethod    cardMethod  = new CardMethod();
    private AdminMethod   adminMethod = new AdminMethod();

    /** Tổng doanh thu tích lũy (nên lấy từ DB trong dự án thực) */
    private double totalRevenue = 0;

    private final ObservableList<TransactionRecord> txList =
            FXCollections.observableArrayList();

    // ─── Initializable ──────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupDepositCombo();
        setupTxFilterCombo();
        setupTabs();
        setupNavigation();

        cmbDepositMethod.valueProperty().addListener((obs, o, newVal) ->
                handleMethodChange(newVal));
        btnDeposit .setOnAction(e -> handleDeposit());
        btnWithdraw.setOnAction(e -> handleWithdraw());
    }

    // ─── Public API ──────────────────────────────────────────────────────────
    /**
     * Inject dữ liệu từ bên ngoài trước khi show màn hình.
     * Ví dụ trong MainController:
     * WalletSellerController ctrl = loader.getController();
     * ctrl.initData(walletManager, currentUser);
     */
    public void initData(WalletManager walletManager, Users currentUser) {
        this.walletManager = walletManager;
        this.currentUser   = currentUser;

        lblUserName.setText(currentUser.getName());
        lblUserRole.setText("Seller");
        refreshBalance();
    }

    /**
     * Gọi từ bên ngoài khi Seller nhận được tiền sau phiên đấu giá thành công.
     * AuctionController sẽ gọi hàm này để cộng doanh thu vào ví Seller.
     */
    public void receivePayment(double amount, String auctionNote) {
        if (walletManager == null || currentUser == null) return;
        walletManager.depositDirectly(currentUser.getId(), amount);
        totalRevenue += amount;
        txList.add(0, new TransactionRecord(
                TransactionRecord.Type.RECEIVE, amount,
                TransactionRecord.Status.SUCCESS, auctionNote));
        refreshBalance();
    }

    // ─── Setup ───────────────────────────────────────────────────────────────

    private void setupTable() {
        colTxTime  .setCellValueFactory(new PropertyValueFactory<>("time"));
        colTxType  .setCellValueFactory(new PropertyValueFactory<>("typeName"));
        colTxAmount.setCellValueFactory(new PropertyValueFactory<>("amountStr"));
        colTxStatus.setCellValueFactory(new PropertyValueFactory<>("statusName"));
        colTxNote  .setCellValueFactory(new PropertyValueFactory<>("note"));
        tblTransactions.setItems(txList);
        tblTransactions.setPlaceholder(new Label("Chưa có giao dịch nào"));

        // Màu cho cột trạng thái
        colTxStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "Thành công" -> setStyle("-fx-text-fill: #3B6D11; -fx-font-weight: bold;");
                    case "Chờ duyệt"  -> setStyle("-fx-text-fill: #854F0B; -fx-font-weight: bold;");
                    case "Từ chối"    -> setStyle("-fx-text-fill: #A32D2D; -fx-font-weight: bold;");
                    default           -> setStyle("");
                }
            }
        });
    }

    private void setupDepositCombo() {
        cmbDepositMethod.setItems(FXCollections.observableArrayList(
                "💳 Thẻ cào (nạp ngay)",
                "🏦 Chuyển khoản / Yêu cầu Admin duyệt"
        ));
    }

    private void setupTxFilterCombo() {
        cmbTxFilter.setItems(FXCollections.observableArrayList(
                "Tất cả", "Nạp tiền", "Rút tiền", "Nhận doanh thu"
        ));
        cmbTxFilter.setValue("Tất cả");
    }

    private void setupTabs() {
        // Tab Nạp tiền mặc định active
        setActiveTab(true);

        btnTabDeposit .setOnAction(e -> setActiveTab(true));
        btnTabWithdraw.setOnAction(e -> setActiveTab(false));
    }

    private void setActiveTab(boolean isDeposit) {
        paneDeposit .setVisible(isDeposit);  paneDeposit .setManaged(isDeposit);
        paneWithdraw.setVisible(!isDeposit); paneWithdraw.setManaged(!isDeposit);

        String activeStyle =
                "-fx-font-size: 12px; -fx-font-weight: bold;" +
                        "-fx-background-color: white; -fx-background-radius: 6;" +
                        "-fx-padding: 6 0; -fx-cursor: hand;";
        String inactiveStyle =
                "-fx-font-size: 12px; -fx-font-weight: bold;" +
                        "-fx-background-color: transparent; -fx-text-fill: #888888;" +
                        "-fx-background-radius: 6; -fx-padding: 6 0; -fx-cursor: hand;";

        if (isDeposit) {
            btnTabDeposit .setStyle(activeStyle + " -fx-text-fill: #3B6D11;");
            btnTabWithdraw.setStyle(inactiveStyle);
        } else {
            btnTabDeposit .setStyle(inactiveStyle);
            btnTabWithdraw.setStyle(activeStyle + " -fx-text-fill: #854F0B;");
        }
        lblActionResult.setText("");
    }

    private void setupNavigation() {
        navDashboard.setOnMouseClicked(e -> navigateTo("dashboard"));
        btnLogout   .setOnMouseClicked(e -> navigateTo("logout"));
    }

    // ─── Event Handlers ──────────────────────────────────────────────────────

    private void handleMethodChange(String selected) {
        if (selected == null) return;
        boolean isCard  = selected.startsWith("💳");
        boolean isAdmin = selected.startsWith("🏦");
        paneCardMethod .setVisible(isCard);  paneCardMethod .setManaged(isCard);
        paneAdminMethod.setVisible(isAdmin); paneAdminMethod.setManaged(isAdmin);
    }

    private void handleDeposit() {
        if (walletManager == null || currentUser == null) {
            showResult("⚠ Chưa khởi tạo.", false); return;
        }
        String method = cmbDepositMethod.getValue();
        if (method == null) { showResult("⚠ Chọn phương thức.", false); return; }

        if (method.startsWith("💳")) {
            // Thẻ cào
            String code = txtCardCode.getText().trim().toUpperCase();
            if (code.isEmpty()) { showResult("⚠ Nhập mã thẻ.", false); return; }
            double before = getWallet().getAmount();
            cardMethod.topUpWithCode(currentUser.getId(), code, walletManager);
            double added = getWallet().getAmount() - before;
            if (added > 0) {
                txList.add(0, new TransactionRecord(TransactionRecord.Type.DEPOSIT_CARD,
                        added, TransactionRecord.Status.SUCCESS, "Mã: " + code));
                txtCardCode.clear();
                refreshBalance();
                showResult("✅ Nạp thành công " + fmt(added), true);
            } else {
                showResult("❌ Mã thẻ không hợp lệ hoặc đã dùng.", false);
            }
        } else {
            // Admin
            String amtStr = txtDepositAmount.getText().trim();
            if (amtStr.isEmpty()) { showResult("⚠ Nhập số tiền.", false); return; }
            try {
                double amount = Double.parseDouble(amtStr.replace(",", ""));
                if (amount <= 0) throw new NumberFormatException();
                walletManager.execute(currentUser.getId(), amount, adminMethod);
                txList.add(0, new TransactionRecord(TransactionRecord.Type.DEPOSIT_ADMIN,
                        amount, TransactionRecord.Status.PENDING, "Chờ Admin duyệt"));
                txtDepositAmount.clear();
                showResult("📨 Yêu cầu nạp " + fmt(amount) + " đã gửi.", true);
            } catch (NumberFormatException ex) {
                showResult("⚠ Số tiền không hợp lệ.", false);
            }
        }
    }

    private void handleWithdraw() {
        if (walletManager == null || currentUser == null) {
            showResult("⚠ Chưa khởi tạo.", false); return;
        }

        String amtStr  = txtWithdrawAmount.getText().trim();
        String account = txtBankAccount.getText().trim();

        if (amtStr.isEmpty()) { showResult("⚠ Nhập số tiền cần rút.", false); return; }
        if (account.isEmpty()) { showResult("⚠ Nhập số tài khoản.", false); return; }

        double amount;
        try {
            amount = Double.parseDouble(amtStr.replace(",", ""));
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showResult("⚠ Số tiền không hợp lệ.", false); return;
        }

        Balance wallet = getWallet();
        if (wallet == null) { showResult("⚠ Không tìm thấy ví.", false); return; }

        boolean ok = wallet.withdraw(amount);
        if (ok) {
            txList.add(0, new TransactionRecord(TransactionRecord.Type.WITHDRAW,
                    amount, TransactionRecord.Status.PENDING,
                    "→ TK: " + account));
            txtWithdrawAmount.clear();
            txtBankAccount.clear();
            refreshBalance();
            showResult("📤 Yêu cầu rút " + fmt(amount) + " đã gửi. Xử lý trong 1-3 ngày làm việc.", true);
        } else {
            showResult("❌ Số dư không đủ. Khả dụng: " + fmt(wallet.getAmount()), false);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void refreshBalance() {
        Balance w = getWallet();
        if (w == null) return;
        lblCurrentBalance.setText(fmt(w.getAmount()));
        lblLockedBalance .setText(fmt(w.getLockedAmount()));
        lblTotalRevenue  .setText(fmt(totalRevenue));
    }

    private Balance getWallet() {
        if (walletManager == null || currentUser == null) return null;
        return walletManager.getWalletByUserId(currentUser.getId());
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

    private void navigateTo(String dest) {
        System.out.println("[SellerNav] Chuyển đến: " + dest);
        // TODO: SceneManager.getInstance().switchTo(dest);
    }
}