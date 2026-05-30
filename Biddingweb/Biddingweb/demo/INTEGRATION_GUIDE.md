# 🔗 INTEGRATION GUIDE: GUI + Engine

## Tóm Tắt Vấn Đề Hiện Tại

**Status**: ⚠️ **DISCONNECTED**
- ✅ Backend Engine (AuctionOperator, AuctionRoom) - Logic hoàn chỉnh
- ✅ Database Layer (DAO, Service) - Phần nào hoàn chỉnh
- ❌ **GUI Controllers - KHÔNG GỌI ENGINE**
- ❌ Service Layer - Chưa tồn tại (đã tạo mới)

---

## 📋 Kiến Trúc Mới (PROPOSED)

```
┌─────────────────────────────────────┐
│   JavaFX GUI (Controllers)          │  ← Gọi Services
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  Service Layer (NEW)                │
│  - AuctionService                   │
│  - WalletService                    │
│  - ItemService (TODO)               │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  Backend Managers (In-Memory)       │
│  - AuctionOperator                  │
│  - AuctionRoom                      │
│  - WalletManager                    │
│  - ItemManager                      │
│  - UserManager                      │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  Database Layer (DAO)               │
│  - SQLite (UserDAO, etc)            │
└─────────────────────────────────────┘
```

---

## ✅ ĐÃ HOÀN THÀNH

### 1. **AuctionService** ✓
- **File**: `com.bidding.service.AuctionService`
- **Chức năng**:
  - `scheduleAuction()` - Lên lịch phiên đấu giá
  - `placeBid()` - Đặt giá
  - `acceptAuctionInvitation()` - Chấp nhận lời mời
  - `getAuctionSession()` - Lấy info phiên
  - Và các wallet operations

### 2. **WalletService** ✓
- **File**: `com.bidding.service.WalletService`
- **Chức năng**:
  - `getAvailableBalance()`, `getLockedBalance()` - Xem số dư
  - `depositDirectly()` - Nạp tiền
  - `transfer()`, `withdraw()` - Chuyển/rút tiền
  - `lockAmount()`, `unlockAmount()` - Khóa/mở tiền
  - `hasEnoughBalance()` - Kiểm tra đủ tiền

### 3. **AppInitializer** ✓
- **File**: `com.bidding.app.AppInitializer`
- **Chức năng**:
  - Khởi tạo toàn bộ Managers
  - Khởi tạo Services
  - Tạo mock data (optional)
  - Shutdown clean

### 4. **BiddingApplication** ✓
- **Update**: Gọi `AppInitializer.initialize()` khi app start

---

## 🚀 CẦN LÀM NGAY (CRITICAL)

### 1. Update Controllers để sử dụng Services
Tất cả các controllers cần được update theo pattern trong `RealtimeBiddingControllerExample`:

```java
// ✓ ĐÚNG: Gọi service
private AuctionService auctionService = AppInitializer.getAuctionService();
String result = auctionService.placeBid(roomId, currentUser, bidAmount);
updateUI(result);

// ❌ SAI: Chỉ update UI
DataContext.getInstance().setCurrentPrice(newPrice);
lblCurrentPrice.setText(newPrice);
```

**Affected Controllers**:
- `RealtimeBiddingController` - gọi placeBid()
- `WalletBidderController` - gọi deposit, withdraw
- `WalletSellerController` - gọi transfer, deposit
- `ProductFormController` - gọi registerNewItem()
- `AdminProductManagementController` - gọi reviewItem()
- `LogInController` - gọi authenticate
- `SignUpController` - gọi register

### 2. Enhance AuctionDisplayDTO
Thêm các field cần thiết:

```java
public class AuctionDisplayDTO {
    // Hiện tại:
    private int auctionId;
    private String itemName;
    private double currentPrice;
    // ...
    
    // CẦN THÊM:
    private String roomId;           // Để gọi placeBid()
    private String status;           // active, ended, etc
    private long timeRemaining;      // ms (để countdown timer)
    private int participantCount;    // Số người tham gia
    private double minimumNextBid;   // Bid tối thiểu tiếp theo
}
```

### 3. Create ItemService (TODO)
Tương tự như AuctionService:
```java
public class ItemService {
    public String registerNewItem(String userId, String name, String desc, double price);
    public Item getSellerItem(String userId);
    public void updateItem(String userId, String name, String desc);
    public void reviewItem(Users admin, String userId, boolean approve);
}
```

### 4. WebSocket/Real-time Updates (OPTIONAL)
Hiện tại placeBid() là synchronous. Để real-time:
- Cần setup WebSocket hoặc polling
- Push updates từ engine khi có bid mới
- Update UI tất cả connected clients

---

## 📝 INTEGRATION CHECKLIST

