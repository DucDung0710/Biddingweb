package com.bidding.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:bidding_system.db";

    private DatabaseConnection() throws SQLException {
        connection = DriverManager.getConnection(DB_URL);
        // DÒNG IN ĐƯỜNG DẪN THỰC TẾ:
        java.io.File dbFile = new java.io.File("bidding_system.db");
        System.out.println("📌 Đường dẫn SQLite thực tế ứng dụng đang ghi vào: "
                + dbFile.getAbsolutePath());
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
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

    private void createTablesIfNotExist() {
        // Gộp toàn bộ mã Schema SQL của bạn vào đây để chạy tự động
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password TEXT NOT NULL, role TEXT NOT NULL, balance REAL DEFAULT 0.0);";
        String sqlItems = "CREATE TABLE IF NOT EXISTS items (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, description TEXT, type TEXT NOT NULL, seller_id INTEGER REFERENCES users(id));";
        String sqlAuctions = "CREATE TABLE IF NOT EXISTS auctions (id INTEGER PRIMARY KEY AUTOINCREMENT, item_id INTEGER REFERENCES items(id), start_price REAL NOT NULL, current_price REAL NOT NULL, start_time TEXT NOT NULL, end_time TEXT NOT NULL, status TEXT NOT NULL, winner_id INTEGER REFERENCES users(id));";
        String sqlBids = "CREATE TABLE IF NOT EXISTS bid_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, auction_id INTEGER REFERENCES auctions(id), bidder_id INTEGER REFERENCES users(id), amount REAL NOT NULL, bid_time TEXT NOT NULL, is_auto INTEGER DEFAULT 0);";
        String sqlAuto = "CREATE TABLE IF NOT EXISTS auto_bids (id INTEGER PRIMARY KEY AUTOINCREMENT, auction_id INTEGER REFERENCES auctions(id), bidder_id INTEGER REFERENCES users(id), max_bid REAL NOT NULL, increment REAL NOT NULL, registered_at TEXT NOT NULL);";
        String sqlWalletTx = "CREATE TABLE IF NOT EXISTS wallet_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id), type TEXT NOT NULL, amount REAL NOT NULL, balance_after REAL NOT NULL, ref_id INTEGER, note TEXT, created_at TEXT NOT NULL);";
        String sqlHolds = "CREATE TABLE IF NOT EXISTS wallet_holds (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id), auction_id INTEGER NOT NULL REFERENCES auctions(id), amount REAL NOT NULL, status TEXT NOT NULL);";

        try (Connection conn = this.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlUsers);
            stmt.execute(sqlItems);
            stmt.execute(sqlAuctions);
            stmt.execute(sqlBids);
            stmt.execute(sqlAuto);
            stmt.execute(sqlWalletTx);
            stmt.execute(sqlHolds);
            System.out.println(" Khởi tạo trọn bộ hệ thống bảng dữ liệu đấu giá trực tuyến thành công!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}