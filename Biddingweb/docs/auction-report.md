# Báo cáo chi tiết: Online Auction System - Bidding Web

Tài liệu này mô tả chi tiết hoạt động của các file chính và các phương thức quan trọng trong hệ thống.
Mục tiêu: cung cấp nội dung để xuất thành PDF (khoảng ~6 trang khi in).

---

## 1. Server entrypoint: `ServerMain` (com.bidding.server.ServerMain)

- File: `Biddingweb/demo/src/main/java/com/bidding/server/ServerMain.java`
- Chức năng chính: khởi động Services nền tảng, khởi tạo kết nối Database, lắng nghe kết nối Socket clients.

Phương thức chính:
- `public static void main(String[] args)`
  - Mô tả: Khởi tạo `DatabaseConnection` (singleton), mở `ServerSocket` trên cổng cấu hình (`NetworkConfig.SERVER_PORT`), lắng nghe vòng lặp accept và khởi thread cho mỗi client bằng `new Thread(new ClientHandler(clientSocket)).start();`.
  - Xử lý lỗi: bắt `BindException` khi cổng bị chiếm, `IOException` cho lỗi I/O, và `Exception` chung để đảm bảo server thoát an toàn khi có lỗi khởi tạo.

Tác động khi chạy:
- Tạo listener TCP cho client GUI; mỗi kết nối client sẽ được xử lý trong `ClientHandler` (thread riêng).

---

## 2. Client entrypoint: `BiddingApplication` (com.bidding.app.BiddingApplication)

- File: `Biddingweb/demo/src/main/java/com/bidding/app/BiddingApplication.java`
- Chức năng chính: Khởi tạo GUI (JavaFX) và services phụ trợ khi client chạy.

Phương thức chính:
- `public void start(Stage stage)`
  - Mô tả: Gọi `AppInitializer.initialize()` để khởi tạo các service nội bộ, thiết lập `SceneManager`, tải `login.fxml` qua `loadLoginScreen()`, set scene, đăng ký `stage.setOnCloseRequest` để gọi `AppInitializer.shutdown()`.
  - Các hành vi dự phòng: nếu không tải được FXML, hiển thị `StackPane` lỗi thay thế.
- `private Parent loadLoginScreen()`
  - Mô tả: Tải resource `uilogin.view/login.fxml` bằng `FXMLLoader`.
- `public static void main(String[] args)`
  - Mô tả: gọi `launch(args)` để khởi chạy JavaFX runtime.

Tác động khi chạy:
- Kết nối tới backend services nội bộ (qua `AppInitializer`) và hiển thị UI cho người dùng tương tác.

---

## 3. Core auction engine: `AuctionOperator` (com.bidding.engine.AuctionOperator)

- File: `Biddingweb/demo/src/main/java/com/bidding/engine/AuctionOperator.java`
- Chức năng chính: quản lý các `AuctionSession`, xử lý `placeBid`, `registerAutoBid`, `scheduleAuction`, auto-extension (anti-sniping), auto-bidding và persist lịch sử.

Lớp và các thành phần chính:
- Field quan trọng: `sessions` (ConcurrentHashMap roomId -> AuctionSession), `scheduler` (ScheduledExecutorService), `walletManager`, `auctionHistory`, `historyFile`.
- Hằng số: `MINIMUM_INCREMENT_FACTOR` (tăng tối thiểu 5%), `EXTENSION_WINDOW_MS` (10s), `EXTENSION_DURATION_MS` (3 phút) — cơ chế Anti-Sniping.

Phương thức cấp cao:
- `scheduleAuction(...)` (nhiều overload): tạo `AuctionSession`, lưu vào `sessions` và `scheduler.schedule(session::begin, delay, ...)`.
- `placeBid(String roomId, Users bidder, double bidAmount)`: tìm session và gọi `session.placeBid(...)`.
- `registerAutoBid(...)`: ghi cấu hình auto-bid cho session.
- `getSession`, `getAuctionHistory`, `shutdown`, `persistHistory`, `archiveSession`.

Inner class `AuctionSession` (lõi):
- Trạng thái: `AuctionStatus` (SCHEDULED, RUNNING, ENDED, CANCELLED).
- Phương thức chính:
  - `begin()`: chuyển trạng thái thành `RUNNING`, broadcast thông báo, gọi `scheduleAuctionEnd()`.
  - `placeBid(double, Users)`: logic phức tạp xử lý đặt giá, lock/unlock tiền qua `WalletManager`, gia hạn thời gian nếu bid trong `EXTENSION_WINDOW_MS`, gọi `broadcastNewBid`, update DB (JdbcAuctionDAO) nếu cần, kích hoạt `runAutoBids()`.
  - `registerAutoBid(...)`: lưu cấu hình auto-bid và cố gắng kích hoạt ngay.
  - `runAutoBids()`: thuật toán chọn auto-bid tốt nhất, lock tiền, cập nhật `currentPrice`, broadcast, lặp lại nếu cần.
  - `finishIfAuctionEnded()`, `completeAuction()`: hoàn tất phiên, commit tiền người thắng, phân phối (seller 90% / admin 10%), broadcast kết quả, ghi lịch sử.

