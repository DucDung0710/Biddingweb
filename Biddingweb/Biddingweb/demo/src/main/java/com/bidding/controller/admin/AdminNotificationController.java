package com.bidding.controller.admin;

import com.bidding.model.SystemNotification;
import com.bidding.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import java.io.IOException;

public class AdminNotificationController {

    // Khai báo các thành phần Sidebar
    @FXML private HBox navOverview, navUsers, navAuctions, navProducts, navWallet, navAuctionHistory, navNotifications, btnLogout;
    @FXML private Label lblPendingBadge;

    // Khai báo thành phần bảng điều khiển chính
    @FXML private Button btnRefreshNoti;
    @FXML private TableView<SystemNotification> tblNotifications;
    @FXML private TableColumn<SystemNotification, String> colNotiTime;
    @FXML private TableColumn<SystemNotification, String> colNotiType;
    @FXML private TableColumn<SystemNotification, String> colNotiContent;

    // Danh sách lưu trữ dữ liệu hiển thị trong bảng
    private final ObservableList<SystemNotification> notificationList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1. LIÊN KẾT CHUYỂN CẢNH SIDEBAR MENU
        navOverview.setOnMouseClicked(e -> {SceneManager.switchToAdminDashboard();  });

        navUsers.setOnMouseClicked(e -> {  SceneManager.switchToAdminUserManagement();  });

        navAuctions.setOnMouseClicked(e -> { SceneManager.switchToAdminAuctionManagement(); });

        navWallet.setOnMouseClicked(e -> { SceneManager.switchToAdminWalletManagement();  });

        navNotifications.setOnMouseClicked(e -> { SceneManager.switchToAdminNotifications(); });

        navProducts.setOnMouseClicked(e -> { SceneManager.switchToAdminProductManagement(); });

        navAuctionHistory.setOnMouseClicked(e -> { SceneManager.switchToAdminAuctionHistory();  });

        btnLogout.setOnMouseClicked(e -> { SceneManager.switchToLogin(); });

        // 2. KẾT NỐI CÁC CỘT BẢNG VỚI MODEL DATA
        colNotiTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colNotiType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colNotiContent.setCellValueFactory(new PropertyValueFactory<>("content"));

        // 3. THIẾT LẬP TỰ ĐỘNG XUỐNG DÒNG (WRAP TEXT) CHO CỘT NỘI DUNG CHI TIẾT
        colNotiContent.setCellFactory(tc -> new TableCell<SystemNotification, String>() {
            private final Text text = new Text();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    text.setText(item);
                    // Giới hạn chiều rộng chữ khít theo độ rộng thực tế của cột để tự xuống dòng
                    text.wrappingWidthProperty().bind(colNotiContent.widthProperty().subtract(15));
                    setGraphic(text);
                }
            }
        });

        // 4. SỰ KIỆN NÚT LÀM MỚI HỘP THƯ
        btnRefreshNoti.setOnAction(e -> loadIncomingNotifications());

        // Nạp dữ liệu mẫu ban đầu khi mở màn hình
        loadIncomingNotifications();
    }

    private void loadIncomingNotifications() {
        // Xóa sạch dữ liệu cũ
        notificationList.clear();

        // Thêm dữ liệu mẫu giả lập đẩy về từ Client
        notificationList.add(new SystemNotification("12:01:45 - 22/05/2026", "💰 Yêu cầu nạp tiền", "Người dùng bbuoibui vừa tạo một yêu cầu nạp tiền với số tiền là 500,000 ₫ cần được phê duyệt khẩn cấp."));
        notificationList.add(new SystemNotification("11:54:20 - 22/05/2026", "👥 Tài khoản mới", "Tài khoản người dùng mới shizuka (Email: shizuka@vnu.edu.vn) vừa đăng ký thành công vào hệ thống với vai trò là Bidder."));
        notificationList.add(new SystemNotification("11:30:00 - 22/05/2026", "🔨 Phiên đấu giá", "Phiên đấu giá tài sản mã số #AUC-9942 kết hợp sản phẩm 'Laptop ThinkPad X1 Gen 11' đã kết thúc. Người chiến thắng: lina."));
        notificationList.add(new SystemNotification("09:15:32 - 22/05/2026", "⚠️ Báo cáo vi phạm", "Seller khanh vừa bị người dùng leminh gửi đơn khiếu nại với lý do: Mô tả sai lệch thông tin hiện trạng thực tế của sản phẩm."));

        // Đẩy danh sách dữ liệu vào TableView
        tblNotifications.setItems(notificationList);

        // Đồng bộ Badge ví tiền đang chờ xử lý
        lblPendingBadge.setText("3");
    }
}