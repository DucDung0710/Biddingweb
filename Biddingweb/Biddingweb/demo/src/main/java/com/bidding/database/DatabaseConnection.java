package com.bidding.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private final Connection connection;

    private static final String DB_HOST = getEnv("DB_HOST", "localhost");
    private static final String DB_PORT = getEnv("DB_PORT", "3306");
    private static final String DB_NAME = getEnv("DB_NAME", "biddingdb");
    private static final String DB_USER = getEnv("DB_USER", "root");
    private static final String DB_PASSWORD = getEnv("DB_PASSWORD", "123456");
    private static final String SERVER_URL = String.format("jdbc:mysql://%s:%s/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", DB_HOST, DB_PORT);
    private static final String DB_URL = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", DB_HOST, DB_PORT, DB_NAME);

    private DatabaseConnection() throws SQLException {
        createDatabaseIfNeeded();
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        System.out.println("📌 MySQL database URL: " + DB_URL);
        createTablesIfNotExist();
    }

    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private void createDatabaseIfNeeded() throws SQLException {
        try (Connection initConnection = DriverManager.getConnection(SERVER_URL, DB_USER, DB_PASSWORD);
             Statement stmt = initConnection.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    public static synchronized DatabaseConnection getInstance() throws SQLException {
        if (instance == null || instance.connection.isClosed()) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    @SuppressWarnings("unused")
    private void createTablesIfNotExist() {
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(100) UNIQUE NOT NULL, " +
                "email VARCHAR(255) UNIQUE NOT NULL, " +
                "password VARCHAR(255) NOT NULL, " +
                "role VARCHAR(50) NOT NULL, " +
                "balance DOUBLE DEFAULT 0.0" +
                ") ENGINE=InnoDB;";
        String sqlItems = "CREATE TABLE IF NOT EXISTS items (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL, " +
                "description TEXT, " +
                "type VARCHAR(100) NOT NULL, " +
                "seller_id INT, " +
                "FOREIGN KEY (seller_id) REFERENCES users(id)" +
                ") ENGINE=InnoDB;";
        String sqlAuctions = "CREATE TABLE IF NOT EXISTS auctions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "item_id INT, " +
                "start_price DOUBLE NOT NULL, " +
                "current_price DOUBLE NOT NULL, " +
                "start_time VARCHAR(50) NOT NULL, " +
                "end_time VARCHAR(50) NOT NULL, " +
                "status VARCHAR(50) NOT NULL, " +
                "winner_id INT, " +
                "FOREIGN KEY (item_id) REFERENCES items(id), " +
                "FOREIGN KEY (winner_id) REFERENCES users(id)" +
                ") ENGINE=InnoDB;";
        String sqlBids = "CREATE TABLE IF NOT EXISTS bid_transactions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "auction_id INT, " +
                "bidder_id INT, " +
                "amount DOUBLE NOT NULL, " +
                "bid_time VARCHAR(50) NOT NULL, " +
                "is_auto TINYINT DEFAULT 0, " +
                "FOREIGN KEY (auction_id) REFERENCES auctions(id), " +
                "FOREIGN KEY (bidder_id) REFERENCES users(id)" +
                ") ENGINE=InnoDB;";
        String sqlAuto = "CREATE TABLE IF NOT EXISTS auto_bids (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "auction_id INT, " +
                "bidder_id INT, " +
                "max_bid DOUBLE NOT NULL, " +
                "increment DOUBLE NOT NULL, " +
                "registered_at VARCHAR(50) NOT NULL, " +
                "FOREIGN KEY (auction_id) REFERENCES auctions(id), " +
                "FOREIGN KEY (bidder_id) REFERENCES users(id)" +
                ") ENGINE=InnoDB;";
        String sqlWalletTx = "CREATE TABLE IF NOT EXISTS wallet_transactions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "type VARCHAR(50) NOT NULL, " +
                "amount DOUBLE NOT NULL, " +
                "balance_after DOUBLE NOT NULL, " +
                "ref_id INT, " +
                "note TEXT, " +
                "created_at VARCHAR(50) NOT NULL, " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ") ENGINE=InnoDB;";
        String sqlHolds = "CREATE TABLE IF NOT EXISTS wallet_holds (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "auction_id INT NOT NULL, " +
                "amount DOUBLE NOT NULL, " +
                "status VARCHAR(50) NOT NULL, " +
                "FOREIGN KEY (user_id) REFERENCES users(id), " +
                "FOREIGN KEY (auction_id) REFERENCES auctions(id)" +
                ") ENGINE=InnoDB;";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlUsers);
            stmt.execute(sqlItems);
            stmt.execute(sqlAuctions);
            stmt.execute(sqlBids);
            stmt.execute(sqlAuto);
            stmt.execute(sqlWalletTx);
            stmt.execute(sqlHolds);
            ensureUsersEmailColumn();
            System.out.println("Khởi tạo trọn bộ hệ thống bảng dữ liệu đấu giá trực tuyến thành công!");
        } catch (SQLException e) {
            // Log exception instead of printing stack trace
        }
    }

    private void ensureUsersEmailColumn() throws SQLException {
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, "users", "email")) {
            if (!rs.next()) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("ALTER TABLE users ADD COLUMN email VARCHAR(255) NOT NULL DEFAULT '';");
                }
            }
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS empty_count FROM users WHERE email = ''")) {
            if (rs.next() && rs.getInt("empty_count") == 0) {
                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email);");
            }
        }
    }
}