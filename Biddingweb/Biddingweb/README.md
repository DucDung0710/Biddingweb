# Biddingweb

This README contains quick-start and module descriptions for the demo application contained in `demo/`.

Mục tiêu: cung cấp đủ thông tin để người phát triển build, chạy Server/Client và hiểu cấu trúc module/package chính.

1) Tóm tắt và phạm vi hệ thống
- Ứng dụng demo cho hệ thống đấu giá: server xử lý phiên đấu thời gian thực; client (JavaFX) dùng để hiển thị và tham gia đấu giá.

2) Công nghệ chính và yêu cầu cài đặt
- Java 21
- Maven
- JavaFX 21.x (thư viện platform-specific — chọn đúng JAR cho Windows/macOS/Linux)

3) Cấu trúc thư mục / module chính
- `demo/` — mã nguồn ứng dụng demo (server + client). Xem [demo/pom.xml](demo/pom.xml) để biết cấu hình build.
- `scripts/` — tiện ích và kiểm tra giúp validate FXML hoặc cấu trúc UI.

4) Mô tả package/ module chi tiết
- `shared` — Lớp/khối dùng chung: DTO, model, constants, helper utilities. Mục đích: giảm lặp và đảm bảo contract giữa client/server.
- `engine` — Core auction engine: quản lý luồng phiên đấu, xử lý auto-bid, so khớp giá, ràng buộc business rule.
- `service` — Business service layer: orchestration giữa `engine` và persistence, xử lý use-cases như publishProduct(), placeBid(), closeAuction().
- `server` — Thành phần mạng: socket handling, thread management, broadcast các sự kiện thời gian thực tới client.
- `client/app` — JavaFX app: controllers, views (FXML), và logic UI.
- `db`/`scripts` — SQL schema và script tiện ích (thư mục `demo/scripts` hoặc root `scripts/`).

5) Lệnh chạy nhanh
- Build & download dependency:
```
mvn -f demo/pom.xml clean compile
mvn -f demo/pom.xml dependency:copy-dependencies
```
- Chạy Server (từ `demo`):
```
java -cp "target/classes;target/dependency/*" com.bidding.server.ServerMain
```
- Chạy Client (JavaFX): xem `run-app.bat` hoặc sử dụng lệnh `java` với `--module-path` tương ứng cho JavaFX.

6) Hướng dẫn OS khác nhau
- Windows: classpath phân tách bằng `;` và JavaFX JAR thường có hậu tố `-win`.
- macOS/Linux: dùng `:` cho classpath/module-path; dùng JAR phù hợp platform.

7) Những gì đã hoàn thành
- Tính năng seller/bidder/admin cơ bản, giao diện FXML, server thời gian thực đơn giản.

8) Tài liệu bổ sung
- Xem `demo/QUICK_START.md` và `demo/INTEGRATION_GUIDE.md` để biết hướng dẫn chi tiết hơn.
- Báo cáo PDF / Video demo: thêm link khi có.

Nếu bạn muốn, tôi có thể tách `shared`, `engine`, `service` thành sub-modules Maven và cập nhật `pom.xml` để quản lý phụ thuộc rõ ràng hơn.