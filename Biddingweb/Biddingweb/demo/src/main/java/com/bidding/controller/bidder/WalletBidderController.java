//package com.bidding.controller.bidder;
//
//import com.bidding.shared.*;
//import com.bidding.util.SessionStore;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.scene.control.*;
//import javafx.scene.control.cell.PropertyValueFactory;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.VBox;
//
//import java.net.URL;
//import java.util.ResourceBundle;
//
///**
// * Controller cho màn hình Ví tiền của Bidder.
// * File FXML: wallet_bidder.fxml
// *
// * Chức năng:
// *  - Hiển thị số dư khả dụng, bị khóa, tổng tài sản
// *  - Nạp tiền qua thẻ cào (CardMethod) hoặc xin Admin duyệt (AdminMethod)
// *  - Hiển thị lịch sử giao dịch
// */
//public class WalletBidderController implements Initializable {
//
//    // ─── FXML Injections ────────────────────────────────────────────────────
//    @FXML private Label lblUserName;
//    @FXML private Label lblUserRole;
//
//    // Balance cards
//    @FXML private Label lblCurrentBalance;
//    @FXML private Label lblLockedBalance;
//    @FXML private Label lblTotalBalance;
//
//    // Deposit form
//    @FXML private ComboBox<String>  cmbDepositMethod;
//    @FXML private VBox              paneCardMethod;   // hiển thị khi chọn thẻ cào
//    @FXML private VBox              paneAdminMethod;  // hiển thị khi chọn Admin
//    @FXML private TextField         txtCardCode;
//    @FXML private TextField         txtDepositAmount;
//    @FXML private Button            btnDeposit;
//    @FXML private Label             lblDepositResult;
//
//    // Transaction table
//    @FXML private TableView<TransactionRecord>       tblTransactions;
//    @FXML private TableColumn<TransactionRecord, String> colTxTime;
//    @FXML private TableColumn<TransactionRecord, String> colTxType;
//    @FXML private TableColumn<TransactionRecord, String> colTxAmount;
//    @FXML private TableColumn<TransactionRecord, String> colTxStatus;
//    @FXML private TableColumn<TransactionRecord, String> colTxNote;
//    @FXML private ComboBox<String>                   cmbTxFilter;
//
//    // Navigation
//    @FXML private HBox navDashboard;
//    @FXML private HBox navAuctions;
//    @FXML private HBox btnLogout;
//
//    // ─── State ──────────────────────────────────────────────────────────────
//    /** Được set từ bên ngoài trước khi load scene (ví dụ từ LoginController) */
//    private WalletManager walletManager;
//    private Users         currentUser;
//    private CardMethod    cardMethod  = new CardMethod();
//    private AdminMethod   adminMethod = new AdminMethod();
//
//    private final ObservableList<TransactionRecord> txList =
//            FXCollections.observableArrayList();
//
//    // ─── Initializable ──────────────────────────────────────────────────────
//    @Override
//    public void initialize(URL url, ResourceBundle rb) {
//        setupTable();
//        setupDepositMethodCombo();
//        setupTxFilterCombo();
//        setupNavigation();
//
//        // Lắng nghe thay đổi ComboBox phương thức nạp
//        cmbDepositMethod.valueProperty().addListener((obs, oldVal, newVal) ->
//                handleMethodChange(newVal));
//
//        btnDeposit.setOnAction(e -> handleDeposit());
//    }
//
//    // ─── Public API (gọi từ ngoài để inject dữ liệu) ────────────────────────
//
//    /**
//     * Gọi hàm này sau khi load FXML, trước khi show Scene.
//     * Ví dụ trong LoginController hoặc MainController:
//     *   WalletBidderController ctrl = loader.getController();
//     *   ctrl.initData(walletManager, currentUser);
//     */
//    public void initData(WalletManager walletManager, Users currentUser) {
//        this.walletManager = walletManager;
//        this.currentUser   = currentUser;
//
//        lblUserName.setText(currentUser.getName());
//        lblUserRole.setText("Bidder");
//
//        refreshBalance();
//    }
//
//    // ─── Setup helpers ───────────────────────────────────────────────────────
//
//    private void setupTable() {
//        colTxTime  .setCellValueFactory(new PropertyValueFactory<>("time"));
//        colTxType  .setCellValueFactory(new PropertyValueFactory<>("typeName"));
//        colTxAmount.setCellValueFactory(new PropertyValueFactory<>("amountStr"));
//        colTxStatus.setCellValueFactory(new PropertyValueFactory<>("statusName"));
//        colTxNote  .setCellValueFactory(new PropertyValueFactory<>("note"));
//        tblTransactions.setItems(txList);
//        tblTransactions.setPlaceholder(new Label("Chưa có giao dịch nào"));
//
//        // Tô màu cột trạng thái
//        colTxStatus.setCellFactory(col -> new TableCell<>() {
//            @Override
//            protected void updateItem(String item, boolean empty) {
//                super.updateItem(item, empty);
//                if (empty || item == null) { setText(null); setStyle(""); return; }
//                setText(item);
//                switch (item) {
//                    case "Thành công" -> setStyle("-fx-text-fill: #3B6D11; -fx-font-weight: bold;");
//                    case "Chờ duyệt"  -> setStyle("-fx-text-fill: #854F0B; -fx-font-weight: bold;");
//                    case "Từ chối"    -> setStyle("-fx-text-fill: #A32D2D; -fx-font-weight: bold;");
//                    default           -> setStyle("");
//                }
//            }
//        });
//    }
//
//    private void setupDepositMethodCombo() {
//        cmbDepositMethod.setItems(FXCollections.observableArrayList(
//                "💳 Thẻ cào (nạp ngay)",
//                "🏦 Chuyển khoản / Yêu cầu Admin duyệt"
//        ));
//    }
//
//    private void setupTxFilterCombo() {
//        cmbTxFilter.setItems(FXCollections.observableArrayList(
//                "Tất cả", "Nạp tiền", "Khóa / Mở khóa", "Thanh toán"
//        ));
//        cmbTxFilter.setValue("Tất cả");
//        cmbTxFilter.valueProperty().addListener((obs, o, newVal) -> applyTxFilter(newVal));
//    }
//
//    private void setupNavigation() {
//        // Kết nối điều hướng — thay thế bằng SceneManager thực tế của dự án
//        navDashboard.setOnMouseClicked(e -> navigateTo("dashboard"));
//        navAuctions .setOnMouseClicked(e -> navigateTo("auctions"));
//        btnLogout   .setOnMouseClicked(e -> navigateTo("logout"));
//    }
//
//    // ─── Event Handlers ──────────────────────────────────────────────────────
//
//    private void handleMethodChange(String selected) {
//        if (selected == null) return;
//        boolean isCard  = selected.startsWith("💳");
//        boolean isAdmin = selected.startsWith("🏦");
//
//        paneCardMethod .setVisible(isCard);  paneCardMethod .setManaged(isCard);
//        paneAdminMethod.setVisible(isAdmin); paneAdminMethod.setManaged(isAdmin);
//        lblDepositResult.setText("");
//    }
//
//    private void handleDeposit() {
//        if (walletManager == null || currentUser == null) {
//            showResult("⚠ Chưa khởi tạo dữ liệu ví.", false);
//            return;
//        }
//
//        String method = cmbDepositMethod.getValue();
//        if (method == null) {
//            showResult("⚠ Vui lòng chọn phương thức nạp tiền.", false);
//            return;
//        }
//
//        if (method.startsWith("💳")) {
//            handleCardDeposit();
//        } else {
//            handleAdminDeposit();
//        }
//    }
//
//    /** Nạp qua thẻ cào — cộng tiền ngay */
//    private void handleCardDeposit() {
//        String code = txtCardCode.getText().trim().toUpperCase();
//        if (code.isEmpty()) {
//            showResult("⚠ Vui lòng nhập mã thẻ.", false);
//            return;
//        }
//
//        double balanceBefore = getWallet().getAmount();
//        cardMethod.topUpWithCode(currentUser.getId(), code, walletManager);
//        double balanceAfter  = getWallet().getAmount();
//        double added = balanceAfter - balanceBefore;
//
//        if (added > 0) {
//            txList.add(0, new TransactionRecord(
//                    TransactionRecord.Type.DEPOSIT_CARD,
//                    added,
//                    TransactionRecord.Status.SUCCESS,
//                    "Mã thẻ: " + code));
//            txtCardCode.clear();
//            refreshBalance();
//            showResult("✅ Nạp thành công " + fmt(added) + " vào ví!", true);
//        } else {
//            showResult("❌ Mã thẻ không hợp lệ hoặc đã sử dụng.", false);
//        }
//    }
//
//    /** Nạp qua Admin — tạo phiếu chờ */
//    private void handleAdminDeposit() {
//        String amtStr = txtDepositAmount.getText().trim();
//        if (amtStr.isEmpty()) {
//            showResult("⚠ Vui lòng nhập số tiền.", false);
//            return;
//        }
//        double amount;
//        try {
//            amount = Double.parseDouble(amtStr.replace(",", ""));
//        } catch (NumberFormatException e) {
//            showResult("⚠ Số tiền không hợp lệ.", false);
//            return;
//        }
//        if (amount <= 0) {
//            showResult("⚠ Số tiền phải lớn hơn 0.", false);
//            return;
//        }
//
//        walletManager.execute(currentUser.getId(), amount, adminMethod);
//        txList.add(0, new TransactionRecord(
//                TransactionRecord.Type.DEPOSIT_ADMIN,
//                amount,
//                TransactionRecord.Status.PENDING,
//                "Chờ Admin duyệt"));
//        txtDepositAmount.clear();
//        showResult("📨 Yêu cầu nạp " + fmt(amount) + " đã gửi. Vui lòng chờ Admin xét duyệt.", true);
//    }
//
//    // ─── Balance & display helpers ───────────────────────────────────────────
//
//    private void refreshBalance() {
//        Balance w = getWallet();
//        if (w == null) return;
//        double current = w.getAmount();
//        double locked  = w.getLockedAmount();
//        lblCurrentBalance.setText(fmt(current));
//        lblLockedBalance .setText(fmt(locked));
//        lblTotalBalance  .setText(fmt(current + locked));
//    }
//
//    private void applyTxFilter(String filter) {
//        // Trong dự án thực tế: lọc txList theo loại giao dịch
//        // Ở đây giữ đơn giản — bạn có thể wrap thêm FilteredList
//    }
//
//    private Balance getWallet() {
//        if (walletManager == null || currentUser == null) return null;
//        return walletManager.getWalletByUserId(currentUser.getId());
//    }
//
//    private void showResult(String msg, boolean success) {
//        lblDepositResult.setText(msg);
//        lblDepositResult.setStyle(success
//                ? "-fx-text-fill: #3B6D11; -fx-font-size: 12px;"
//                : "-fx-text-fill: #A32D2D; -fx-font-size: 12px;");
//    }
//
//    private String fmt(double amount) {
//        return String.format("%,.0f ₫", amount);
//    }
//
//    // ─── Navigation stub ─────────────────────────────────────────────────────
//    /**
//     * Thay thế bằng SceneManager thực tế của dự án.
//     * Ví dụ: SceneManager.getInstance().switchTo(destination);
//     */
//    private void navigateTo(String destination) {
//        System.out.println("[Nav] Chuyển đến: " + destination);
//        // TODO: gọi SceneManager hoặc MainController để đổi scene
//    }
//}
