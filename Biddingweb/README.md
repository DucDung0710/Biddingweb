# 📝 ONLINE AUCTION SYSTEM - BIDDING WEB PROJECT

## 1. Giới thiệu dự án
Hệ thống Đấu giá trực tuyến (Online Auction System) được xây dựng theo kiến trúc **Client-Server**, sử dụng **Java 21** và **JavaFX**. Dự án tập trung vào tính thực tế, xử lý đồng thời và tối ưu hóa hiệu năng trên nền tảng Cloud.

### Đặc điểm kỹ thuật nổi bật:

- **Cloud Database:** Triển khai MySQL trên **Aiven Cloud**, đảm bảo tính sẵn sàng cao.
- **Performance:** Tích hợp **HikariCP** để quản lý Connection Pool, giảm lag khi có nhiều phiên đấu giá cùng lúc.
- **Realtime:** Cập nhật giá thầu tức thời qua **Socket** (mô hình Observer).

## 2. Kiến trúc và Công nghệ (MVC & Design Patterns)
Dự án được thiết kế chuẩn hóa để đáp ứng các tiêu chí chấm điểm kỹ thuật:

- **Mô hình MVC:** Phân tách rõ ràng giữa **Model** (Entity, DAO), **View** (JavaFX FXML), và **Controller**.
- **Design Patterns:** **Singleton:** Quản lý kết nối Database và Socket Client. **Factory:** Khởi tạo các loại người dùng (Admin, Seller, Bidder).
- **Observer:** Cập nhật trạng thái phiên đấu giá và giá thầu realtime.

- **Database:** MySQL 8.x (Aiven) + **DAO Pattern** để quản lý truy vấn.

### Các module / package chính

- `app` — Client (JavaFX) entrypoint và UI controllers.
- `server` — Logic Server, Socket handler và entrypoint `ServerMain`.
- `controller` — JavaFX controllers (ứng xử theo MVC).
- `model` — Business entities và validation logic.
- `dao` — Data Access Objects, mapping tới MySQL.
- `util` — Helper classes (HikariCP config, Socket client, NetworkConfig).
- `shared` — Các util, DTO, và hằng số dùng chung giữa client & server.
- `engine` — Logic đấu giá lõi: xử lý bid, so sánh, auto-bidding, anti-sniping.
- `service` — Lớp trung gian (service layer) kết hợp DAO và engine để thực thi nghiệp vụ.

## 3. Danh sách chức năng (Theo tiêu chí chấm điểm)

### Chức năng bắt buộc (Core):

- [x] **Quản lý người dùng:** Đăng ký, đăng nhập, phân quyền (Role-based Access Control).
- [x] **Quản lý sản phẩm:** Seller thực hiện CRUD sản phẩm và thiết lập thông số đấu giá.
- [x] **Đấu giá Realtime:** Đặt giá thầu, kiểm tra tính hợp lệ và cập nhật tức thời cho tất cả client qua Socket.
- [x] **Xử lý kết thúc:** Tự động đóng phiên khi hết giờ và xác định người thắng cuộc.
- [x] **Xử lý ngoại lệ:** Kiểm soát lỗi mạng, lỗi nhập liệu và lỗi logic nghiệp vụ.

### Chức năng nâng cao & Sáng tạo (Bonus):

- [x] **Auto-Bidding:** Hệ thống tự động đặt giá thầu dựa trên mức giá tối đa người dùng thiết lập.
- [x] **Anti-Sniping (Auto-Extension):** Tự động gia hạn thời gian đấu giá (thêm 1-2 phút) nếu có người đặt thầu trong những giây cuối cùng, đảm bảo tính công bằng.
- [x] **Concurrency Handling:** Đảm bảo tính toàn vẹn dữ liệu (Atomic updates) khi nhiều người cùng bid ở một mili giây.

## 4. Kiểm thử và Quy trình phát triển (Quality Assurance)

- **Unit Test:** Sử dụng **JUnit 5** để kiểm thử logic nghiệp vụ (DAO, Bid Validation).
- **CI/CD:** Tích hợp **GitHub Actions** để tự động build và kiểm tra mã nguồn sau mỗi lần commit.
- **Coding Convention:** Tuân thủ quy tắc đặt tên và cấu trúc code Java chuẩn.

## 5. Hướng dẫn cài đặt & Khởi chạy

### Bước 1: Biên dịch (Maven)
```bash
mvn clean package
```

### Bước 2: Chạy Server (Cổng mặc định: 8080)
```bash
java -cp "target/classes;target/dependency/*" com.bidding.server.ServerMain
```

### Bước 3: Chạy Client (JavaFX)
```bash
java --module-path /lib --add-modules javafx.controls,javafx.fxml -cp "target/classes;target/dependency/*" com.bidding.app.BiddingApplication
```

## 6. Cấu trúc thư mục dự án (đã sửa cho khớp với repo)

```
Biddingweb/
├── README.md                       # README chính (gốc)
├── scripts/                        # Các script tiện ích (check_fxml_*.py, validate_fxml_xml.py)
├── target/                         # Build outputs (cấp repo)
└── Biddingweb/
	├── README.md                   # README module/demo
	├── demo/
	│   ├── pom.xml
	│   ├── run-app.bat
	│   ├── compile_out.txt
	│   ├── cp.txt
	│   ├── src/
	│   │   ├── main/
	│   │   │   ├── java/
	│   │   │   │   └── com/bidding/
	│   │   │   │       ├── app/         # Client (JavaFX) entrypoint & UI
	│   │   │   │       ├── server/      # Server entrypoint & Socket logic
	│   │   │   │       ├── controller/  # JavaFX controllers (MVC)
	│   │   │   │       ├── model/       # Business entities & validation
	│   │   │   │       ├── dao/         # Data Access Objects
	│   │   │   │       ├── service/     # Service layer (AuctionService, UserService, WalletService)
	│   │   │   │       ├── shared/      # Shared DTOs & utilities (UserSession, Item, WalletManager)
	│   │   │   │       ├── engine/      # Core auction logic (AuctionOperator, anti-sniping)
	│   │   │   │       ├── util/        # Helpers (HikariCP, NetworkConfig, SocketClient)
	│   │   │   │       └── classes_test/ # Test helper classes
	│   │   │   └── resources/
	│   │   │       ├── admin.view/
	│   │   │       ├── bidder.view/
	│   │   │       ├── seller.view/
	│   │   │       └── uilogin.view/
	│   └── target/                     # Demo build outputs
	└── target/
```

## 7. Ghi chú về Anti-Sniping / Auto-Extension

- Tính năng **Anti-Sniping**: khi có bid trong khoảng cuối cùng (ví dụ 30s trước khi kết thúc), hệ thống sẽ tự động gia hạn thêm một khoảng thời gian cấu hình được (ví dụ +60–120s). Giải pháp được implement trong module `engine` và được phối hợp bởi `service` để đảm bảo atomicity và phát notification qua Socket.

## 8. Tài liệu & Demo

- Báo cáo PDF và video demo: (đính kèm tại thư mục `docs/` nếu có)

---
_Nội dung đã được cập nhật từ mẫu yêu cầu. Nếu muốn đổi văn phong (tiếng Anh/Việt) hoặc thêm link tài liệu, báo mình biết._