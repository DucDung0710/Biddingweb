# ⚡ QUICK START: Connect GUI + Engine

## 🎯 Mục Tiêu
Update GUI controllers để **thực sự** xử lý business logic thay vì chỉ hiển thị UI.

---

## 📋 IMMEDIATE ACTIONS

### 1️⃣ Verify Compilation
```bash
cd demo/
mvn clean compile
# Hoặc: javac -cp ... (nếu không có Maven)
```

**Check**: Có lỗi không?
- Nếu có: Fix imports
- Nếu không: Proceed to step 2

### 2️⃣ Test Backend Logic (xác nhận engine chạy)
```bash
java -cp target/classes com.bidding.Main
# Nên thấy output từ Main.java test case
```

**Expected Output**:
```
========== Số dư ban đầu ==========
Ví [User: seller | Available: 50000.0 ...]
Ví [User: bidder | Available: 250.0 ...]
...
[Bid] Đặt giá thành công! ...
========== Số dư sau khi đặt giá ==========
Ví [User: bidder | Available: 140.0 | Locked: 110.0 ...]
```

✅ Nếu thành công → Engine logic hoạt động

### 3️⃣ Test Service Layer (verify services work)
Tạo file: `TestAuctionService.java`

```java
package com.bidding.app;

public class TestAuctionService {
    public static void main(String[] args) {
        // Initialize
        AppInitializer.initialize();
        
        // Get services
        var auctionService = AppInitializer.getAuctionService();
        var walletService = AppInitializer.getWalletService();
        
        System.out.println("✓ Services initialized");
        System.out.println("✓ AuctionService: " + auctionService);
        System.out.println("✓ WalletService: " + walletService);
        
        // Test wallet query
        var info = walletService.getWalletInfo("1");
        System.out.println("✓ Admin wallet: " + info);
        
        AppInitializer.shutdown();
    }
}
```

Run:
```bash
java -cp target/classes com.bidding.app.TestAuctionService
```

✅ Nếu thành công → Services ready

### 4️⃣ Update First Controller (RealtimeBiddingController)

**In the file**: `RealtimeBiddingController.java`

**Find this code**:
```java
@FXML
private void initialize() {
    super.setupSidebarBehavior();
    setupRealtimeChart();
    initSocketConnection();  // ← Line to comment out if error
    AuctionDisplayDTO currentAuction = DataContext.getInstance().getCurrentAuction();
    // ... rest of initialization
}
```

**Add these lines AFTER super.setupSidebarBehavior()**:
```java
@FXML
private void initialize() {
    super.setupSidebarBehavior();
    
    // ========== NEW: Initialize Services ==========
    try {
        this.auctionService = AppInitializer.getAuctionService();
        this.walletService = AppInitializer.getWalletService();
        this.currentUser = DataContext.getInstance().getCurrentUser();
    } catch (Exception e) {
        System.err.println("Error initializing services: " + e.getMessage());
        e.printStackTrace();
    }
    
    setupRealtimeChart();
    initSocketConnection();
    // ... rest remains same
}
```

**Add as class variables** (top of class):
```java
public class RealtimeBiddingController extends BaseBidderController {
    // NEW: Services
    private AuctionService auctionService;
    private WalletService walletService;
    private Users currentUser;
    
    // Existing variables...
    private int currentAuctionId;
    // ... rest
}
```

