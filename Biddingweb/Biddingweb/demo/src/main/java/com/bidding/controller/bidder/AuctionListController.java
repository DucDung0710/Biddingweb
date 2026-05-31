package com.bidding.controller.bidder;

import com.bidding.model.AuctionDisplayDTO;
import com.bidding.util.DataContext;
import com.bidding.util.SocketClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AuctionListController extends BaseBidderController {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private ComboBox<String> cmbType;

    // 3 Vùng chứa tương ứng với 3 trạng thái phiên đấu giá trên UI
    @FXML private FlowPane paneRunning;
    @FXML private FlowPane paneOpen;
    @FXML private FlowPane paneFinished;

    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        // 1. Gọi thiết lập Sidebar chung của lớp cha
        super.setupSidebarBehavior();

        // 2. TỐI ƯU UX: Vô hiệu hóa sự kiện click lại chính trang danh sách
        if (navAuctions != null) {
            navAuctions.setOnMouseClicked(null);
        }

        // 3. Khởi tạo các giá trị bộ lọc
        initFilterComboboxes();

        // 4.Kết nối tìm kiếm từ Dashboard qua Singleton DataContext
        String externalKeyword = DataContext.getInstance().getSearchKeyword();

        if (externalKeyword != null && !externalKeyword.trim().isEmpty()) {
            txtSearch.setText(externalKeyword);

            // Xóa từ khóa trong DataContext để tránh tự động lọc ở các lần sau
            DataContext.getInstance().clear();

            // Gọi hàm lọc để hiển thị ngay kết quả
            handleFilter();
        } else {
            loadAllAuctionItems();
        }

        // LẮNG NGHE SỰ KIỆN: Khi người dùng thay đổi lựa chọn ở ComboBox thì tự động lọc luôn
        cmbStatus.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> handleFilter());
        cmbType.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> handleFilter());
    }

    private void initFilterComboboxes() {
        cmbStatus.getItems().clear();
        cmbStatus.getItems().addAll("Tất cả", "Đang diễn ra", "Sắp bắt đầu", "Đã kết thúc");
        cmbStatus.setValue("Tất cả");

        cmbType.getItems().clear();
        cmbType.getItems().addAll("Tất cả", "Electronics", "Vehicles", "Art");
        cmbType.setValue("Tất cả");
    }

    @FXML
    private void handleFilter() {
        String keyword = txtSearch.getText().trim();
        String status = cmbStatus.getValue();
        String type = cmbType.getValue();

        System.out.println("Đang lọc theo mạng: " + keyword + " | " + status + " | " + type);

        // Đóng gói request lọc gửi lên Server Socket
        JsonObject filterReq = new JsonObject();
        filterReq.addProperty("action", "GET_AUCTIONS_BY_FILTER");
        filterReq.addProperty("keyword", keyword);
        filterReq.addProperty("status", status);
        filterReq.addProperty("type", type);

        // Gọi qua lớp SocketClient Singleton kết nối dài hạn đã đồng bộ ở các màn hình trước
        JsonObject response = SocketClient.getInstance().sendRequest(filterReq);

        if (response != null && "OK".equals(response.get("status").getAsString())) {
            JsonArray array = response.getAsJsonArray("auctions");
            Type listType = new TypeToken<ArrayList<AuctionDisplayDTO>>(){}.getType();
            List<AuctionDisplayDTO> filteredList = gson.fromJson(array, listType);

            // Đổ dữ liệu thật lên giao diện phân mảnh FlowPane
            renderAuctionsToPanes(filteredList);
        } else {
            System.err.println("Lỗi tải danh sách bộ lọc từ server.");
        }
    }

    private void loadAllAuctionItems() {
        // Mặc định load hết sản phẩm (tương đương với việc gọi bộ lọc "Tất cả")
        handleFilter();
    }

    /**
     * Hàm bóc tách phân loại trạng thái để đổ động file item_card.fxml vào đúng vị trí FlowPane
     */
    private void renderAuctionsToPanes(List<AuctionDisplayDTO> auctions) {
        // Dọn sạch dữ liệu cũ của cả 3 pane trước khi nạp mới
        paneRunning.getChildren().clear();
        paneOpen.getChildren().clear();
        paneFinished.getChildren().clear();

        if (auctions == null || auctions.isEmpty()) {
            Label lblEmpty = new Label("Không tìm thấy phiên đấu giá nào phù hợp.");
            lblEmpty.setStyle("-fx-text-fill: #888888; -fx-font-style: italic; -fx-padding: 10;");
            paneRunning.getChildren().add(lblEmpty); // Đưa tạm thông báo trống vào pane chính
            return;
        }

        for (AuctionDisplayDTO auction : auctions) {
            try {
                // Tái sử dụng thiết kế loader mẫu item_card tương tự trang Dashboard của bạn
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/bidder.view/item_card.fxml"));
                Parent cardNode = loader.load();

                // Lấy controller của thẻ card truyền dữ liệu thật vào
                // (Chỉ cần ép kiểu hoặc gọi hàm set data tùy thuộc vào ItemCardController của bạn)
                Object controller = loader.getController();
                if (controller instanceof com.bidding.controller.bidder.ItemCardController itemCardController) {
                    itemCardController.setAuctionData(auction);
                }

                // Dựa vào trạng thái phiên (status từ DTO hoặc DB gửi về) để nạp vào đúng vùng giao diện
                // Trạng thái mẫu: RUNNING -> Đang diễn ra, OPEN -> Sắp bắt đầu, FINISHED -> Đã kết thúc
                String auctionStatus = auction.getStatus() != null ? auction.getStatus().toUpperCase() : "RUNNING";

                switch (auctionStatus) {
                    case "RUNNING" -> paneRunning.getChildren().add(cardNode);
                    case "OPEN"    -> paneOpen.getChildren().add(cardNode);
                    case "FINISHED", "PAID", "CANCELED" -> paneFinished.getChildren().add(cardNode);
                    default -> paneRunning.getChildren().add(cardNode);
                }

            } catch (IOException e) {
                System.err.println("Lỗi nạp mẫu giao diện item_card.fxml tại danh sách: " + e.getMessage());
                // Log exception instead of printing stack trace
            }
        }
    }
}