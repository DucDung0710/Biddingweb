-- SQL Script for Bidding System Tables
-- This script creates necessary tables for the bidding system if they don't exist

-- Table for storing bid records
CREATE TABLE IF NOT EXISTS `bid_records` (
  `bid_id` INT NOT NULL AUTO_INCREMENT,
  `auction_id` INT NOT NULL,
  `bidder_id` INT NOT NULL,
  `bidder_name` VARCHAR(255) NOT NULL,
  `bid_amount` DECIMAL(15, 2) NOT NULL,
  `bid_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `is_winning` BOOLEAN DEFAULT FALSE,
  PRIMARY KEY (`bid_id`),
  FOREIGN KEY (`auction_id`) REFERENCES `auctions`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`bidder_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  INDEX `idx_auction_id` (`auction_id`),
  INDEX `idx_bidder_id` (`bidder_id`),
  INDEX `idx_bid_time` (`bid_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ensure auctions table has the necessary columns
ALTER TABLE `auctions` ADD COLUMN IF NOT EXISTS `current_price` DECIMAL(15, 2) DEFAULT 0;
ALTER TABLE `auctions` ADD COLUMN IF NOT EXISTS `winner_id` INT DEFAULT NULL;
ALTER TABLE `auctions` ADD COLUMN IF NOT EXISTS `status` VARCHAR(50) DEFAULT 'OPEN';
ALTER TABLE `auctions` ADD COLUMN IF NOT EXISTS `start_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `auctions` ADD COLUMN IF NOT EXISTS `end_time` TIMESTAMP DEFAULT NULL;

-- Add indexes for better query performance
ALTER TABLE `auctions` ADD INDEX IF NOT EXISTS `idx_status` (`status`);
ALTER TABLE `auctions` ADD INDEX IF NOT EXISTS `idx_winner_id` (`winner_id`);

-- Ensure users table has necessary columns
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `balance` DECIMAL(15, 2) DEFAULT 0;
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `role` VARCHAR(50) DEFAULT 'Bidder';

-- View for high-level auction statistics
CREATE OR REPLACE VIEW auction_statistics AS
SELECT
  a.id AS auction_id,
  a.item_id,
  a.status,
  COUNT(DISTINCT br.bidder_id) AS unique_bidders,
  COUNT(br.bid_id) AS total_bids,
  MAX(br.bid_amount) AS highest_bid,
  MIN(br.bid_amount) AS lowest_bid,
  AVG(CAST(br.bid_amount AS DECIMAL(15,2))) AS average_bid,
  a.winner_id,
  a.current_price
FROM auctions a
LEFT JOIN bid_records br ON a.id = br.auction_id
GROUP BY a.id;

-- View for bidder statistics
CREATE OR REPLACE VIEW bidder_statistics AS
SELECT
  br.bidder_id,
  br.bidder_name,
  COUNT(br.bid_id) AS total_bids,
  COUNT(DISTINCT br.auction_id) AS auctions_participated,
  SUM(CASE WHEN br.is_winning THEN 1 ELSE 0 END) AS winning_bids,
  MAX(br.bid_amount) AS highest_bid_placed,
  AVG(CAST(br.bid_amount AS DECIMAL(15,2))) AS average_bid_amount
FROM bid_records br
GROUP BY br.bidder_id;

COMMIT;