Ghi chú an toàn:
- Dùng `ReentrantLock` (`bidLock`) cho từng session để tránh race condition trên thao tác đặt giá.
- Dùng collection thread-safe (ConcurrentHashMap, CopyOnWriteArrayList).

Tác động khi chạy:
- Đây là module chịu trách nhiệm thực thi nghiệp vụ đấu giá: mọi lệnh đặt giá từ GUI cuối cùng sẽ đi qua `AuctionOperator` / `AuctionSession`.

---

## 4. Service layer

Tóm tắt các lớp service chính (các file trong `com.bidding.service`):

### `AuctionService`
- File: `service/AuctionService.java`
- Vai trò: Bridge giữa UI controllers và `AuctionOperator` (initialize, schedule auction, place bid, register auto-bid, wallet ops, item ops).
- Phương thức nổi bật: `initialize(...)`, `getInstance()`, `scheduleAuction(...)` (gọi DAO để tạo record, sau đó gọi `auctionOperator.scheduleAuction`), `placeBid`, `registerAutoBid`, `shutdown`, `setCurrentAdmin`, `hasActiveAuction`.

### `BiddingService`
- File: `service/BiddingService.java`
- Vai trò: xử lý chi tiết lưu/vận hành đặt giá tới cơ sở dữ liệu (DAO), và engine auto-bid giữa DB và logic.
- Phương thức nổi bật: `synchronized placeBid(...)` (kiểm tra số dư, hold tiền, insert bid, update current price, kích hoạt `executeAutoBidEngine`), `executeAutoBidEngine(...)` — chạy vòng lặp auto-bid bằng truy vấn DB và cập nhật giá.

### `AuctionCompletionService`
- File: `service/AuctionCompletionService.java`
- Vai trò: kiểm tra phiên hết hạn và thực hiện chuyển tiền + cập nhật trạng thái (kết thúc phiên)
- Phương thức: `completeAuction(int auctionId)`, `checkAndCompleteExpiredAuctions()`.

### `UserService` và `WalletService`
- `UserService`: `login(...)`, `register(...)` — xử lý auth đơn giản.
- `WalletService`: nhiều phương thức quản lý ví (get balances, depositDirectly, hold/release, transfer, withdraw, commitLockedAmount, requestDeposit, approveDepositRequest). Cung cấp `WalletInfo` DTO.

Tác động: các `Controller` gọi `*Service` để thực thi nghiệp vụ; `Service` đảm bảo validation và phối hợp với `engine`/`dao`.

---

## 5. Shared utilities

- `WalletManager` (com.bidding.shared.WalletManager)
  - Quản lý các `Balance` trong memory, deposit, transfer, register wallet, pending requests.
  - Phương thức quan trọng: `registerNewWallet`, `getWalletByUserId`, `depositDirectly`, `transferMoney`, `countPendingRequests`.

- `UserSession` (singleton) cung cấp `getLoggedInUser`, `setLoggedInUser`, `logout()`.

---

## 6. Controllers (UI layer): ví dụ `SellerDashboardController`

- File: `controller/SellerDashboardController.java`
- Chức năng: điều khiển view seller dashboard, load products seller, filter, open product form, cập nhật stats.
- Phương thức/handlers: `initialize()`, `setupTable()`, `loadSellerProducts()`, `applyFilter()`, `updateStats()`, `handleAddProduct()`, `handleOut()`.

Tác động: các sự kiện UI (button, double-click, search) gọi controller, controller gọi `AppInitializer`/`AuctionService`/`ItemManager` để lấy dữ liệu và hiển thị.

---

## 7. Lưu ý đặc biệt — Anti-Sniping & Auto-Extension

- Cơ chế triển khai nằm ở `AuctionOperator.AuctionSession`:
  - Kiểm tra nếu `auctionEndTimeMillis - System.currentTimeMillis() <= EXTENSION_WINDOW_MS` (10s), thì khi có bid mới sẽ `auctionEndTimeMillis += EXTENSION_DURATION_MS` (3 phút) và gọi `scheduleAuctionEnd()` để reschedule.
  - Điều này bảo đảm tính công bằng và tránh sniping; logic này đã có trong cả `placeBid(...)` và `runAutoBids()`.

---

## 8. Kiến nghị để xuất PDF và mở rộng báo cáo

- Tệp hiện tại: `docs/auction-report.md` (nội dung này).
- Để chuyển sang PDF: dùng `pandoc` hoặc in từ VS Code Preview to PDF.

Ví dụ chuyển bằng `pandoc`:

```bash
pandoc docs/auction-report.md -o docs/auction-report.pdf --pdf-engine=xelatex -V geometry:margin=1in
```

---

### Ghi chú cuối

Nếu bạn muốn, tôi sẽ:
- Thêm danh sách đầy đủ từng phương thức (với signnature) cho mọi file trong `com.bidding` (mất thời gian hơn).
- Thử tự động convert sang PDF trên máy của bạn (yêu cầu `pandoc` + LaTeX cài đặt) hoặc dùng `wkhtmltopdf`.

Cho tôi biết bạn muốn xuất PDF luôn hay chỉ cần file Markdown để bạn review.