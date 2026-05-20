package com.bidding.controller.admin;

import com.bidding.controller.admin.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminUserController implements Initializable {

    // ── Stat cards ────────────────────────────────────────────────
    @FXML private Label lblTotalUsers;
    @FXML private Label lblBidderCount;
    @FXML private Label lblSellerCount;
    @FXML private Label lblLockedCount;

    // ── Filter ────────────────────────────────────────────────────
    @FXML private TextField    txtSearchUser;
    @FXML private ComboBox<String> cmbRoleFilter;
    @FXML private ComboBox<String> cmbStatusFilter;

    // ── Table ─────────────────────────────────────────────────────
    @FXML private TableView<User>                tblUsers;
    @FXML private TableColumn<User, String>      colUserInfo;
    @FXML private TableColumn<User, String>      colUserRole;
    @FXML private TableColumn<User, String>      colUserCreated;
    @FXML private TableColumn<User, String>      colUserLastLogin;
    @FXML private TableColumn<User, String>      colUserStatus;
    @FXML private TableColumn<User, Void>        colUserActions;

    @FXML private Label      lblPageInfo;
    @FXML private Pagination pagination;

    // ── Data ──────────────────────────────────────────────────────
    private final ObservableList<User> masterList  = FXCollections.observableArrayList();
    private FilteredList<User>         filteredList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupComboBoxes();
        setupTableColumns();
        loadUsers();
        setupSearch();
        setupPagination();
    }

    private void setupComboBoxes() {
        cmbRoleFilter.setItems(FXCollections.observableArrayList(
                "Tất cả vai trò", "BIDDER", "SELLER", "ADMIN"
        ));
        cmbRoleFilter.setValue("Tất cả vai trò");

        cmbStatusFilter.setItems(FXCollections.observableArrayList(
                "Tất cả trạng thái", "Hoạt động", "Bị khóa"
        ));
        cmbStatusFilter.setValue("Tất cả trạng thái");
    }

    private void setupTableColumns() {
        // Cột tên + email (render tùy chỉnh)
        colUserInfo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                User u = (User) getTableRow().getItem();

                // Avatar chữ cái
                Label av = new Label(u.getInitials());
                av.setStyle(
                        "-fx-background-color: " + (u.getRole().equals("SELLER") ? "#EAF3DE" : "#E6F1FB") + ";" +
                                "-fx-text-fill: " + (u.getRole().equals("SELLER") ? "#27500A" : "#0C447C") + ";" +
                                "-fx-background-radius: 50; -fx-min-width: 26; -fx-min-height: 26;" +
                                "-fx-max-width: 26; -fx-max-height: 26;" +
                                "-fx-alignment: center; -fx-font-weight: bold; -fx-font-size: 10px;"
                );

                Label name  = new Label(u.getFullName());
                name.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                Label email = new Label(u.getEmail());
                email.setStyle("-fx-font-size: 10px; -fx-text-fill: #888888;");

                VBox info = new VBox(1, name, email);
                HBox box  = new HBox(7, av, info);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        // Vai trò
        colUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colUserRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) { setText(null); setStyle(""); return; }
                Label badge = new Label(role);
                badge.setStyle("-fx-font-size: 10px; -fx-padding: 2 8; -fx-background-radius: 20; -fx-font-weight: bold;" +
                        switch (role) {
                            case "BIDDER" -> "-fx-background-color: #E6F1FB; -fx-text-fill: #0C447C;";
                            case "SELLER" -> "-fx-background-color: #EAF3DE; -fx-text-fill: #27500A;";
                            case "ADMIN"  -> "-fx-background-color: #FCEBEB; -fx-text-fill: #791F1F;";
                            default       -> "";
                        });
                setGraphic(badge);
                setText(null);
            }
        });

        colUserCreated.setCellValueFactory(new PropertyValueFactory<>("createdDate"));
        colUserLastLogin.setCellValueFactory(new PropertyValueFactory<>("lastLogin"));

        // Trạng thái
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colUserStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label badge = new Label(status);
                badge.setStyle("-fx-font-size: 10px; -fx-padding: 2 8; -fx-background-radius: 20; -fx-font-weight: bold;" +
                        ("Hoạt động".equals(status)
                                ? "-fx-background-color: #EAF3DE; -fx-text-fill: #27500A;"
                                : "-fx-background-color: #FCEBEB; -fx-text-fill: #791F1F;"));
                setGraphic(badge);
                setText(null);
            }
        });

        // Hành động
        colUserActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView   = makeBtn("Xem",     "#E6F1FB", "#0C447C");
            private final Button btnLock   = makeBtn("Khóa",    "#FCEBEB", "#791F1F");
            private final Button btnUnlock = makeBtn("Mở khóa", "#EAF3DE", "#27500A");
            private final HBox   box       = new HBox(4);

            {
                btnView.setOnAction(e   -> handleViewUser(getUser()));
                btnLock.setOnAction(e   -> handleLockUser(getUser()));
                btnUnlock.setOnAction(e -> handleUnlockUser(getUser()));
            }

            private User getUser() { return getTableView().getItems().get(getIndex()); }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                User u = getUser();
                box.getChildren().setAll(
                        btnView,
                        "Hoạt động".equals(u.getStatus()) ? btnLock : btnUnlock
                );
                setGraphic(box);
            }
        });

        filteredList = new FilteredList<>(masterList, u -> true);
        tblUsers.setItems(filteredList);
    }

    private Button makeBtn(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: " + bg +
                "; -fx-text-fill: " + fg + "; -fx-padding: 3 8; -fx-background-radius: 5; -fx-cursor: hand; -fx-border-color: transparent;");
        return b;
    }

    private void setupSearch() {
        txtSearchUser.textProperty().addListener((obs, o, n) -> applyFilter());
        cmbRoleFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
        cmbStatusFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
    }

    private void applyFilter() {
        String kw     = txtSearchUser.getText().toLowerCase().trim();
        String role   = cmbRoleFilter.getValue();
        String status = cmbStatusFilter.getValue();
        filteredList.setPredicate(u -> {
            boolean matchKw = kw.isEmpty()
                    || u.getFullName().toLowerCase().contains(kw)
                    || u.getEmail().toLowerCase().contains(kw);
            boolean matchRole   = "Tất cả vai trò".equals(role)    || role == null   || u.getRole().equals(role);
            boolean matchStatus = "Tất cả trạng thái".equals(status) || status == null || u.getStatus().equals(status);
            return matchKw && matchRole && matchStatus;
        });
        updatePageInfo();
    }

    private void setupPagination() {
        pagination.currentPageIndexProperty().addListener((obs, o, n) -> updatePageInfo());
    }

    private void updatePageInfo() {
        int total = filteredList.size();
        int page  = pagination.getCurrentPageIndex();
        int from  = page * 10 + 1;
        int to    = Math.min(from + 9, total);
        lblPageInfo.setText("Hiển thị " + from + "–" + to + " của " + total + " người dùng");
        pagination.setPageCount(Math.max(1, (int) Math.ceil(total / 10.0)));
    }

    private void loadUsers() {
        // TODO: gọi API GET /api/admin/users
        masterList.setAll(
                new User(1, "Nguyễn Văn A",   "nguyenvana@email.com",   "BIDDER", "token1", "01/01/2024", "Hôm nay",    "Hoạt động"),
                new User(2, "Trần Thị Lan",   "tranthilan@email.com",   "SELLER", "token2", "15/02/2024", "Hôm qua",    "Hoạt động"),
                new User(3, "Lê Minh Đức",    "leminhduc@email.com",    "BIDDER", "token3", "20/03/2024", "3 ngày trước","Bị khóa"),
                new User(4, "Phạm Minh Tuấn", "phamminhtuan@email.com", "SELLER", "token4", "05/04/2024", "2 ngày trước","Hoạt động"),
                new User(5, "Hoàng Ngọc Bảo", "hoangbao@email.com",     "BIDDER", "token5", "10/04/2024", "1 tuần trước","Bị khóa")
        );

        long total  = masterList.size();
        long bidder = masterList.stream().filter(u -> "BIDDER".equals(u.getRole())).count();
        long seller = masterList.stream().filter(u -> "SELLER".equals(u.getRole())).count();
        long locked = masterList.stream().filter(u -> "Bị khóa".equals(u.getStatus())).count();

        lblTotalUsers.setText(String.valueOf(total));
        lblBidderCount.setText(String.valueOf(bidder));
        lblSellerCount.setText(String.valueOf(seller));
        lblLockedCount.setText(String.valueOf(locked));

        updatePageInfo();
    }

    // ── Event handlers ────────────────────────────────────────────

    @FXML
    private void handleAddUser(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/bidonline/admin_add_user_form.fxml")
            );
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Thêm người dùng mới");
            stage.setScene(new Scene(root, 480, 420));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadUsers(); // reload sau khi thêm
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleFilterUsers(ActionEvent event) {
        applyFilter();
    }

    private void handleViewUser(User user) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Thông tin người dùng");
        info.setHeaderText(user.getFullName() + " — " + user.getRole());
        info.setContentText(
                "Email: " + user.getEmail() + "\n" +
                        "Trạng thái: " + user.getStatus() + "\n" +
                        "Ngày tạo: " + user.getCreatedDate() + "\n" +
                        "Đăng nhập cuối: " + user.getLastLogin()
        );
        info.showAndWait();
    }

    private void handleLockUser(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận khóa tài khoản");
        confirm.setHeaderText("Khóa tài khoản \"" + user.getFullName() + "\"?");
        confirm.setContentText("Người dùng sẽ không thể đăng nhập cho đến khi được mở khóa.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            user.setStatus("Bị khóa");
            // TODO: gọi API PUT /api/admin/users/{id}/lock
            tblUsers.refresh();
            updateStats();
        }
    }

    private void handleUnlockUser(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận mở khóa");
        confirm.setHeaderText("Mở khóa tài khoản \"" + user.getFullName() + "\"?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            user.setStatus("Hoạt động");
            // TODO: gọi API PUT /api/admin/users/{id}/unlock
            tblUsers.refresh();
            updateStats();
        }
    }

    private void updateStats() {
        long locked = masterList.stream().filter(u -> "Bị khóa".equals(u.getStatus())).count();
        lblLockedCount.setText(String.valueOf(locked));
    }

    // Được gọi từ LoginController sau khi load màn hình
    public void setUser(User user) {
        // Không cần dùng user ở đây vì Admin thấy tất cả
    }

    @FXML
    private void handleExportUsers(ActionEvent event) {
        // TODO: xuất danh sách ra CSV
        Alert info = new Alert(Alert.AlertType.INFORMATION, "Đã xuất file CSV thành công.", ButtonType.OK);
        info.setTitle("Xuất CSV");
        info.showAndWait();
    }
}