**Find btnBid click handler** and update it:
```java
// BEFORE (probably looks like this):
@FXML
private void handleBidButton() {
    // Just updates UI without validation
}

// AFTER (should look like this):
@FXML
private void handleBidButton() {
    // Step 1: Validate input
    String bidStr = txtBidAmount.getText().trim();
    if (bidStr.isEmpty()) {
        lblBidError.setText("❌ Vui lòng nhập số tiền");
        return;
    }
    
    double bidAmount;
    try {
        bidAmount = Double.parseDouble(bidStr);
    } catch (NumberFormatException e) {
        lblBidError.setText("❌ Số tiền không hợp lệ");
        return;
    }
    
    if (bidAmount <= 0) {
        lblBidError.setText("❌ Số tiền phải > 0");
        return;
    }
    
    // Step 2: Check balance
    double available = walletService.getAvailableBalance(currentUser.getId());
    if (available < bidAmount) {
        lblBidError.setText(String.format("❌ Số dư không đủ (Còn: %.0f)", available));
        return;
    }
    
    // Step 3: Get auction info
    AuctionDisplayDTO auction = DataContext.getInstance().getCurrentAuction();
    if (auction == null) {
        lblBidError.setText("❌ Không tìm phiên đấu giá");
        return;
    }
    
    // ========== CRITICAL: CALL SERVICE ==========
    String roomId = auction.getRoomId(); // Need to add this field to DTO
    String result = auctionService.placeBid(roomId, currentUser, bidAmount);
    
    // Step 4: Handle result
    if (result.contains("thành công") || result.contains("successfully")) {
        lblBidError.setText("✓ Đặt giá thành công!");
        lblBidError.setStyle("-fx-text-fill: green;");
        
        // Update display
        lblCurrentPrice.setText(String.format("%.0f ₫", bidAmount));
        DataContext.getInstance().getCurrentAuction().setCurrentPrice(bidAmount);
        DataContext.getInstance().getCurrentAuction().setWinnerId((int)currentUser.getId());
        
        // Update balance display
        var info = walletService.getWalletInfo(currentUser.getId());
        lblBidError.setText(String.format("✓ Đặt giá thành công! Ví: %.0f ₫ (Khóa: %.0f ₫)",
            info.available, info.locked));
        
        txtBidAmount.clear();
    } else {
        lblBidError.setText("❌ " + result);
        lblBidError.setStyle("-fx-text-fill: red;");
    }
}
```

### 5️⃣ Add Missing Fields to AuctionDisplayDTO

**File**: `AuctionDisplayDTO.java`

**Current fields** (probably):
```java
private int auctionId;
private String itemName;
private double currentPrice;
private String sellerName;
private int winnerId;
private String status;
private String endTime;
private double startPrice;
private String type;
```

**ADD these fields**:
```java
// For GUI-to-Engine integration
private String roomId;              // ← CRITICAL: needed for placeBid()
private long timeRemaining;         // ms until auction ends
private int participantCount;       // number of bidders
private double minimumNextBid;      // validation for next bid
```

**Add getters/setters**:
```java
public String getRoomId() { return roomId; }
public void setRoomId(String roomId) { this.roomId = roomId; }

public long getTimeRemaining() { return timeRemaining; }
public void setTimeRemaining(long time) { this.timeRemaining = time; }

public int getParticipantCount() { return participantCount; }
public void setParticipantCount(int count) { this.participantCount = count; }

public double getMinimumNextBid() { return minimumNextBid; }
public void setMinimumNextBid(double bid) { this.minimumNextBid = bid; }
```

### 6️⃣ Test Updated Controller
```bash
# Start app
mvn javafx:run
# OR
java -cp target/classes:... com.bidding.app.BiddingApplication
```

**Test scenario**:
1. Login as bidder (any account from mock data)
2. Navigate to auction list
3. Click on auction → should load in RealtimeBiddingController
4. Enter bid amount
5. Click "Đặt giá"
6. ✅ Should see success message
7. ✅ Balance should update (locked amount shown)

---

## 📝 TIMELINE

**Today**: 
- ✅ Compile + test Main.java
- ✅ Compile + test new Services
- ✅ Update RealtimeBiddingController

**Tomorrow**:
- [ ] Update other wallet controllers
- [ ] Update ProductFormController
- [ ] Update AdminProductManagementController
- [ ] Enhance AuctionDisplayDTO

**This Week**:
- [ ] Full integration test
- [ ] Fix any issues
- [ ] Add logging/debugging

---

## 🐛 Troubleshooting

### Error: "AuctionService not initialized"
→ Make sure BiddingApplication calls AppInitializer.initialize()

### Error: "roomId is null"
→ Make sure AuctionDisplayDTO has roomId field populated

### Bid success but balance doesn't update
→ Call walletService.getWalletInfo() after placeBid()

### UI lag/freezing
→ Move placeBid() call to background thread (use Task<>)

---

## ✅ SUCCESS CRITERIA

When done, you should be able to:
1. ✅ Start app → See 3 mock users created
2. ✅ Login → Proceed to auction
3. ✅ View auction → See all info including roomId
4. ✅ Place bid → Get success/error message
5. ✅ Check wallet → See updated balance + locked amount
6. ✅ Main.java test output matches GUI workflow

---

## 📞 IF STUCK

1. Check `INTEGRATION_GUIDE.md` for detailed architecture
2. Check `RealtimeBiddingControllerExample.java` for reference code
3. Check `Main.java` to see expected workflow
4. Add `System.out.println()` to debug flow
5. Check console logs in IDE

Good luck! 🚀
