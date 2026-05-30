-- Create table for storing auto-bid configurations
CREATE TABLE IF NOT EXISTS auto_bids (
    id INT AUTO_INCREMENT PRIMARY KEY,
    auction_id INT NOT NULL,
    user_id INT NOT NULL,
    max_bid DECIMAL(20,2) NOT NULL,
    increment DECIMAL(20,2) NOT NULL,
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_auto_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    CONSTRAINT fk_auto_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Helpful indexes
CREATE INDEX idx_auto_auction ON auto_bids(auction_id);
CREATE INDEX idx_auto_user ON auto_bids(user_id);

-- Recommended updates for auctions/items schema (run separately if needed):
-- ALTER TABLE auctions ADD COLUMN status VARCHAR(32) DEFAULT 'OPEN';
-- ALTER TABLE auctions ADD COLUMN start_time BIGINT;
-- ALTER TABLE auctions ADD COLUMN end_time BIGINT;
--
-- ALTER TABLE items ADD COLUMN admin_status VARCHAR(32) DEFAULT 'PENDING';
-- ALTER TABLE items ADD COLUMN sale_status VARCHAR(32) DEFAULT 'NOT_LISTED';
-- ALTER TABLE items ADD COLUMN type VARCHAR(64) DEFAULT 'unknown';

-- Note: run these statements manually against your MySQL DB used by the application.
