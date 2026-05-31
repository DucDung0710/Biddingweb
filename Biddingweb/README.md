# Biddingweb

Một hệ thống đấu giá trực tuyến nhỏ (demo) gồm Server, Client (JavaFX) và các module hỗ trợ.

Tóm tắt ngắn: Ứng dụng mô phỏng nền tảng đấu giá — cho phép người bán đăng sản phẩm, người mua/đấu thầu, và server xử lý phiên đấu giá theo thời gian thực.

Công nghệ & môi trường chạy:
- Java 21
- Maven để build và download dependency
- JavaFX cho client (phiên bản 21.x được sử dụng trong dự án này)
- Hệ điều hành: Windows, macOS, Linux (một vài lệnh chạy JavaFX module-path có thể cần điều chỉnh theo OS)

Cấu trúc chính (thư mục / module):
- `Biddingweb/demo` — ứng dụng demo (server + client) với mã nguồn Java và tài nguyên FXML.
- `scripts/` — các công cụ/kiểm tra hỗ trợ (ví dụ: kiểm tra FXML).
- `target/` — output build (class files, dependency).

Mô tả các package / module chính:
- `com.bidding.shared` (hoặc module `shared`): chứa các lớp dùng chung giữa client và server (DTO, model, hằng số, utility chung). Dùng để tránh lặp mã và đảm bảo contract giữa các bên.
- `com.bidding.engine` (hoặc module `engine`): triển khai logic cốt lõi của hệ thống đấu giá (xử lý phiên đấu, cơ chế so khớp giá, tính toán auto-bid, quy tắc business).
- `com.bidding.service` (hoặc module `service`): lớp service xử lý nghiệp vụ cao hơn, kết nối giữa `engine` và `persistence` (ví dụ: quản lý sản phẩm, người dùng, phiên đấu, giao dịch).
- `com.bidding.server`: thành phần mạng/Server (socket/http) chịu trách nhiệm lắng nghe kết nối, nhận/điều phối sự kiện, và gửi thông báo thời gian thực đến client.
- `com.bidding.client` / `com.bidding.app`: giao diện người dùng (JavaFX), controllers, và view (FXML). Chịu trách nhiệm hiển thị danh sách đấu giá, form đăng sản phẩm, và trải nghiệm người dùng.
- `db` / `scripts` : chứa script tạo schema, dữ liệu mẫu và các tiện ích hỗ trợ triển khai.

Lệnh dòng để build và chạy (lưu ý thay đổi đường dẫn JavaFX theo OS nếu cần):

Build và download dependency:
```
mvn -f Biddingweb/demo/pom.xml clean compile
mvn -f Biddingweb/demo/pom.xml dependency:copy-dependencies
```

Chạy Server (từ thư mục `Biddingweb/demo`):
```
java -cp "target/classes;target/dependency/*" com.bidding.server.ServerMain
```

Chạy Client / Ứng dụng (JavaFX) (Windows example — điều chỉnh `--module-path` cho Linux/macOS):
```
java --module-path "%USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\21.0.6\javafx-controls-21.0.6-win.jar;..." --add-modules javafx.controls,javafx.fxml -cp "target/classes;target/dependency/*" com.bidding.app.BiddingApplication
```

Hoặc chạy script tiện lợi (Windows):
```
cd Biddingweb/demo
run-app.bat
```

Ghi chú hệ điều hành:
- Trên Linux/macOS, đường phân cách classpath và module-path khác (`:` thay cho `;`).
- Kiểm tra kỹ `--module-path` cho JavaFX (thư viện platform-specific). Nếu gặp lỗi missing JavaFX, hãy cài JavaFX phù hợp với OS.

Danh sách chức năng đã hoàn thành (tóm tắt):
- Đăng/hiển thị sản phẩm (seller)
- Danh sách phiên đấu và tham gia đấu giá (bidder)
- Dashboard admin + quản lý người dùng, sản phẩm, phiên đấu
- Tích hợp socket/network cơ bản cho thời gian thực

Tài liệu bổ sung / demo:
- Báo cáo PDF: (thêm link ở đây khi có)
- Video demo: (thêm link ở đây khi có)

Liên hệ / phát triển tiếp: mở issue hoặc PR trên repo để thảo luận tính năng, bugfix hoặc hướng mở rộng (ví dụ tách `shared`, `engine`, `service` thành các module Maven riêng để reuse).

---
Xem chi tiết hướng dẫn nhanh và phần demo trong [Biddingweb/demo](Biddingweb/demo).