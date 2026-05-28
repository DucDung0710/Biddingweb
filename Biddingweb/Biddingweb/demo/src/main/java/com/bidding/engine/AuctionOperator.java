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

/**
 * AuctionOperator - Quản lý và điều hành các phiên đấu giá
 * 
 * Chức năng chính:
 * - Lên lịch các phiên đấu giá (scheduleAuction)
 * - Quản lý việc đặt giá của bidders (placeBid)
 * - Hoàn tất đấu giá và phân phối tiền (completeAuction)
 * - Lưu trữ lịch sử đấu giá (persistHistory)
 * 
 * Đặc điểm bảo mật:
 * - Sử dụng ConcurrentHashMap và CopyOnWriteArrayList để đồng bộ thread-safe
 * - Mỗi phiên đấu giá có Lock riêng để bảo vệ thao tác đặt giá
 * - Tự động timeout nếu không có người đặt giá trong 3 phút
 * 
 * Cơ chế tài chính:
 * - Khóa tiền (lock) của bidder khi họ dẫn đầu
 * - Mở khóa tiền (unlock) khi bị người khác vượt giá
 * - Commit tiền khi phiên kết thúc (người thắng)
 * - Chia tỷ lệ: Seller 90%, Admin 10%
 */
public class AuctionOperator {
    // Quản lý ví tiền của các người dùng
    private final WalletManager walletManager;
    
    // ID của admin (cho việc phân phối tiền)
    private final String adminUserId;
    
    // Thread pool để lên lịch các task (timeout phiên, etc)
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
    
    // Bộ lưu trữ các phiên đấu giá đang hoạt động (roomId -> AuctionSession)
    private final Map<String, AuctionSession> sessions = new ConcurrentHashMap<>();
    
    // Lịch sử tất cả các phiên đấu giá (kể cả đã kết thúc)
    private final List<AuctionHistory> auctionHistory = new CopyOnWriteArrayList<>();
    
    // File lưu trữ log lịch sử đấu giá
    private final Path historyFile = Paths.get("auction-history.log");
    
    // Mức tăng giá tối thiểu: 5% so với giá hiện tại
    private static final BigDecimal MINIMUM_INCREMENT_FACTOR = new BigDecimal("1.05");

    public AuctionOperator(WalletManager walletManager, String adminUserId) {
        this.walletManager = walletManager;
        this.adminUserId = adminUserId;
    }

    /**
     * Lên lịch một phiên đấu giá để bắt đầu vào thời điểm chỉ định.
     * 
     * Quá trình:
     * 1. Kiểm tra xác nhận của phòng đấu giá (phải được Admin duyệt)
     * 2. Kiểm tra không có phiên đấu giá nào đang hoạt động với roomId này
     * 3. Tạo AuctionSession và lên lịch để bắt đầu vào thời điểm startTimeMillis
     * 4. Trả về AuctionResult với kết quả (thành công/thất bại)
     * 
     * @param auctionRoom Phòng đấu giá cần lên lịch
     * @param startTimeMillis Thời điểm bắt đầu (milliseconds từ epoch)
     * @return AuctionResult với trạng thái và thông báo chi tiết
     */
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

    /**
     * Đặt giá cho một phiên đấu giá đang hoạt động.
     * 
     * Quá trình:
     * 1. Tìm AuctionSession tương ứng với roomId
     * 2. Gọi phương thức placeBid của session
     * 3. Trả về kết quả: thành công/thất bại với thông báo chi tiết
     * 
     * @param roomId ID của phòng đấu giá
     * @param bidder Người dùng đặt giá (phải có role "Bidder")
     * @param bidAmount Số tiền đặt giá
     * @return AuctionResult với trạng thái kết quả
     */
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

    /**
     * Lấy thông tin phiên đấu giá theo roomId.
     * 
     * @param roomId ID của phòng đấu giá
     * @return AuctionSession nếu tìm thấy, null nếu không tồn tại
     */
    public AuctionSession getSession(String roomId) {
        return sessions.get(roomId);
    }

    /**
     * Lấy toàn bộ lịch sử đấu giá (bao gồm các phiên đã kết thúc và bị hủy).
     * 
     * @return Danh sách không thể sửa đổi của AuctionHistory
     */
    public List<AuctionHistory> getAuctionHistory() {
        return Collections.unmodifiableList(auctionHistory);
    }

