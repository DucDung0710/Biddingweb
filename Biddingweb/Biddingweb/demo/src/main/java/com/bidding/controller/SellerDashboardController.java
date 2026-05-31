package com.bidding.controller;

import com.bidding.app.AppInitializer;
import com.bidding.shared.Item;
import com.bidding.shared.UserSession;
import com.bidding.shared.Users;
import com.bidding.util.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class SellerDashboardController implements Initializable {

    @FXML
    private Label lblUserName;
    @FXML
    private Label lblUserRole;
    @FXML
    private Label lblStatActive;
    @FXML
    private Label lblStatFinished;
    @FXML
    private Label lblStatRevenue;
    @FXML
    private Label lblStatPending;

    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cmbFilterStatus;
    @FXML
    private javafx.scene.layout.HBox btnAddProduct;
    @FXML
    private javafx.scene.layout.HBox navOverview;
    @FXML
    private javafx.scene.layout.HBox navProducts;
    @FXML
    private javafx.scene.layout.HBox navAuctions;
    @FXML
    private javafx.scene.layout.HBox navTransactionHistory;
    @FXML
    private javafx.scene.layout.HBox navNotification;
    @FXML
    private javafx.scene.layout.HBox btnLogout;

    @FXML
    private TableView<Item> tblProducts;
    @FXML
    private TableColumn<Item, String> colName;
    @FXML
    private TableColumn<Item, String> colType;
    @FXML
    private TableColumn<Item, String> colStartPrice;
    @FXML
    private TableColumn<Item, String> colCurrentPrice;
    @FXML
    private TableColumn<Item, String> colEndTime;
    @FXML
    private TableColumn<Item, String> colStatus;
    @FXML
    private TableColumn<Item, Void> colActions;

    private final ObservableList<Item> masterProducts = FXCollections.observableArrayList();
    private final FilteredList<Item> filteredProducts = new FilteredList<>(masterProducts, p -> true);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupStatusFilter();
        loadCurrentUser();
        loadSellerProducts();

        // Sidebar and search wiring
        if (btnAddProduct != null) btnAddProduct.setOnMouseClicked(e -> handleAddProduct());
        if (navOverview != null) navOverview.setOnMouseClicked(e -> {
            // For now simply reload overview stats
            loadSellerProducts();
        });
        if (navProducts != null) navProducts.setOnMouseClicked(e -> {
            // Scroll to products table - keep behavior simple
            tblProducts.requestFocus();
        });
        if (navAuctions != null) navAuctions.setOnMouseClicked(e -> {
            System.out.println("Clicked: Phiên đấu giá (Auctions)");
            // TODO: Navigate to auctions view
        });
        if (navTransactionHistory != null) navTransactionHistory.setOnMouseClicked(e -> {
            System.out.println("Clicked: Lịch sử giao dịch (Transaction History)");
            // TODO: Navigate to transaction history view
        });
        if (navNotification != null) navNotification.setOnMouseClicked(e -> {
            System.out.println("Clicked: Thông báo (Notifications)");
            // TODO: Navigate to notifications view
        });
        if (btnLogout != null) btnLogout.setOnMouseClicked(e -> handleOut());

        if (txtSearch != null) {
            txtSearch.setOnAction(e -> applyFilter());
            txtSearch.textProperty().addListener((obs, o, n) -> applyFilter());
        }
        if (cmbFilterStatus != null) cmbFilterStatus.setOnAction(e -> applyFilter());
    }

    private void setupTable() {
        colName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItemName()));
        colType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getType()));
        colStartPrice.setCellValueFactory(cell -> new SimpleStringProperty(formatMoney(cell.getValue().getFirstprice())));
        colCurrentPrice.setCellValueFactory(cell -> new SimpleStringProperty(formatMoney(cell.getValue().getCurrentPrice())));
        colEndTime.setCellValueFactory(cell -> new SimpleStringProperty("--"));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));

        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnEdit = new Button("Sửa");

            {
                btnEdit.setStyle("-fx-background-color: #3B6D11; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 6;");
                btnEdit.setOnAction(event -> {
                    Item currentItem = getTableView().getItems().get(getIndex());
                    if (currentItem != null) {
                        openProductForm(currentItem);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnEdit);
            }
        });

        tblProducts.setItems(filteredProducts);

        // Double-click row to edit
        tblProducts.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Item> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(evt -> {
                if (evt.getClickCount() == 2 && !row.isEmpty()) {
                    openProductForm(row.getItem());
                }
            });
            return row;
        });
    }

    private void setupStatusFilter() {
        cmbFilterStatus.setItems(FXCollections.observableArrayList(
                "Tất cả",
                Item.STATUS_PENDING,
                Item.STATUS_APPROVED,
                Item.STATUS_IN_AUCTION,
                Item.STATUS_SOLD,
                Item.STATUS_UNSOLD
        ));
        cmbFilterStatus.getSelectionModel().selectFirst();
    }

    private void loadCurrentUser() {
        Users currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null) {
            lblUserName.setText("Khách");
            lblUserRole.setText("Seller");
            return;
        }
        lblUserName.setText(currentUser.getUsername());
        lblUserRole.setText(currentUser.getRole());
    }

    private void loadSellerProducts() {
        masterProducts.clear();
        Users currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser != null) {
            masterProducts.addAll(AppInitializer.getItemManager().getItemsByUserId(currentUser.getId()));
        }
        applyFilter();
        updateStats();
    }

    private void applyFilter() {
        String keyword = txtSearch.getText();
        String status = cmbFilterStatus.getValue();

        Predicate<Item> predicate = item -> {
            boolean matchesSearch = keyword == null || keyword.isBlank()
                    || item.getItemName().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
            boolean matchesStatus = status == null || status.isBlank() || status.equals("Tất cả")
                    || item.getStatus().equalsIgnoreCase(status);
            return matchesSearch && matchesStatus;
        };

        filteredProducts.setPredicate(predicate);
        updateStats();
    }

    private void updateStats() {
        long activeCount = masterProducts.stream().filter(item -> Item.STATUS_IN_AUCTION.equalsIgnoreCase(item.getStatus())).count();
        long finishedCount = masterProducts.stream().filter(item -> Item.STATUS_SOLD.equalsIgnoreCase(item.getStatus())
                || Item.STATUS_UNSOLD.equalsIgnoreCase(item.getStatus())).count();
        long pendingCount = masterProducts.stream().filter(item -> Item.STATUS_PENDING.equalsIgnoreCase(item.getStatus())).count();
        BigDecimal totalRevenue = masterProducts.stream()
                .map(Item::getCurrentPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        lblStatActive.setText(String.valueOf(activeCount));
        lblStatFinished.setText(String.valueOf(finishedCount));
        lblStatPending.setText(String.valueOf(pendingCount));
        lblStatRevenue.setText(formatMoney(totalRevenue));
    }

    @FXML
    private void handleFilter() {
        applyFilter();
    }

    @FXML
    private void handleAddProduct() {
        openProductForm(null);
    }

    @FXML
    private void handleOut() {
        UserSession.logout();
        SceneManager.switchToLogin();
    }

    private void openProductForm(Item item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/seller.view/product_form.fxml"));
            Parent root = loader.load();
            com.bidding.controller.seller.ProductFormController formController = loader.getController();
            if (formController != null) {
                if (item != null) {
                    formController.setProduct(item);
                }
                formController.setOnSaveCallback(this::loadSellerProducts);
            }
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(item == null ? "Đăng sản phẩm mới" : "Sửa sản phẩm");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Không thể mở form sản phẩm: " + e.getMessage());
        }
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0 ₫";
        }
        return String.format("%,.0f ₫", amount.doubleValue());
    }
}
