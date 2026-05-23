package com.bidding.controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Controller for admin_product_management.fxml
 * Manages product listings with filtering, pagination, and CRUD actions.
 */
public class AdminProductController {
    // Navigation sidebar
    @FXML private HBox navDashboard;
    @FXML private HBox navUsers;
    @FXML private HBox navAuctions;
    @FXML private HBox navProducts;
    @FXML private HBox btnLogout;

    // Stats
    @FXML private Label lblTotalProducts;
    @FXML private Label lblActiveProducts;
    @FXML private Label lblPendingProducts;
    @FXML private Label lblRejectedProducts;

    // Filters
    @FXML private TextField txtSearchProduct;
    @FXML private ComboBox<String> cmbProdCategory;
    @FXML private ComboBox<String> cmbProdStatus;

    // Table & pagination
    @FXML private TableView<ProductRow> tblProducts;
    @FXML private TableColumn<ProductRow, String> colProdName;
    @FXML private TableColumn<ProductRow, String> colProdCategory;
    @FXML private TableColumn<ProductRow, String> colProdSeller;
    @FXML private TableColumn<ProductRow, Double> colProdPrice;
    @FXML private TableColumn<ProductRow, String> colProdStatus;
    @FXML private TableColumn<ProductRow, String> colProdCreated;
    @FXML private TableColumn<ProductRow, Void> colProdActions;

    @FXML private Label lblProdPageInfo;
    @FXML private Pagination prodPagination;

    private final ObservableList<ProductRow> masterData = FXCollections.observableArrayList();
    private final ObservableList<ProductRow> filteredData = FXCollections.observableArrayList();