    /**
     * Tắt AuctionOperator: dừng tất cả các tác vụ được lên lịch.
     * 
     * Lưu ý: Gọi method này khi kết thúc ứng dụng để tránh rò rỉ tài nguyên.
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }

    /**
     * Xóa phiên đấu giá khỏi bộ lưu trữ hoạt động và thêm vào lịch sử.
     * 
     * Đây là bước cuối cùng trong vòng đời của một phiên đấu giá.
     * 
     * @param session Phiên đấu giá cần xóa
     * @param historyEntry Bản ghi lịch sử để lưu trữ
     */
    private void archiveSession(AuctionSession session, AuctionHistory historyEntry) {
        sessions.remove(session.room.getRoomId(), session);
        auctionHistory.add(historyEntry);
        persistHistory(historyEntry);
    }

    /**
     * Ghi bản ghi lịch sử đấu giá vào file (append mode).
     * 
     * Định dạng log: [timestamp | roomId | itemId | winnerId | finalPrice | sellerShare | adminShare | status | note]
     * 
     * @param historyEntry Bản ghi lịch sử cần ghi
     */
    private void persistHistory(AuctionHistory historyEntry) {
        try {
            Files.write(historyFile, (historyEntry.toLogLine() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.out.println("Không thể ghi lịch sử đấu giá: " + ex.getMessage());
        }
    }

    /**
     * AuctionResult - Lớp wrapping kết quả thao tác của phiên đấu giá.
     * 
     * Được sử dụng để trả về trạng thái (thành công/thất bại) và thông báo chi tiết
     * cho các phương thức scheduleAuction() và placeBid().
     */
    public static class AuctionResult {
        // Trạng thái thao tác: true = thành công, false = thất bại
        private final boolean accepted;
        
        // Thông báo chi tiết mô tả kết quả hoặc lý do thất bại
        private final String message;

        public AuctionResult(boolean accepted, String message) {
            this.accepted = accepted;
            this.message = message;
        }

        /**
         * Kiểm tra xem thao tác có thành công hay không.
         * @return true nếu thành công, false nếu thất bại
         */
        public boolean isAccepted() {
            return accepted;
        }

        /**
         * Lấy thông báo chi tiết về kết quả.
         * @return Chuỗi thông báo
         */
        public String getMessage() {
            return message;
        }
    }

    /**
     * AuctionSession - Đại diện cho một phiên đấu giá đang hoạt động.
     * 
     * Chức năng:
     * - Quản lý trạng thái phiên (SCHEDULED -> RUNNING -> ENDED/CANCELLED)
     * - Xử lý đặt giá từ bidders (placeBid)
     * - Tự động kết thúc phiên nếu không có bidders trong 3 phút (finishIfNoMoreBids)
     * - Phân phối tiền cho seller và admin (completeAuction)
     * 
     * Thread safety:
     * - Sử dụng bidLock (ReentrantLock) để bảo vệ thao tác đặt giá
     * - Sử dụng volatile fields để đảm bảo visibility trong multi-threaded environment
     */
    public class AuctionSession {
        // Thời gian tối đa cho mỗi turn của phiên: 3 phút = 180 giây
        private static final long TURN_DURATION_MS = 3 * 60 * 1000L;

        // Phòng đấu giá này belong to
        private final AuctionRoom room;
        
        // Lock để bảo vệ các thao tác đặt giá (bidding operations)
        private final Lock bidLock = new ReentrantLock();
        
        // Trạng thái hiện tại của phiên (volatile để visibility)
        private volatile AuctionStatus status;
        
        // Giá hiện tại cao nhất (volatile để visibility)
        private volatile BigDecimal currentPrice;
        
        // ID của người đặt giá cao nhất (volatile để visibility)
        private volatile String highestBidderId;
        
        // Đối tượng Users của người đặt giá cao nhất (volatile để visibility)
        private volatile Users highestBidder;
        
        // Future của tác vụ timeout turn hiện tại (để cancel nếu có bid mới)
        private volatile ScheduledFuture<?> turnTimeoutFuture;

        public AuctionSession(AuctionRoom room) {
            this.room = room;
            this.status = AuctionStatus.SCHEDULED;
            this.currentPrice = room.getItem().getFirstprice();
            this.highestBidderId = null;
            this.highestBidder = null;
        }

        /**
         * Bắt đầu phiên đấu giá.
         * 
         * Quá trình:
         * 1. Kiểm tra phòng đã được duyệt (approved)
         * 2. Thay đổi status thành RUNNING
         * 3. Thông báo cho tất cả observers (watchers)
         * 4. Lên lịch timeout cho turn (nếu không có bidders trong 3 phút sẽ kết thúc)
         */
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

        /**
         * Lên lịch timeout cho turn hiện tại (3 phút).
         * 
         * Nếu có timeout cũ chưa hoàn thành, hủy nó trước.
         * Khi timeout hết, nếu không có bidders mới thì kết thúc phiên.
         */
        private void scheduleTurnTimeout() {
            if (turnTimeoutFuture != null && !turnTimeoutFuture.isDone()) {
                turnTimeoutFuture.cancel(false);
            }
            turnTimeoutFuture = scheduler.schedule(this::finishIfNoMoreBids, TURN_DURATION_MS, TimeUnit.MILLISECONDS);
        }

        /**
         * Hoàn tất phiên nếu hết thời gian và không có bids mới.
         * 
         * Quá trình:
         * 1. Kiểm tra lock để tránh race condition
         * 2. Nếu không có bidders, kết thúc với trạng thái ENDED
         * 3. Nếu có bidders, hoàn tất đấu giá và phân phối tiền
         */
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

        /**
         * Hoàn tất đấu giá và phân phối tiền.
         * 
         * Quá trình chi tiết:
         * 1. Thay đổi status thành ENDED
         * 2. Commit tiền của winner (trừ từ ví)
         * 3. Phân phối tiền:
         *    - Seller nhận 90% giá bán
         *    - Admin nhận 10% giá bán
         * 4. Thông báo cho tất cả observers
         * 5. Cập nhật notification cho winner
         * 6. Archive phiên vào lịch sử
         * 
         * Xử lý ngoại lệ:
         * - Nếu ví không tồn tại: hủy phiên
         * - Nếu commit tiền lỗi: hủy phiên
         * - Nếu phân phối lỗi: hoàn trả tiền cho winner và hủy phiên
         */
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

        /**
         * Xử lý yêu cầu đặt giá từ một bidder.
         * 
         * Quá trình chi tiết:
         * 1. Kiểm tra xác nhận bidder:
         *    - Bidder có role "Bidder"
         *    - Số tiền > 0
         *    - Phiên đang RUNNING
         * 
         * 2. Tính giá tối thiểu cần đạt:
         *    - Nếu là bid đầu tiên: firstPrice * 1.05 (tăng 5%)
         *    - Nếu không phải bid đầu: currentPrice * 1.05
         * 
         * 3. Xử lý hai trường hợp:
         *    a) Bidder hiện tại tăng giá (nâng lên từ vị trí dẫn đầu):
         *       - Chỉ cần lock thêm tiền chênh lệch
         *       - Giữ nguyên vị trí dẫn đầu
         *    
         *    b) Bidder khác đặt giá cao hơn:
         *       - Unlock tiền của bidder cũ
         *       - Lock tiền mới của bidder này
         *       - Cập nhật highestBidderId và currentPrice
         * 
         * 4. Reset timeout turn sau mỗi bid mới
         * 5. Thông báo cho tất cả observers
         * 
         * @param bidAmount Số tiền bidder muốn đặt
         * @param bidder Đối tượng Users của bidder
         * @return AuctionResult với kết quả chi tiết
         */
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

        /**
         * Kiểm tra xem phiên đấu giá đã bắt đầu chưa.
         * @return true nếu status == RUNNING
         */
        public boolean isStarted() {
            return status == AuctionStatus.RUNNING;
        }

        /**
         * Kiểm tra xem phiên đấu giá đã kết thúc hay bị hủy chưa.
         * @return true nếu status == ENDED hoặc CANCELLED
         */
        public boolean isEnded() {
            return status == AuctionStatus.ENDED || status == AuctionStatus.CANCELLED;
        }

        /**
         * Lấy giá hiện tại cao nhất của phiên.
         * @return BigDecimal giá hiện tại
         */
        public BigDecimal getCurrentPrice() {
            return currentPrice;
        }

        /**
         * Lấy ID của bidder dẫn đầu hiện tại.
         * @return ID của bidder, hoặc null nếu chưa có bidders
         */
        public String getHighestBidderId() {
            return highestBidderId;
        }
    }

    /**
     * AuctionHistory - Bản ghi lịch sử một phiên đấu giá đã kết thúc.
     * 
     * Được sử dụng để:
     * - Lưu trữ thông tin chi tiết về phiên đấu giá (winner, giá cuối, phân phối tiền)
     * - Persist vào file log để kiểm toán
     * - Cho phép truy vấn lịch sử hoạt động đấu giá
     * 
     * Dữ liệu lưu trữ:
     * - roomId: ID của phòng đấu giá
     * - itemId: ID của sản phẩm được đấu giá
     * - winnerId: ID của người thắng (null nếu không có bidders)
     * - finalPrice: Giá cuối cùng của sản phẩm
     * - sellerShare: Tiền seller nhận được (90% của finalPrice)
     * - adminShare: Tiền admin nhận được (10% của finalPrice)
     * - status: Trạng thái kết quả (ENDED, CANCELLED, etc)
     * - executedAtMillis: Timestamp khi phiên hoàn tất
     * - note: Ghi chú chi tiết (e.g., lý do hủy, thông báo lỗi)
     */
    public static class AuctionHistory {
        // ID của phòng đấu giá
        private final String roomId;
        
        // ID của sản phẩm được đấu giá
        private final String itemId;
        
        // ID của người thắng (null nếu không có bidders)
        private final String winnerId;
        
        // Giá cuối cùng mà người thắng phải trả
        private final BigDecimal finalPrice;
        
        // Tổng tiền seller nhận được (90% của finalPrice)
        private final BigDecimal sellerShare;
        
        // Tổng tiền admin nhận được (10% của finalPrice)
        private final BigDecimal adminShare;
        
        // Trạng thái kết quả: ENDED, CANCELLED, etc
        private final AuctionStatus status;
        
        // Timestamp khi bản ghi này được tạo (milliseconds từ epoch)
        private final long executedAtMillis;
        
        // Ghi chú chi tiết (e.g., lý do hủy, thông báo lỗi)
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

        /**
         * Factory method để tạo AuctionHistory record.
         * 
         * @param roomId ID của phòng đấu giá
         * @param itemId ID của sản phẩm
         * @param winnerId ID của người thắng (null nếu không có bidders)
         * @param finalPrice Giá cuối cùng
         * @param sellerShare Tiền seller nhận
         * @param adminShare Tiền admin nhận
         * @param status Trạng thái (ENDED, CANCELLED)
         * @param note Ghi chú chi tiết
         * @return AuctionHistory mới được tạo
         */
        public static AuctionHistory create(String roomId, String itemId, String winnerId, BigDecimal finalPrice, BigDecimal sellerShare, BigDecimal adminShare, AuctionStatus status, String note) {
            return new AuctionHistory(roomId, itemId, winnerId, finalPrice, sellerShare, adminShare, status, note);
        }

        /**
         * Chuyển đổi record thành một dòng log để ghi vào file.
         * 
         * Format: [timestamp | roomId | itemId | winnerId | finalPrice | sellerShare | adminShare | status | note]
         * 
         * @return Chuỗi log đã định dạng
         */
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

        // ===== GETTERS =====
        
        /**
         * @return ID của phòng đấu giá
         */
        public String getRoomId() {
            return roomId;
        }

        /**
         * @return ID của sản phẩm được đấu giá
         */
        public String getItemId() {
            return itemId;
        }

        /**
         * @return ID của người thắng (null nếu không có bidders)
         */
        public String getWinnerId() {
            return winnerId;
        }

        /**
         * @return Giá cuối cùng của sản phẩm
         */
        public BigDecimal getFinalPrice() {
            return finalPrice;
        }

        /**
         * @return Tiền seller nhận được
         */
        public BigDecimal getSellerShare() {
            return sellerShare;
        }

        /**
         * @return Tiền admin nhận được
         */
        public BigDecimal getAdminShare() {
            return adminShare;
        }

        /**
         * @return Trạng thái kết quả của phiên
         */
        public AuctionStatus getStatus() {
            return status;
        }

        /**
         * @return Timestamp khi phiên hoàn tất (milliseconds từ epoch)
         */
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
