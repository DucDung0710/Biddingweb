package com.bidding.controller.seller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import com.bidding.service.AuctionService;
import com.bidding.shared.Item;
import com.bidding.shared.UserSession;
import com.bidding.shared.Users;
import com.bidding.util.SocketClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SellerDashboardController {

    // ==========================================
    // 1. ÁNH XẠ THÀNH PHẦN SIDEBAR (GÓC TRÁI DƯỚI)
    // ==========================================
    @FXML
    private Label lblUserName;   // Hiển thị tên thực tế của Seller

    @FXML
    private Label lblUserRole;   // Hiển thị vai trò (Mặc định: Seller)

    @FXML
    private Label lblAvatar;     // Vòng tròn chứa chữ cái đầu của tên

    @FXML
    private HBox navOverview;

    @FXML
    private HBox btnAddProduct;

    @FXML
    private HBox navAuctions;

    @FXML
    private HBox navTransactionHistory;

    @FXML
    private HBox navNotification;

    @FXML
    private HBox btnLogout;

    @FXML
    private Label lblNotifBadge;

    // ==========================================
    // 2. ÁNH XẠ CÁC THÀNH PHẦN KHÁC TRÊN GIAO DIỆN
    // ==========================================
    @FXML
    private Button btnTopAddProduct;

    @FXML
    private TextField txtSearch;

    @FXML
    private ComboBox<String> cmbFilterStatus;

    @FXML
    private Label lblStatActive;

    @FXML
    private Label lblStatFinished;

    @FXML
    private Label lblStatRevenue;

    @FXML
    private Label lblStatPending;

    @FXML
    private TableView<Item> tblProducts;

    private final ObservableList<Item> masterProducts = FXCollections.observableArrayList();
    private final FilteredList<Item> filteredProducts = new FilteredList<>(masterProducts, p -> true);

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
    private TableColumn<Item, String> colActions;

    // ==========================================
    // 3. LOGIC XỬ LÝ KHỞI TẠO & CẬP NHẬT THÔNG TIN
    // ==========================================
    
    @FXML
    public void initialize() {
        // Hàm tự động chạy khi file FXML được nạp thành công
        // Bạn có thể thêm logic lấy danh sách sản phẩm hoặc cấu hình bảng tại đây
        // Gán sự kiện click cho các mục sidebar để tương tác
        try {
            if (navOverview != null) {
                navOverview.setOnMouseClicked(e -> handleNavOverview());
            }
            if (btnAddProduct != null) {
                btnAddProduct.setOnMouseClicked(e -> {
                    System.out.println("Sidebar: btnAddProduct clicked");
                    handleAddProduct();
                });
            }
            if (navAuctions != null) {
                navAuctions.setOnMouseClicked(e -> {
                    System.out.println("Sidebar: navAuctions clicked");
                    handleNavAuctions();
                });
            }
            if (navTransactionHistory != null) {
                navTransactionHistory.setOnMouseClicked(e -> {
                    System.out.println("Sidebar: navTransactionHistory clicked");
                    com.bidding.util.SceneManager.switchToSellerTransactionHistory();
                });
            }
            if (navNotification != null) {
                navNotification.setOnMouseClicked(e -> {
                    System.out.println("Sidebar: navNotification clicked");
                    com.bidding.util.SceneManager.switchToSellerNotifications();
                });
            }
            if (btnLogout != null) {
                btnLogout.setOnMouseClicked(e -> handleOut());
            }
        } catch (Exception ex) {
            System.err.println("SellerDashboardController initialize error: " + ex.getMessage());
            ex.printStackTrace();
        }

        if (tblProducts != null) {
            tblProducts.setItems(filteredProducts);
        }
        if (cmbFilterStatus != null) {
            cmbFilterStatus.setItems(FXCollections.observableArrayList(
                    "Tất cả",
                    Item.STATUS_PENDING,
                    Item.STATUS_APPROVED,
                    Item.STATUS_IN_AUCTION,
                    Item.STATUS_SOLD,
                    Item.STATUS_UNSOLD
            ));
            cmbFilterStatus.getSelectionModel().selectFirst();
            cmbFilterStatus.setOnAction(e -> applyFilter());
        }
        if (txtSearch != null) {
            txtSearch.setOnAction(e -> applyFilter());
            txtSearch.textProperty().addListener((obs, o, n) -> applyFilter());
        }

        // Thiết lập cell factories cho các cột (hiển thị thuộc tính Item)
        try {
            colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItemName()));
            colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
            colStartPrice.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFirstprice() != null ? c.getValue().getFirstprice().toString() : "0"));
            colCurrentPrice.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCurrentPrice() != null ? c.getValue().getCurrentPrice().toString() : "0"));
            colEndTime.setCellValueFactory(c -> new SimpleStringProperty(""));
            colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        } catch (Exception e) {
            System.err.println("Không thể gán cell factories cho TableView: " + e.getMessage());
        }

        // Tải dữ liệu sản phẩm Seller ngay khi vào màn hình
        reloadProducts();
    }

    /**
     * HÀM TỰ ĐỘNG CẬP NHẬT THÔNG TIN SELLER
     * Hàm này sẽ được gọi từ LoginController sau khi xác thực tài khoản thành công.
     * * @param fullName Họ và tên đầy đủ của Seller 
     */
    public void setSellerInfo(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            lblUserName.setText("Khách");
            if (lblAvatar != null) lblAvatar.setText("K");
            return;
        }

        // 1. Cập nhật chuỗi tên người bán lên giao diện góc trái dưới
        lblUserName.setText(fullName);
        lblUserRole.setText("Seller");

        // 2. Logic tự động bóc tách chữ cái đầu tiên của Tên chính để làm Avatar
        // Ví dụ: "Nguyễn Văn Hoàng" -> Tách chuỗi lấy từ cuối "Hoàng" -> Lấy chữ "H"
        try {
            String[] words = fullName.trim().split("\\s+");
            String firstName = words[words.length - 1]; // Lấy từ cuối cùng
            
            if (!firstName.isEmpty() && lblAvatar != null) {
                String firstLetter = firstName.substring(0, 1).toUpperCase();
                lblAvatar.setText(firstLetter); // Thay chữ 'S' tĩnh bằng chữ cái của tên họ
            }
        } catch (Exception e) {
            // Đề phòng trường hợp chuỗi tên bị lỗi định dạng bất ngờ
            if (lblAvatar != null) lblAvatar.setText("S");
        }
    }

    // ==========================================
    // 4. ĐỊNH NGHĨA CÁC HÀM SỰ KIỆN (EVENT HANDLERS)
    // ==========================================
    
    @FXML
    private void handleAddProduct() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/seller.view/product_form.fxml"));
            Parent root = loader.load();

            ProductFormController formController = loader.getController();
            formController.setOnSaveCallback(this::reloadProducts);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Đăng sản phẩm mới");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (Exception e) {
            System.err.println("Lỗi mở form thêm sản phẩm: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void reloadProducts() {
        Users currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null) {
            System.out.println("Không có user đang đăng nhập.");
            return;
        }

        try {
            JsonObject request = new JsonObject();
            request.addProperty("action", "GET_ALL_ITEMS");

            JsonObject response = SocketClient.getInstance().sendRequest(request);
            if (response != null && "OK".equals(response.get("status").getAsString())) {
                JsonArray array = response.getAsJsonArray("data");
                ObservableList<Item> items = FXCollections.observableArrayList();
                for (JsonElement elem : array) {
                    JsonObject obj = elem.getAsJsonObject();
                    if (obj.get("userId").getAsInt() != currentUser.getId()) {
                        continue;
                    }
                    Item item = new Item();
                    item.setItemId(obj.get("itemId").getAsInt());
                    item.setUserId(obj.get("userId").getAsInt());
                    item.setItemName(obj.get("itemName").getAsString());
                    item.setType(obj.has("type") ? obj.get("type").getAsString() : "Khác");
                    item.setDescription(obj.has("description") ? obj.get("description").getAsString() : "");
                    item.setStatus(obj.has("status") ? obj.get("status").getAsString() : Item.STATUS_PENDING);
                    if (obj.has("firstprice")) {
                        item.setFirstprice(java.math.BigDecimal.valueOf(obj.get("firstprice").getAsDouble()));
                    }
                    items.add(item);
                }
                masterProducts.setAll(items);
                System.out.println("Đã tải lại " + items.size() + " sản phẩm cho seller từ Server.");
                applyFilter();
                return;
            }

            System.err.println("Không thể tải sản phẩm từ Server, sử dụng dữ liệu cục bộ.");
            ObservableList<Item> fallbackItems = FXCollections.observableArrayList(AuctionService.getInstance().getSellerItems(currentUser.getId()));
            masterProducts.setAll(fallbackItems);
            applyFilter();
        } catch (Exception ex) {
            System.err.println("Lỗi khi tải sản phẩm: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void handleNavOverview() {
        com.bidding.util.SceneManager.switchToSellerDashboard();
    }

    private void handleNavAuctions() {
        com.bidding.util.SceneManager.switchToSellerProductManagement();
    }

    private void applyFilter() {
        if (txtSearch == null || cmbFilterStatus == null) {
            return;
        }
        String keyword = txtSearch.getText();
        String status = cmbFilterStatus.getValue();
        ObservableList<Item> filtered = masterProducts.filtered(item -> {
            boolean matchesKeyword = keyword == null || keyword.isBlank() || item.getItemName().toLowerCase().contains(keyword.toLowerCase());
            boolean matchesStatus = status == null || status.isBlank() || status.equals("Tất cả") || item.getStatus().equalsIgnoreCase(status);
            return matchesKeyword && matchesStatus;
        });
        if (tblProducts != null) {
            tblProducts.setItems(filtered);
        }
    }

    @FXML
    private void handleFilter() {
        applyFilter();
    }

    @FXML
    private void handleOut() {
        UserSession.logout();
        com.bidding.util.SceneManager.switchToLogin();
    }
}