    private static final int ROWS_PER_PAGE = 10;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault());

    @FXML
    public void initialize() {
        setupFilters();
        setupTableColumns();
        loadDemoData();
        applyFilterAndRefresh();
        setupPagination();
        setupNavHandlers();
    }

    private void setupFilters() {
        cmbProdStatus.setItems(FXCollections.observableArrayList("All", "ACTIVE", "PENDING", "REJECTED", "ARCHIVED"));
        cmbProdStatus.getSelectionModel().selectFirst();
        cmbProdCategory.setItems(FXCollections.observableArrayList("All", "Electronics", "Vehicles", "Art", "Jewelry", "Real Estate"));
        cmbProdCategory.getSelectionModel().selectFirst();
    }

    private void setupTableColumns() {
        colProdName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colProdCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colProdSeller.setCellValueFactory(new PropertyValueFactory<>("seller"));
        colProdPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colProdStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colProdCreated.setCellValueFactory(new PropertyValueFactory<>("createdDate"));

        // Format price column
        colProdPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) setText(null);
                else setText(String.format("%,.0f ₫", value));
            }
        });

        // Status column with color coding
        colProdStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle(null);
                } else {
                    setText(value);
                    switch (value.toUpperCase()) {
                        case "ACTIVE":
                            setStyle("-fx-text-fill: #3B6D11; -fx-font-weight: bold;");
                            break;
                        case "PENDING":
                            setStyle("-fx-text-fill: #854F0B; -fx-font-weight: bold;");
                            break;
                        case "REJECTED":
                            setStyle("-fx-text-fill: #A32D2D; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("-fx-text-fill: #888888;");
                    }
                }
            }
        });

        // Actions column
        colProdActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("View");
            private final Button btnApprove = new Button("Approve");
            private final Button btnReject = new Button("Reject");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(6, btnView, btnApprove, btnReject, btnDelete);
            {
                btnView.setOnAction(e -> onView(getIndex()));
                btnApprove.setOnAction(e -> onApprove(getIndex()));
                btnReject.setOnAction(e -> onReject(getIndex()));
                btnDelete.setOnAction(e -> onDelete(getIndex()));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(box);
            }
        });
    }

    private void loadDemoData() {
        masterData.clear();
        String[] categories = {"Electronics", "Vehicles", "Art", "Jewelry", "Real Estate"};
        String[] statuses = {"ACTIVE", "ACTIVE", "PENDING", "REJECTED"};

        List<ProductRow> demo = IntStream.rangeClosed(1, 45)
                .mapToObj(i -> new ProductRow(
                        i,
                        "Product #" + i,
                        categories[i % 5],
                        "Seller" + ((i % 8) + 1),
                        100000.0 * (i % 50 + 1),
                        statuses[i % 4],
                        dtf.format(Instant.now().minusSeconds(86400L * (i % 30)))
                ))
                .collect(Collectors.toList());
        masterData.addAll(demo);
        updateStats();
    }

    private void updateStats() {
        lblTotalProducts.setText(String.valueOf(masterData.size()));
        long active = masterData.stream().filter(p -> p.getStatus().equals("ACTIVE")).count();
        long pending = masterData.stream().filter(p -> p.getStatus().equals("PENDING")).count();
        long rejected = masterData.stream().filter(p -> p.getStatus().equals("REJECTED")).count();
        lblActiveProducts.setText(String.valueOf(active));
        lblPendingProducts.setText(String.valueOf(pending));
        lblRejectedProducts.setText(String.valueOf(rejected));
    }

    private void applyFilterAndRefresh() {
        filteredData.setAll(masterData.stream()
                .filter(this::matchesFilter)
                .collect(Collectors.toList()));
        int pageCount = Math.max(1, (int) Math.ceil((double) filteredData.size() / ROWS_PER_PAGE));
        prodPagination.setPageCount(pageCount);
        changePage(0);
    }

    private boolean matchesFilter(ProductRow row) {
        String q = txtSearchProduct.getText();
        if (q != null && !q.isEmpty()) {
            String low = q.toLowerCase();
            if (!(row.getName().toLowerCase().contains(low) || row.getSeller().toLowerCase().contains(low))) {
                return false;
            }
        }
        String status = cmbProdStatus.getValue();
        if (status != null && !status.equals("All") && !row.getStatus().equals(status)) return false;
        String category = cmbProdCategory.getValue();
        if (category != null && !category.equals("All") && !row.getCategory().equals(category)) return false;
        return true;
    }

    private void setupPagination() {
        prodPagination.setPageFactory(this::createPage);
        prodPagination.setMaxPageIndicatorCount(5);
    }

    private Node createPage(int pageIndex) {
        changePage(pageIndex);
        return tblProducts;
    }

    private void changePage(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filteredData.size());
        if (fromIndex > toIndex) {
            tblProducts.setItems(FXCollections.observableArrayList());
        } else {
            tblProducts.setItems(FXCollections.observableArrayList(filteredData.subList(fromIndex, toIndex)));
        }
        lblProdPageInfo.setText(String.format("Hiển thị %d–%d của %d sản phẩm", fromIndex + 1, toIndex, filteredData.size()));
    }

    private void setupNavHandlers() {
        btnLogout.setOnMouseClicked(e -> handleLogout());
        navUsers.setOnMouseClicked(e -> System.out.println("Navigate to Users"));
        navDashboard.setOnMouseClicked(e -> System.out.println("Navigate to Dashboard"));
        navAuctions.setOnMouseClicked(e -> System.out.println("Navigate to Auctions"));
    }

    @FXML
    private void handleFilterProducts() {
        applyFilterAndRefresh();
    }

    @FXML
    private void handleCreateProduct() {
        System.out.println("Create new product clicked");
        // TODO: Open product creation dialog or switch to create scene
    }

    private void onView(int index) {
        ProductRow row = getRowByIndex(index);
        if (row != null) System.out.println("View product: " + row.getName());
    }

    private void onApprove(int index) {
        ProductRow row = getRowByIndex(index);
        if (row != null) {
            System.out.println("Approve product: " + row.getName());
            row.setStatus("ACTIVE");
            updateStats();
            tblProducts.refresh();
        }
    }

    private void onReject(int index) {
        ProductRow row = getRowByIndex(index);
        if (row != null) {
            System.out.println("Reject product: " + row.getName());
            row.setStatus("REJECTED");
            updateStats();
            tblProducts.refresh();
        }
    }

    private void onDelete(int index) {
        ProductRow row = getRowByIndex(index);
        if (row != null) {
            System.out.println("Delete product: " + row.getName());
            masterData.remove(row);
            updateStats();
            applyFilterAndRefresh();
        }
    }

    private ProductRow getRowByIndex(int index) {
        if (index < 0 || index >= tblProducts.getItems().size()) return null;
        return tblProducts.getItems().get(index);
    }

    private void handleLogout() {
        System.out.println("Admin logout clicked");
        // TODO: Implement logout (SceneManager.switchToLogin())
    }

    /**
     * POJO for product table row
     */
    public static class ProductRow {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty name;
        private final SimpleStringProperty category;
        private final SimpleStringProperty seller;
        private final SimpleDoubleProperty price;
        private final SimpleStringProperty status;
        private final SimpleStringProperty createdDate;

        public ProductRow(int id, String name, String category, String seller, double price, String status, String createdDate) {
            this.id = new SimpleIntegerProperty(id);
            this.name = new SimpleStringProperty(name);
            this.category = new SimpleStringProperty(category);
            this.seller = new SimpleStringProperty(seller);
            this.price = new SimpleDoubleProperty(price);
            this.status = new SimpleStringProperty(status);
            this.createdDate = new SimpleStringProperty(createdDate);
        }

        public int getId() { return id.get(); }
        public String getName() { return name.get(); }
        public String getCategory() { return category.get(); }
        public String getSeller() { return seller.get(); }
        public double getPrice() { return price.get(); }
        public String getStatus() { return status.get(); }
        public String getCreatedDate() { return createdDate.get(); }

        public void setStatus(String s) { status.set(s); }
    }
}

