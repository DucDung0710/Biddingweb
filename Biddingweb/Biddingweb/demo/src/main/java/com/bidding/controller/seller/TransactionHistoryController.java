package com.bidding.controller.seller;

import com.bidding.dao.WalletTransactionDAO;
import com.bidding.service.WalletService;
import com.bidding.shared.UserSession;
import com.bidding.util.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Callback;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class TransactionHistoryController implements Initializable {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private TableView<WalletTransactionDAO.WalletTransaction> tblTransactions;
    @FXML private TableColumn<WalletTransactionDAO.WalletTransaction, String> colProduct;
    @FXML private TableColumn<WalletTransactionDAO.WalletTransaction, String> colWinner;
    @FXML private TableColumn<WalletTransactionDAO.WalletTransaction, String> colPrice;
    @FXML private TableColumn<WalletTransactionDAO.WalletTransaction, String> colEndTime;
    @FXML private TableColumn<WalletTransactionDAO.WalletTransaction, String> colStatus;
    @FXML private TableColumn<WalletTransactionDAO.WalletTransaction, Void> colActions;
    @FXML private Label lblTotalCount;
    @FXML private Label lblTotalRevenue;
    @FXML private Label lblPaidCount;
    @FXML private Label lblCanceledCount;

    private final ObservableList<WalletTransactionDAO.WalletTransaction> masterTransactions = FXCollections.observableArrayList();
    private final FilteredList<WalletTransactionDAO.WalletTransaction> filteredTransactions = new FilteredList<>(masterTransactions, p -> true);
    private final WalletService walletService = new WalletService();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0 ₫");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupStatusCombo();
        setupTable();
        loadTransactions();

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterTransactions());
        }
        if (cmbStatus != null) {
            cmbStatus.setOnAction(e -> filterTransactions());
        }
        if (dpFrom != null) {
            dpFrom.setOnAction(e -> filterTransactions());
        }
        if (dpTo != null) {
            dpTo.setOnAction(e -> filterTransactions());
        }
    }

    private void setupStatusCombo() {
        if (cmbStatus != null) {
            cmbStatus.getItems().addAll("Tất cả", "Đã thanh toán", "Đang chờ", "Hủy");
            cmbStatus.getSelectionModel().selectFirst();
        }
    }

    private void setupTable() {
        tblTransactions.setItems(filteredTransactions);
        colProduct.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNote()));
        colWinner.setCellValueFactory(cell -> new SimpleStringProperty(mapTypeToLabel(cell.getValue().getType())));
        colPrice.setCellValueFactory(cell -> new SimpleStringProperty(formatMoney(cell.getValue().getAmount())));
        colEndTime.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatedAt()));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(mapStatus(cell.getValue().getType())));
        colActions.setCellFactory(createActionCellFactory());
    }

    private Callback<TableColumn<WalletTransactionDAO.WalletTransaction, Void>, TableCell<WalletTransactionDAO.WalletTransaction, Void>> createActionCellFactory() {
        return column -> new TableCell<>() {
            private final Button actionButton = new Button("Xem");

            {
                actionButton.setStyle("-fx-background-color: #3B6D11; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 12;");
                actionButton.setOnAction(event -> {
                    WalletTransactionDAO.WalletTransaction tx = getTableView().getItems().get(getIndex());
                    if (tx != null) {
                        System.out.println("Xem chi tiết giao dịch: " + tx.getId() + " - " + tx.getNote());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionButton);
            }
        };
    }

    private void loadTransactions() {
        int userId = UserSession.getInstance().getLoggedInUser() != null ? UserSession.getInstance().getLoggedInUser().getId() : 0;
        var transactions = walletService.getTransactionHistory(userId, 100);
        masterTransactions.setAll(transactions != null ? transactions : List.of());
        filterTransactions();
    }

    @FXML
    private void handleFilter() {
        filterTransactions();
    }

    private void filterTransactions() {
        String keyword = txtSearch != null ? txtSearch.getText().trim().toLowerCase(Locale.ROOT) : "";
        String status = cmbStatus != null ? cmbStatus.getValue() : "Tất cả";
        LocalDate fromDate = dpFrom != null ? dpFrom.getValue() : null;
        LocalDate toDate = dpTo != null ? dpTo.getValue() : null;

        filteredTransactions.setPredicate(tx -> {
            boolean matchesSearch = keyword.isEmpty() || tx.getNote().toLowerCase(Locale.ROOT).contains(keyword) || tx.getType().toLowerCase(Locale.ROOT).contains(keyword);
            boolean matchesStatus = "Tất cả".equals(status) || mapStatus(tx.getType()).equals(status);
            boolean matchesDate = true;
            if (fromDate != null || toDate != null) {
                try {
                    LocalDate txDate = LocalDate.parse(tx.getCreatedAt().substring(0, 10));
                    if (fromDate != null && txDate.isBefore(fromDate)) {
                        matchesDate = false;
                    }
                    if (toDate != null && txDate.isAfter(toDate)) {
                        matchesDate = false;
                    }
                } catch (Exception ignored) {
                    matchesDate = true;
                }
            }
            return matchesSearch && matchesStatus && matchesDate;
        });
        updateSummary();
    }

    private void updateSummary() {
        lblTotalCount.setText(String.valueOf(filteredTransactions.size()));
        double totalRevenue = filteredTransactions.stream().mapToDouble(WalletTransactionDAO.WalletTransaction::getAmount).sum();
        lblTotalRevenue.setText(formatMoney(totalRevenue));
        lblPaidCount.setText(String.valueOf(filteredTransactions.stream().filter(tx -> "Đã thanh toán".equals(mapStatus(tx.getType()))).count()));
        lblCanceledCount.setText(String.valueOf(filteredTransactions.stream().filter(tx -> "Hủy".equals(mapStatus(tx.getType()))).count()));
    }

    private String mapTypeToLabel(String type) {
        return switch (type) {
            case "DEPOSIT" -> "Nạp tiền";
            case "WITHDRAW" -> "Rút tiền";
            case "RECEIVED" -> "Nhận";
            case "PAYMENT" -> "Thanh toán";
            default -> type != null ? type : "Khác";
        };
    }

    private String mapStatus(String type) {
        return switch (type) {
            case "DEPOSIT", "RECEIVED", "PAYMENT" -> "Đã thanh toán";
            case "WITHDRAW" -> "Đang chờ";
            default -> "Đã thanh toán";
        };
    }

    private String formatMoney(double amount) {
        return moneyFormat.format(amount);
    }

    @FXML
    private void handleBack() {
        try {
            SceneManager.switchToSellerDashboard();
        } catch (Exception e) {
            System.err.println("Lỗi chuyển về dashboard: " + e.getMessage());
        }
    }
}