### Phase 1: Basic Integration (URGENT)
- [ ] Compile project với new Services
- [ ] Update RealtimeBiddingController 
- [ ] Update WalletBidderController
- [ ] Update WalletSellerController
- [ ] Test: Login → View Auction → Place Bid → Check Balance
- [ ] Test: Main.java logic tương tự phía GUI

### Phase 2: Complete Integration
- [ ] Update ProductFormController (register item)
- [ ] Update AdminProductManagementController (review item)
- [ ] Update LogInController (use AppInitializer)
- [ ] Update SignUpController
- [ ] Add ItemService
- [ ] Add notifications/notifications updates

### Phase 3: Enhancement
- [ ] Add WebSocket for real-time updates
- [ ] Add offline support
- [ ] Add history tracking
- [ ] Performance optimization

---

## 🧪 Testing Strategy

### Test 1: Service Initialization
```java
// File: tests/AppInitializerTest.java
AppInitializer.initialize();
AuctionService service = AppInitializer.getAuctionService();
assertTrue(service != null);
```

### Test 2: Place Bid Flow
```java
// Simulate GUI action
Users bidder = AppInitializer.getUserManager().signIn("bidder", "bidderpass");
String result = auctionService.placeBid(roomId, bidder, 110.0);
assertTrue(result.contains("thành công"));

// Verify wallet locked
double locked = walletService.getLockedBalance(bidder.getId());
assertTrue(locked >= 110.0);
```

### Test 3: Full Workflow (Main.java pattern)
```java
// 1. Create users
// 2. Register item
// 3. Review item
// 4. Schedule auction
// 5. Accept invitation
// 6. Place bid
// 7. Check balances
// 8. Verify winner
```

---

## 🔧 Implementation Tips

### How to Update a Controller:

**BEFORE** (Current):
```java
public class RealtimeBiddingController {
    @FXML
    private void handleBidButton() {
        double bidAmount = Double.parseDouble(txtBidAmount.getText());
        DataContext.getInstance().setCurrentPrice(bidAmount);
        lblCurrentPrice.setText(String.format("%.0f ₫", bidAmount));
    }
}
```

**AFTER** (With Service):
```java
public class RealtimeBiddingController {
    private AuctionService auctionService;
    
    @FXML
    public void initialize() {
        auctionService = AppInitializer.getAuctionService();
    }
    
    @FXML
    private void handleBidButton() {
        double bidAmount = Double.parseDouble(txtBidAmount.getText());
        Users user = DataContext.getInstance().getCurrentUser();
        String roomId = /* get từ auction */;
        
        // Gọi SERVICE
        String result = auctionService.placeBid(roomId, user, bidAmount);
        
        // Update UI
        if (result.contains("thành công")) {
            DataContext.getInstance().setCurrentPrice(bidAmount);
            lblCurrentPrice.setText(String.format("%.0f ₫", bidAmount));
        } else {
            lblError.setText("Lỗi: " + result);
        }
    }
}
```

---

## ⚠️ Common Issues & Solutions

### Issue 1: "AuctionService not initialized"
**Cause**: Controllers load trước khi AppInitializer.initialize() gọi
**Solution**: Đảm bảo BiddingApplication gọi AppInitializer.initialize() trước khi load FXML

### Issue 2: DataContext không có roomId
**Cause**: AuctionDisplayDTO chưa có roomId field
**Solution**: Thêm roomId vào DTO + populate khi load auction

### Issue 3: Wallet không update real-time
**Cause**: UI không refresh sau khi placeBid()
**Solution**: Gọi `walletService.getWalletInfo()` + update lblBalance

### Issue 4: Multiple users/sessions
**Cause**: AppInitializer.initialize() chỉ tạo 1 set mock data
**Solution**: Implement proper user session management

---

## 📞 When Complete

Khi hoàn thành integration:
1. ✓ GUI gọi backend logic (không chỉ update UI)
2. ✓ Ví tiền tự động update
3. ✓ Phiên đấu giá quản lý đúng
4. ✓ Có thể chạy full workflow: Login → Bid → Check Balance
5. ✓ Logic giống Main.java test case

---

## 📚 Relevant Files

| File | Purpose | Status |
|------|---------|--------|
| `AuctionService.java` | Bridge GUI + Engine | ✅ NEW |
| `WalletService.java` | Wallet management | ✅ NEW |
| `AppInitializer.java` | System setup | ✅ NEW |
| `BiddingApplication.java` | App entry | ✅ UPDATED |
| `RealtimeBiddingController.java` | Bidding UI | ⏳ TODO UPDATE |
| `AuctionDisplayDTO.java` | Data transfer | ⏳ TODO ENHANCE |
| `Main.java` | Reference implementation | ✅ COMPLETE |
