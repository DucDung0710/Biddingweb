package com.bidding.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseInitializer - Khởi tạo các bảng cần thiết cho hệ thống đấu giá
 * Tạo các bảng bid_records nếu chưa tồn tại
 */
public class DatabaseInitializer {

    /**
     * Khởi tạo database schema cho hệ thống đấu giá
     * Tạo bảng bid_records và các index cần thiết
     */
    public static void initializeDatabase() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            // Tạo bảng bid_records
            String createBidRecordsTable = "CREATE TABLE IF NOT EXISTS `bid_records` (" +
                    "  `bid_id` INT NOT NULL AUTO_INCREMENT," +
                    "  `auction_id` INT NOT NULL," +
                    "  `bidder_id` INT NOT NULL," +
                    "  `bidder_name` VARCHAR(255) NOT NULL," +
                    "  `bid_amount` DECIMAL(15, 2) NOT NULL," +
                    "  `bid_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  `is_winning` BOOLEAN DEFAULT FALSE," +
                    "  PRIMARY KEY (`bid_id`)," +
                    "  INDEX `idx_auction_id` (`auction_id`)," +
                    "  INDEX `idx_bidder_id` (`bidder_id`)," +
                    "  INDEX `idx_bid_time` (`bid_time`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

            stmt.executeUpdate(createBidRecordsTable);
            System.out.println("✅ Bảng bid_records đã được tạo hoặc đã tồn tại.");

            // Đảm bảo bảng auctions có các cột cần thiết
            String alterAuctions = "ALTER TABLE `auctions` ADD COLUMN IF NOT EXISTS `current_price` DECIMAL(15, 2) DEFAULT 0";
            stmt.executeUpdate(alterAuctions);
            
            alterAuctions = "ALTER TABLE `auctions` ADD COLUMN IF NOT EXISTS `winner_id` INT DEFAULT NULL";
            stmt.executeUpdate(alterAuctions);
            
            alterAuctions = "ALTER TABLE `auctions` ADD COLUMN IF NOT EXISTS `status` VARCHAR(50) DEFAULT 'OPEN'";
            stmt.executeUpdate(alterAuctions);
            
            alterAuctions = "ALTER TABLE `auctions` ADD COLUMN IF NOT EXISTS `start_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP";
            stmt.executeUpdate(alterAuctions);
            
            alterAuctions = "ALTER TABLE `auctions` ADD COLUMN IF NOT EXISTS `end_time` TIMESTAMP DEFAULT NULL";
            stmt.executeUpdate(alterAuctions);

            System.out.println("✅ Schema đấu giá đã được khởi tạo thành công.");

        } catch (SQLException e) {
            System.err.println("❌ Lỗi khởi tạo database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

