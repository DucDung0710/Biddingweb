package com.bidding.engine;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.bidding.shared.Balance;
import com.bidding.shared.Users;
import com.bidding.shared.WalletManager;

public class AuctionOperator {
    private final WalletManager walletManager;
    private final String adminUserId;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
    private final Map<String, AuctionSession> sessions = new ConcurrentHashMap<>();
    private final List<AuctionHistory> auctionHistory = new CopyOnWriteArrayList<>();
    private final Path historyFile = Paths.get("auction-history.log");
    private static final BigDecimal MINIMUM_INCREMENT_FACTOR = new BigDecimal("1.05");

    public AuctionOperator(WalletManager walletManager, String adminUserId) {
        this.walletManager = walletManager;
        this.adminUserId = adminUserId;
    }

    public AuctionResult scheduleAuction(AuctionRoom auctionRoom, long startTimeMillis) {
        if (auctionRoom == null) {
            return new AuctionResult(false, "AuctionRoom không hợp lệ.");
        }
        if (!auctionRoom.isApproved()) {
            return new AuctionResult(false, "Phòng đấu giá phải được Admin duyệt trước khi lên lịch.");
        }
        if (auctionRoom.getRoomId() == null || auctionRoom.getRoomId().isEmpty()) {
            return new AuctionResult(false, "Room ID chưa được thiết lập.");
        }

        AuctionSession existing = sessions.get(auctionRoom.getRoomId());
        if (existing != null && !existing.isEnded()) {
            return new AuctionResult(false, "Đã có một phiên đấu giá đang hoạt động với roomId này.");
        }

        auctionRoom.setScheduledStartTimeMillis(startTimeMillis);

        AuctionSession session = new AuctionSession(auctionRoom);
        sessions.put(auctionRoom.getRoomId(), session);

        long delay = Math.max(0, startTimeMillis - System.currentTimeMillis());
        scheduler.schedule(session::begin, delay, TimeUnit.MILLISECONDS);
        return new AuctionResult(true, "Đã lên lịch đấu giá cho phòng " + auctionRoom.getRoomId() + " vào thời điểm " + startTimeMillis + ".");
    }

    public AuctionResult placeBid(String roomId, Users bidder, double bidAmount) {
        if (roomId == null || roomId.isEmpty()) {
            return new AuctionResult(false, "Room ID không hợp lệ.");
        }
        AuctionSession session = sessions.get(roomId);
        if (session == null) {
            return new AuctionResult(false, "Không tìm thấy phiên đấu giá cho roomId này.");
        }
        return session.placeBid(bidAmount, bidder);
    }

    public AuctionSession getSession(String roomId) {
        return sessions.get(roomId);
    }

