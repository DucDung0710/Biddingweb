package com.bidding.controller.bidder;

import com.bidding.model.AuctionDisplayDTO;
import com.bidding.util.DataContext;
import com.bidding.util.SocketClient;
import com.bidding.util.SceneManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DashboardController extends BaseBidderController {
    @FXML private Button txtTopSearch;
    @FXML @SuppressWarnings("unused") private Button btnViewAll;

    @FXML private Label lblStatActive;
    @FXML private Label lblStatLeading;
    @FXML private Label lblStatTotal;
    @FXML private Label lblStatWon;

    @FXML private HBox auctionContainer;
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        super.setupSidebarBehavior();
        if (navDashboard!= null) {
            navDashboard.setOnMouseClicked(null);
        }

        try {
            // 1. Lấy trạng thái người dùng hiện tại thông qua SUserSession Singleton
            com.bidding.shared.Users currentUser = com.bidding.shared.UserSession.getInstance().getLoggedInUser();
            int currentUserId = (currentUser != null) ? currentUser.getId() : 0;

            // 2. LẤY SỐ LIỆU THỐNG KÊ TỪ SERVER QUA SOCKET
            JsonObject statRequest = new JsonObject();
            statRequest.addProperty("action", "GET_DASHBOARD_STATS");
            statRequest.addProperty("userId", currentUserId);

            JsonObject statResponse = SocketClient.getInstance().sendRequest(statRequest);

            if (statResponse.get("status").getAsString().equals("OK")) {
                int activeCount = statResponse.get("activeCount").getAsInt();
                int wonCount = statResponse.get("wonCount").getAsInt();

                lblStatActive.setText(String.valueOf(activeCount));
                lblStatWon.setText(String.valueOf(wonCount));
                lblStatLeading.setText(String.valueOf(activeCount)); // logic tạm thời của bạn
            } else {
                System.err.println("Không lấy được thống kê từ server: " + statResponse.get("message").getAsString());
            }

            // Hiển thị số dư
            if (currentUser != null) {
                lblStatTotal.setText(String.format("%,.0f ₫", currentUser.getBalance()));
            } else {
                lblStatTotal.setText("0 ₫");
            }

            // 3. ĐỔ SẢN PHẨM THEO KIẾN TRÚC CLIENT - SERVER
            loadActiveAuctionsWithNetwork();

        } catch (Exception e) {
            System.err.println("Lỗi nghiêm trọng khi khởi tạo Dashboard: " + e.getMessage());
            // Log exception instead of printing stack trace
        }
    }

    private void loadActiveAuctionsWithNetwork() {
        auctionContainer.getChildren().clear();

        // Đóng gói yêu cầu lấy danh sách phiên đấu giá đang chạy
        JsonObject listRequest = new JsonObject();
        listRequest.addProperty("action", "GET_ACTIVE_AUCTIONS");

        JsonObject listResponse = SocketClient.getInstance().sendRequest(listRequest);

        if (!listResponse.get("status").getAsString().equals("OK")) {
            Label lblError = new Label("Không thể tải danh sách sản phẩm từ Server.");
            lblError.setStyle("-fx-text-fill: red; -fx-font-style: italic;");
            auctionContainer.getChildren().add(lblError);
            return;
        }

        // Parse JsonArray từ Server gửi về thành List<AuctionDisplayDTO>
        JsonArray jsonArray = listResponse.getAsJsonArray("auctions");
        Type listType = new TypeToken<ArrayList<AuctionDisplayDTO>>(){}.getType();
        List<AuctionDisplayDTO> activeAuctions = gson.fromJson(jsonArray, listType);

        if (activeAuctions == null || activeAuctions.isEmpty()) {
            Label lblEmpty = new Label("Hiện tại chưa có mặt hàng nào lên sàn.");
            lblEmpty.setStyle("-fx-text-fill: #999999; -fx-font-style: italic; -fx-font-size: 13px;");
            auctionContainer.getChildren().add(lblEmpty);
            return;
        }

        // Duyệt thủ công từng phần tử JSON để đảm bảo không một trường nào bị bỏ sót hay sai tên biến
        for (int i = 0; i < jsonArray.size(); i++) {
            try {
                JsonObject obj = jsonArray.get(i).getAsJsonObject();
                AuctionDisplayDTO dto = new AuctionDisplayDTO();

                // Đọc chính xác các trường cốt lõi
                dto.setAuctionId(obj.get("auctionId").getAsInt());
                dto.setItemId(obj.get("itemId").getAsInt());
                dto.setItemName(obj.get("itemName").getAsString());

                // Đọc các trường chi tiết (Thủ phạm gây trống dữ liệu)
                dto.setDescription(obj.has("description") && !obj.get("description").isJsonNull() ? obj.get("description").getAsString() : "");
                dto.setType(obj.has("type") && !obj.get("type").isJsonNull() ? obj.get("type").getAsString() : "Chưa phân loại");
                dto.setSellerName(obj.has("sellerName") && !obj.get("sellerName").isJsonNull() ? obj.get("sellerName").getAsString() : "Ẩn danh");

                // Đọc thông tin giá cả và thời gian
                dto.setStartPrice(obj.get("startPrice").getAsDouble());
                dto.setCurrentPrice(obj.get("currentPrice").getAsDouble());
                dto.setStartTime(obj.has("startTime") && !obj.get("startTime").isJsonNull() ? obj.get("startTime").getAsString() : "");
                dto.setEndTime(obj.has("endTime") && !obj.get("endTime").isJsonNull() ? obj.get("endTime").getAsString() : "");
                dto.setStatus(obj.get("status").getAsString());
                dto.setWinnerId(obj.has("winnerId") && !obj.get("winnerId").isJsonNull() ? obj.get("winnerId").getAsInt() : 0);

                // Nạp và hiển thị lên giao diện Card
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/bidder.view/item_card.fxml"));
                Parent cardNode = loader.load();

                ItemCardController cardController = loader.getController();
                cardController.setAuctionData(dto); // Truyền DTO đã bóc tách đầy đủ vào đây

                auctionContainer.getChildren().add(cardNode);
            } catch (IOException e) {
                System.err.println("Lỗi nạp file mẫu giao diện item_card.fxml: " + e.getMessage());
                // Log exception instead of printing stack trace
            } catch (Exception ex) {
                System.err.println("Lỗi phân rã cấu trúc JSON sản phẩm: " + ex.getMessage());
                // Log exception instead of printing stack trace
            }
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleTopSearch() {
        if (txtTopSearch != null) {
            String keyword = txtTopSearch.getText().trim();
            if (!keyword.isEmpty()) {
                DataContext.getInstance().setSearchKeyword(keyword);
                SceneManager.switchToAuctionList();
            }
        }
    }
    @FXML
    @SuppressWarnings("unused")
    private void handleViewAll() {
        SceneManager.switchToAuctionList();
    }
}