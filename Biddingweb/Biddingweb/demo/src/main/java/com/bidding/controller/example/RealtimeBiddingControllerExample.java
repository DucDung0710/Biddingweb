package com.bidding.controller.example;

import com.bidding.app.AppInitializer;
import com.bidding.model.AuctionDisplayDTO;
import com.bidding.service.AuctionService;
import com.bidding.service.WalletService;
import com.bidding.shared.Users;
import com.bidding.util.DataContext;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * EXAMPLE: RealtimeBiddingController - Cách sử dụng Services
 * 
 * Đây là ví dụ minh họa cách update các controllers hiện tại
 * để sử dụng AuctionService + WalletService thay vì chỉ update UI
 * 
 * Các bước:
 * 1. Inject AuctionService + WalletService
 * 2. Khi user click button "Đặt giá", gọi auctionService.placeBid()
 * 3. Cập nhật UI dựa vào kết quả từ service
 * 4. Hiển thị error/success message
 */
public class RealtimeBiddingControllerExample {
    
    // Services injected từ AppInitializer
    private AuctionService auctionService;
    private WalletService walletService;
    private Users currentUser;
    
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLeader;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnBid;
    @FXML private Label lblError;
    @FXML private Label lblBalance;
    
    /**
     * Initialize được gọi khi FXML load
     */
    @FXML
    public void initialize() {
        // Lấy services từ AppInitializer
        this.auctionService = AppInitializer.getAuctionService();
        this.walletService = AppInitializer.getWalletService();
        
        // Lấy current user từ DataContext
        this.currentUser = DataContext.getInstance().getCurrentUser();
        
        if (currentUser != null) {
            updateBalanceDisplay();
        }
        
        // Setup button click handler
        btnBid.setOnAction(e -> handlePlaceBid());
    }
    
    /**
     * Handler khi user click button "Đặt giá"
     */
    private void handlePlaceBid() {
        // 1. Validate input
        String bidAmountStr = txtBidAmount.getText().trim();
        if (bidAmountStr.isEmpty()) {
            showError("Vui lòng nhập số tiền");
            return;
        }
        
        double bidAmount;
        try {
            bidAmount = Double.parseDouble(bidAmountStr);
        } catch (NumberFormatException e) {
            showError("Số tiền không hợp lệ");
            return;
        }
        
        if (bidAmount <= 0) {
            showError("Số tiền phải lớn hơn 0");
            return;
        }
        
        // 2. Kiểm tra user có đủ tiền không
        double availableBalance = walletService.getAvailableBalance(currentUser.getId());
        if (availableBalance < bidAmount) {
            showError(String.format("Số dư không đủ (Còn lại: %.0f)", availableBalance));
            return;
        }
        
        // 3. LẤY AUCTION INFO
        AuctionDisplayDTO currentAuction = DataContext.getInstance().getCurrentAuction();
        if (currentAuction == null) {
            showError("Không tìm thấy phiên đấu giá");
            return;
        }
        
        String auctionId = String.valueOf(currentAuction.getAuctionId());
        
        // ========== ĐÂY LÀ ĐIỂM CHÍNH: GỌI ENGINE ==========
        // 4. GỌI AuctionService để XỬ LÝ BID
        String result = auctionService.placeBid(auctionId, currentUser, bidAmount);
        
        // 5. Xử lý kết quả
        if (result.contains("thành công") || result.contains("successfully")) {
            // ✓ Bid thành công
            showSuccess(result);
            
            // Cập nhật UI
            updateAuctionDisplay();
            updateBalanceDisplay();
            
            // Clear input
            txtBidAmount.clear();
            
        } else {
            // ✗ Bid thất bại
            showError(result);
        }
    }
    
    /**
     * Cập nhật hiển thị số dư ví
     */
    private void updateBalanceDisplay() {
        WalletService.WalletInfo walletInfo = walletService.getWalletInfo(currentUser.getId());
        lblBalance.setText(String.format("Tài khoản: %.0f ₫ (Khóa: %.0f ₫)",
            walletInfo.available, walletInfo.locked));
    }
    
    /**
     * Cập nhật hiển thị thông tin phiên đấu giá
     */
    private void updateAuctionDisplay() {
        AuctionDisplayDTO auction = DataContext.getInstance().getCurrentAuction();
        if (auction != null) {
            lblCurrentPrice.setText(String.format("Giá hiện tại: %.0f ₫", auction.getCurrentPrice()));
            lblLeader.setText("Người dẫn đầu: " + auction.getWinnerId());
        }
    }
    
    /**
     * Hiển thị error message
     */
    private void showError(String message) {
        lblError.setText("❌ " + message);
        lblError.setStyle("-fx-text-fill: red;");
    }
    
    /**
     * Hiển thị success message
     */
    private void showSuccess(String message) {
        lblError.setText("✓ " + message);
        lblError.setStyle("-fx-text-fill: green;");
    }
    
    // ==================== THÊM FIELDS VÀO DTO ====================
    
    /**
     * NOTE: Cần thêm các field vào AuctionDisplayDTO:
     * - roomId: String (để call placeBid)
     * - status: String (để check xem phiên còn hoạt động không)
     * - timeRemaining: long (thời gian còn lại tính bằng ms)
     * 
     * Các field này cần được populate khi loading auction từ database
     */
}
