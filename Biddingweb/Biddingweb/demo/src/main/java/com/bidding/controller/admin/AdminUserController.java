package com.bidding.controller.admin; // Đã đồng bộ đúng package quản lý admin của bạn

import com.bidding.util.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class AdminUserController {

    @FXML private HBox navOverview, navUsers, navAuctions, navProducts, navWallet, navAuctionHistory, navNotifications, btnLogout;
    @FXML private Label lblPendingBadge; // Badge thông báo số lượng đơn ví chờ duyệt

    @FXML private Button btnAddUser;
    @FXML private TextField txtSearchUser;
    @FXML private ComboBox<String> cmbRoleFilter;
    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private TableView<?> tblUsers;
    @FXML private Pagination pagination;

    @FXML
    public void initialize() {
        // --- LIÊN KẾT SIDEBAR ---

        navOverview.setOnMouseClicked(e -> {SceneManager.switchToAdminDashboard();});
        // Mục navUsers đang active ở màn hình này nên không cần bắt sự kiện tự chuyển cảnh

        navAuctions.setOnMouseClicked(e -> {SceneManager.switchToAdminAuctionManagement();});

        navProducts.setOnMouseClicked(e -> {SceneManager.switchToAdminProductManagement();});

        navUsers.setOnMouseClicked(e -> {SceneManager.switchToAdminUserManagement();});

        navAuctionHistory.setOnMouseClicked(e -> {SceneManager.switchToAdminAuctionHistory();});

        navNotifications.setOnMouseClicked(e -> {SceneManager.switchToAdminNotifications();});


        navWallet.setOnMouseClicked(e -> {SceneManager.switchToAdminWalletManagement();});

        btnLogout.setOnMouseClicked(e -> {SceneManager.switchToLogin();});

        // --- CÀI ĐẶT BỘ LỌC DỮ LIỆU ---
        cmbRoleFilter.getItems().addAll("Tất cả vai trò", "Bidder", "Seller", "Admin");
        cmbStatusFilter.getItems().addAll("Tất cả trạng thái", "Hoạt động", "Bị khóa");

        // Đổ số liệu mẫu cho Badge ví tiền trên Sidebar
        if (lblPendingBadge != null) {
            lblPendingBadge.setText("3");
        }
    }

    @FXML
    private void handleFilterUsers() {
        String search = txtSearchUser.getText();
        String role = cmbRoleFilter.getValue();
        String status = cmbStatusFilter.getValue();

        System.out.println("Thực hiện tìm kiếm User: " + search + " | " + role + " | " + status);
    }

    @FXML
    private void handleAddUser(ActionEvent event) {
        try {
            // 1. Nạp file FXML bằng URL
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin.view/admin-add-user-dialog.fxml"));
            Parent root = loader.load();
            // 2. Tạo một Stage mới cho cửa sổ Pop-up
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Thêm người dùng mới - BidOnline");

            dialogStage.initStyle(StageStyle.UTILITY);

            // Ngăn người dùng tương tác với màn hình chính phía sau khi pop-up đang mở
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            // Định vị pop-up phụ thuộc vào màn hình chính hiện tại
            Stage mainStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            dialogStage.initOwner(mainStage);

            // 3. Thiết lập Scene và hiển thị cửa sổ
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);

            // Ngăn không cho kéo giãn kích thước cửa sổ nhập liệu này
            dialogStage.setResizable(false);

            // Hiển thị và đợi cho đến khi người dùng đóng cửa sổ này (bấm Lưu hoặc Hủy)
            dialogStage.showAndWait();

            // 5. Thêm logic làm mới (refresh) lại TableView danh sách người dùng ở đây sau khi đóng pop-up nếu muốn
            // handleFilterUsers();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không thể mở giao diện Thêm người dùng. Hãy kiểm tra lại đường dẫn file FXML.");
        }
}}