    public List<AuctionHistory> getAuctionHistory() {
        return Collections.unmodifiableList(auctionHistory);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void archiveSession(AuctionSession session, AuctionHistory historyEntry) {
        sessions.remove(session.room.getRoomId(), session);
        auctionHistory.add(historyEntry);
        persistHistory(historyEntry);
    }

    private void persistHistory(AuctionHistory historyEntry) {
        try {
            Files.write(historyFile, (historyEntry.toLogLine() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.out.println("Không thể ghi lịch sử đấu giá: " + ex.getMessage());
        }
    }

    public static class AuctionResult {
        private final boolean accepted;
        private final String message;

        public AuctionResult(boolean accepted, String message) {
            this.accepted = accepted;
            this.message = message;
        }

        public boolean isAccepted() {
            return accepted;
        }

        public String getMessage() {
            return message;
        }
    }

    public class AuctionSession {
        private static final long TURN_DURATION_MS = 3 * 60 * 1000L;

        private final AuctionRoom room;
        private final Lock bidLock = new ReentrantLock();
        private volatile AuctionStatus status;
        private volatile BigDecimal currentPrice;
        private volatile String highestBidderId;
        private volatile Users highestBidder;
        private volatile ScheduledFuture<?> turnTimeoutFuture;

        public AuctionSession(AuctionRoom room) {
            this.room = room;
            this.status = AuctionStatus.SCHEDULED;
            this.currentPrice = room.getItem().getFirstprice();
            this.highestBidderId = null;
            this.highestBidder = null;
        }

        public void begin() {
            if (status == AuctionStatus.CANCELLED || status == AuctionStatus.ENDED) {
                return;
            }
            if (!room.isApproved()) {
                room.notifyObservers("Phòng đấu giá chưa được duyệt, phiên không thể bắt đầu.");
                archiveSession(this, AuctionHistory.create(room.getRoomId(), room.getItem().getItemId(), null, currentPrice, BigDecimal.ZERO, BigDecimal.ZERO, AuctionStatus.CANCELLED, "Phòng chưa được duyệt."));
                return;
            }
            status = AuctionStatus.RUNNING;
            room.notifyObservers(String.format(
                "Đấu giá cho item '%s' (ID: %s) của seller '%s' sẽ bắt đầu ngay bây giờ. Giá khởi điểm: %s.",
                room.getItem().getItemName(),
                room.getItem().getItemId(),
                room.getSellerUsername().isEmpty() ? room.getSellerUserId() : room.getSellerUsername(),
                currentPrice
            ));
            scheduleTurnTimeout();
        }

        private void scheduleTurnTimeout() {
            if (turnTimeoutFuture != null && !turnTimeoutFuture.isDone()) {
                turnTimeoutFuture.cancel(false);
            }
            turnTimeoutFuture = scheduler.schedule(this::finishIfNoMoreBids, TURN_DURATION_MS, TimeUnit.MILLISECONDS);
        }

        private void finishIfNoMoreBids() {
            bidLock.lock();
            try {
                if (status != AuctionStatus.RUNNING) {
                    return;
                }
                if (highestBidderId == null) {
                    status = AuctionStatus.ENDED;
                    room.notifyObservers("Phiên đấu giá đã kết thúc mà không có người đặt giá. Không có người chiến thắng.");
                    archiveSession(this, AuctionHistory.create(room.getRoomId(), room.getItem().getItemId(), null, currentPrice, BigDecimal.ZERO, BigDecimal.ZERO, AuctionStatus.ENDED, "Không có người đặt giá."));
                    return;
                }
                completeAuction();
            } finally {
                bidLock.unlock();
            }
        }

        private void completeAuction() {
            status = AuctionStatus.ENDED;
            Balance winnerWallet = walletManager.getWalletByUserId(highestBidderId);
            Balance sellerWallet = walletManager.getWalletByUserId(room.getSellerUserId());
            Balance adminWallet = walletManager.getWalletByUserId(adminUserId);

            if (winnerWallet == null || sellerWallet == null || adminWallet == null) {
                room.notifyObservers("Không thể hoàn tất đấu giá do ví không hợp lệ. Cần xử lý thủ công.");
                archiveSession(this, AuctionHistory.create(room.getRoomId(), room.getItem().getItemId(), highestBidderId, currentPrice, BigDecimal.ZERO, BigDecimal.ZERO, AuctionStatus.CANCELLED, "Ví người tham gia hoặc seller/admin không tồn tại."));
                return;
            }

            BigDecimal paid;
            try {
                paid = winnerWallet.commitLockedAmount(currentPrice);
            } catch (IllegalArgumentException ex) {
                room.notifyObservers("Không thể trừ tiền người thắng: " + ex.getMessage());
                archiveSession(this, AuctionHistory.create(room.getRoomId(), room.getItem().getItemId(), highestBidderId, currentPrice, BigDecimal.ZERO, BigDecimal.ZERO, AuctionStatus.CANCELLED, "Commit tiền lỗi: " + ex.getMessage()));
                return;
            }

            BigDecimal sellerShare = paid.multiply(new BigDecimal("0.90"));
            BigDecimal adminShare = paid.multiply(new BigDecimal("0.10"));

            try {
                sellerWallet.deposit(sellerShare);
                adminWallet.deposit(adminShare);
            } catch (IllegalArgumentException ex) {
                room.notifyObservers("Không thể phân phối tiền thắng: " + ex.getMessage() + ". Hoàn trả người thắng.");
                winnerWallet.deposit(paid);
                archiveSession(this, AuctionHistory.create(room.getRoomId(), room.getItem().getItemId(), highestBidderId, currentPrice, BigDecimal.ZERO, BigDecimal.ZERO, AuctionStatus.CANCELLED, "Rollback do phân phối lỗi: " + ex.getMessage()));
                return;
            }

            String winnerName = highestBidder.getUsername();
            room.notifyObservers(String.format(
                "Đấu giá kết thúc. Người thắng: %s (ID: %s) với giá %s. Seller '%s' nhận %s, Admin nhận %s.",
                winnerName,
                highestBidderId,
                paid,
                room.getSellerUsername().isEmpty() ? room.getSellerUserId() : room.getSellerUsername(),
                sellerShare,
                adminShare
            ));
            highestBidder.update(String.format("Chúc mừng! Bạn đã thắng phiên đấu giá '%s' với giá %s.", room.getRoomId(), paid));
            archiveSession(this, AuctionHistory.create(room.getRoomId(), room.getItem().getItemId(), highestBidderId, paid, sellerShare, adminShare, AuctionStatus.ENDED, "Đấu giá hoàn tất thành công."));
        }

        public AuctionResult placeBid(double bidAmount, Users bidder) {
            if (bidder == null) {
                return new AuctionResult(false, "Người đặt giá không hợp lệ.");
            }
            if (!"Bidder".equalsIgnoreCase(bidder.getRole())) {
                return new AuctionResult(false, "Chỉ Bidder mới được phép đặt giá.");
            }
            BigDecimal bid = BigDecimal.valueOf(bidAmount);
            if (bid.compareTo(BigDecimal.ZERO) <= 0) {
                return new AuctionResult(false, "Số tiền đặt giá phải lớn hơn 0.");
            }
            if (status != AuctionStatus.RUNNING) {
                return new AuctionResult(false, "Phiên đấu giá chưa bắt đầu hoặc đã kết thúc.");
            }

            bidLock.lock();
            try {
                if (status != AuctionStatus.RUNNING) {
                    return new AuctionResult(false, "Phiên đấu giá đã kết thúc.");
                }

                BigDecimal requiredMinimum = (highestBidderId == null)
                        ? room.getItem().getFirstprice().multiply(MINIMUM_INCREMENT_FACTOR)
                        : currentPrice.multiply(MINIMUM_INCREMENT_FACTOR);

                if (bid.compareTo(requiredMinimum) < 0) {
                    return new AuctionResult(false, String.format("Giá đặt phải ít nhất %s (tăng 5%% so với giá hiện tại).", requiredMinimum));
                }

                Balance bidderWallet = walletManager.getWalletByUserId(bidder.getId());
                if (bidderWallet == null) {
                    return new AuctionResult(false, "Không tìm thấy ví của bidder.");
                }

                if (highestBidderId != null && bidder.getId().equals(highestBidderId)) {
                    BigDecimal additionalAmount = bid.subtract(currentPrice);
                    if (additionalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        return new AuctionResult(false, "Giá đặt mới phải cao hơn giá hiện tại.");
                    }
                    if (bidderWallet.getAmount().compareTo(additionalAmount) < 0) {
                        return new AuctionResult(false, "Số dư không đủ để tăng giá.");
                    }
                    try {
                        bidderWallet.lockAmount(additionalAmount);
                    } catch (IllegalArgumentException ex) {
                        return new AuctionResult(false, ex.getMessage());
                    }
                    currentPrice = bid;
                    scheduleTurnTimeout();
                    room.notifyObservers(String.format("Bidder '%s' đã tiếp tục giữ vị trí dẫn đầu với giá %s.", bidder.getUsername(), currentPrice));
                    return new AuctionResult(true, "Bạn đã gia hạn vị trí dẫn đầu và khóa thêm tiền.");
                }

                if (highestBidderId != null) {
                    Balance previousWallet = walletManager.getWalletByUserId(highestBidderId);
                    if (previousWallet != null) {
                        try {
                            previousWallet.unlockAmount(currentPrice);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }

                try {
                    bidderWallet.lockAmount(bid);
                } catch (IllegalArgumentException ex) {
                    return new AuctionResult(false, ex.getMessage());
                }
                highestBidderId = bidder.getId();
                highestBidder = bidder;
                currentPrice = bid;
                scheduleTurnTimeout();

                room.notifyObservers(String.format("Bidder '%s' đã dẫn đầu phiên với giá %s. Thời gian turn reset lại 3 phút.", bidder.getUsername(), currentPrice));
                return new AuctionResult(true, "Đặt giá thành công. Bạn đang dẫn đầu phiên đấu giá.");
            } finally {
                bidLock.unlock();
            }
        }

        public boolean isStarted() {
            return status == AuctionStatus.RUNNING;
        }

        public boolean isEnded() {
            return status == AuctionStatus.ENDED || status == AuctionStatus.CANCELLED;
        }

        public BigDecimal getCurrentPrice() {
            return currentPrice;
        }

        public String getHighestBidderId() {
            return highestBidderId;
        }
    }

    public static class AuctionHistory {
        private final String roomId;
        private final String itemId;
        private final String winnerId;
        private final BigDecimal finalPrice;
        private final BigDecimal sellerShare;
        private final BigDecimal adminShare;
        private final AuctionStatus status;
        private final long executedAtMillis;
        private final String note;

        private AuctionHistory(String roomId, String itemId, String winnerId, BigDecimal finalPrice, BigDecimal sellerShare, BigDecimal adminShare, AuctionStatus status, String note) {
            this.roomId = roomId;
            this.itemId = itemId;
            this.winnerId = winnerId;
            this.finalPrice = finalPrice;
            this.sellerShare = sellerShare;
            this.adminShare = adminShare;
            this.status = status;
            this.executedAtMillis = System.currentTimeMillis();
            this.note = note;
        }

        public static AuctionHistory create(String roomId, String itemId, String winnerId, BigDecimal finalPrice, BigDecimal sellerShare, BigDecimal adminShare, AuctionStatus status, String note) {
            return new AuctionHistory(roomId, itemId, winnerId, finalPrice, sellerShare, adminShare, status, note);
        }

        public String toLogLine() {
            return String.format("%d | roomId=%s | itemId=%s | winnerId=%s | finalPrice=%s | sellerShare=%s | adminShare=%s | status=%s | note=%s",
                    executedAtMillis,
                    roomId,
                    itemId,
                    winnerId == null ? "NONE" : winnerId,
                    finalPrice,
                    sellerShare,
                    adminShare,
                    status,
                    note);
        }

        public String getRoomId() {
            return roomId;
        }

        public String getItemId() {
            return itemId;
        }

        public String getWinnerId() {
            return winnerId;
        }

        public BigDecimal getFinalPrice() {
            return finalPrice;
        }

        public BigDecimal getSellerShare() {
            return sellerShare;
        }

        public BigDecimal getAdminShare() {
            return adminShare;
        }

        public AuctionStatus getStatus() {
            return status;
        }

        public long getExecutedAtMillis() {
            return executedAtMillis;
        }

        public String getNote() {
            return note;
        }
    }

    public enum AuctionStatus {
        SCHEDULED,
        RUNNING,
        ENDED,
        CANCELLED
    }
}
