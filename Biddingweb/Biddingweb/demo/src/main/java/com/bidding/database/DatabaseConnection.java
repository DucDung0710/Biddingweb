package com.bidding.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private final HikariDataSource dataSource;

    private static final String DB_HOST = getEnv("DB_HOST", "mysql-7603b93-vnu-d637.l.aivencloud.com");
    private static final String DB_PORT = getEnv("DB_PORT", "20763");
    private static final String DB_NAME = getEnv("DB_NAME", "biddingdb");
    private static final String DB_USER = getEnv("DB_USER", "avnadmin");
    private static final String DB_PASSWORD = getEnv("DB_PASSWORD", "AVNS_iQ9fobJ2RsRXOaJVIKQ");

    // Tối ưu URL kết nối bằng cách gộp tham số cấu hình mã hóa mã nguồn chuẩn cho MySQL Cloud
    private static final String DB_URL = String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8",
            DB_HOST, DB_PORT, DB_NAME
    );

    private DatabaseConnection() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        /* ====================================================================
         * 🔥 CẤU HÌNH TỐI ƯU HIKARICP DÀNH CHO MYSQL CLOUD (AIVEN)
         * ==================================================================== */

        // 1. Tăng kích thước Pool phù hợp cho ứng dụng đa luồng client-server
        config.setMaximumPoolSize(15); // Cho phép tối đa 15 kết nối đồng thời thay vì 3
        config.setMinimumIdle(5);       // Luôn giữ ít nhất 5 kết nối "sống" chờ sẵn để dùng ngay lập tức

        // 2. Tối ưu hóa thời gian Timeout nhằm tránh nghẽn luồng xếp hàng
        config.setConnectionTimeout(15000); // 15 giây chờ tối đa để lấy kết nối (quá thời gian sẽ báo lỗi thay vì treo vô hạn)
        config.setIdleTimeout(300000);      // 5 phút: Giải phóng bớt các kết nối thừa khi hệ thống rảnh
        config.setMaxLifetime(900000);     // 15 phút: Tự động làm mới kết nối để tránh lỗi "gãy kết nối ngầm" từ phía Cloud

        // 3. Kích hoạt tính năng CACHE PREPARED STATEMENT (Cực kỳ quan trọng để giảm delay 2s)
        // Giúp lưu lại cấu trúc câu lệnh SQL ở local, không cần gửi thô lên Cloud biên dịch lại mỗi lần gọi
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true"); // Thực thi tiền biên dịch phía Server

        // 4. Các cấu hình tối ưu hiệu năng đọc/ghi dữ liệu mạng
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("useLocalTransactionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false"); // Tắt thống kê thời gian chạy để giảm tải CPU

        this.dataSource = new HikariDataSource(config);

        // Khởi tạo cấu trúc bảng dữ liệu một lần duy nhất lúc bật ứng dụng
        initializeDatabase();
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null) ? value : defaultValue;
    }

    /**
     * Hàm khởi tạo cấu trúc bảng hệ thống (Chỉ chạy một lần duy nhất khi khởi tạo Instance ban đầu)
     */
    private void initializeDatabase() {
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "username VARCHAR(255) NOT NULL UNIQUE, "
                + "password VARCHAR(255) NOT NULL, "
                + "role VARCHAR(50) NOT NULL, "
                + "balance DOUBLE DEFAULT 0.0"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String sqlItems = "CREATE TABLE IF NOT EXISTS items ("
                + "id INT PRIMARY KEY, "
                + "name VARCHAR(255) NOT NULL, "
                + "description TEXT, "
                + "type VARCHAR(100), "
                + "seller_id INT, "
                + "FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String sqlAuctions = "CREATE TABLE IF NOT EXISTS auctions ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "item_id INT, "
                + "start_price DOUBLE NOT NULL, "
                + "current_price DOUBLE NOT NULL, "
                + "start_time VARCHAR(100), "
                + "end_time VARCHAR(100), "
                + "status VARCHAR(50) DEFAULT 'OPEN', "
                + "winner_id INT NULL, "
                + "FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (winner_id) REFERENCES users(id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String sqlBids = "CREATE TABLE IF NOT EXISTS bid_transactions ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "auction_id INT, "
                + "bidder_id INT, "
                + "amount DOUBLE NOT NULL, "
                + "bid_time VARCHAR(100), "
                + "is_auto INT DEFAULT 0, "
                + "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String sqlAuto = "CREATE TABLE IF NOT EXISTS auto_bids ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "auction_id INT, "
                + "user_id INT, "
                + "max_amount DOUBLE NOT NULL, "
                + "increment_amount DOUBLE NOT NULL, "
                + "created_at VARCHAR(100), "
                + "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" // Đã xóa chữ user_id_fk bị thừa
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String sqlWalletTx = "CREATE TABLE IF NOT EXISTS wallet_transactions ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "user_id INT, "
                + "type VARCHAR(50), "
                + "amount DOUBLE NOT NULL, "
                + "balance_after DOUBLE NOT NULL, "
                + "ref_id INT, "
                + "note VARCHAR(255), "
                + "created_at VARCHAR(100), "
                + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String sqlHolds = "CREATE TABLE IF NOT EXISTS wallet_holds ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "user_id INT, "
                + "auction_id INT, "
                + "amount DOUBLE NOT NULL, "
                + "status VARCHAR(50) DEFAULT 'ACTIVE', "
                + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String sqlDepositRequests = "CREATE TABLE IF NOT EXISTS wallet_deposit_requests ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "user_id INT NOT NULL, "
                + "amount DOUBLE NOT NULL, "
                + "status VARCHAR(50) DEFAULT 'PENDING', "
                + "created_at VARCHAR(50), "
                + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        // Thực thi tạo bảng đồng loạt bằng một Connection duy nhất nhằm tiết kiệm chi phí tạo mạng
        try (Connection connection = getConnection();
             Statement stmt = connection.createStatement()) {

            stmt.execute(sqlUsers);
            stmt.execute(sqlItems);
            stmt.execute(sqlAuctions);
            stmt.execute(sqlBids);
            stmt.execute(sqlAuto);
            stmt.execute(sqlWalletTx);
            stmt.execute(sqlHolds);
            stmt.execute(sqlDepositRequests);

            ensureUsersEmailColumn(connection);
            System.out.println("🚀 [HikariCP] Kết nối MySQL Cloud thành công & Trọn bộ cấu trúc bảng đã sẵn sàng!");
        } catch (SQLException e) {
            System.err.println("❌ Lỗi cấu trúc khởi tạo dữ liệu: " + e.getMessage());
        }
    }

    private void ensureUsersEmailColumn(Connection connection) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, "users", "email")) {
            if (!rs.next()) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("ALTER TABLE users ADD COLUMN email VARCHAR(255) NOT NULL DEFAULT '';");
                }
            }
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS empty_count FROM users WHERE email = ''")) {
            if (rs.next()) {
                try {
                    stmt.execute("CREATE UNIQUE INDEX idx_users_email ON users(email);");
                } catch (SQLException e) {
                    // Bỏ qua nếu index đã tồn tại trước đó
                }
            }
        }
    }
}