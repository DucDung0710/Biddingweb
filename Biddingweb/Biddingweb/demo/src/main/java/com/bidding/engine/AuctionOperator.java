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

import com.bidding.shared.AuctionObserver;
import com.bidding.shared.Balance;
import com.bidding.shared.Item;
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
    private volatile Integer adminUserId;
    
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
    // Nếu bid xảy ra trong 10s cuối thì áp dụng cơ chế gia hạn
    private static final long EXTENSION_WINDOW_MS = 10 * 1000L;
    // Mỗi lần gia hạn sẽ thêm 3 phút
    private static final long EXTENSION_DURATION_MS = 3 * 60 * 1000L;
    // Thời lượng mặc định nếu không có endTime được cung cấp
    private static final long DEFAULT_INITIAL_DURATION_MS = 3 * 60 * 1000L;

    public AuctionOperator(WalletManager walletManager) {
        this.walletManager = walletManager;
    }

    public AuctionOperator(WalletManager walletManager, String adminUserIdStr) {
        this(walletManager);
        try {
            if (adminUserIdStr != null && !adminUserIdStr.isEmpty()) {
                this.adminUserId = Integer.valueOf(adminUserIdStr);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    public void setAdminUserId(int adminUserId) {
        this.adminUserId = adminUserId;
    }

    /**
     * Lên lịch một phiên đấu giá để bắt đầu vào thời điểm chỉ định.
     * 
     * Quá trình:
     * 1. Kiểm tra item và seller đã được xác thực
     * 2. Kiểm tra không có phiên đấu giá nào đang hoạt động với roomId này
     * 3. Tạo AuctionSession và lên lịch để bắt đầu vào thời điểm startTimeMillis
     * 4. Trả về AuctionResult với trạng thái và thông báo chi tiết
     * 
     * @param item Item đấu giá
     * @param seller Người bán
     * @param roomId ID phiên đấu giá
     * @param startTimeMillis Thời điểm bắt đầu (milliseconds từ epoch)
     * @return AuctionResult với trạng thái và thông báo chi tiết
     */
    public AuctionResult scheduleAuction(Item item, Users seller, String roomId, long startTimeMillis) {
        long defaultEnd = startTimeMillis + DEFAULT_INITIAL_DURATION_MS;
        return scheduleAuction(item, seller, roomId, startTimeMillis, defaultEnd);
    }

    public AuctionResult scheduleAuction(Item item, Users seller, String roomId, long startTimeMillis, long endTimeMillis) {
        if (item == null) {
            return new AuctionResult(false, "Item không hợp lệ.");
        }
        if (seller == null || item.getUserId() != seller.getId()) {
            return new AuctionResult(false, "Chỉ seller sở hữu sản phẩm mới có thể lên lịch đấu giá.");
        }
        if (!Item.STATUS_APPROVED.equalsIgnoreCase(item.getStatus())) {
            return new AuctionResult(false, "Sản phẩm chưa được Admin duyệt.");
        }
        if (roomId == null || roomId.isEmpty()) {
            return new AuctionResult(false, "Room ID không hợp lệ.");
        }

        AuctionSession existing = sessions.get(roomId);
        if (existing != null && !existing.isEnded()) {
            return new AuctionResult(false, "Đã có một phiên đấu giá đang hoạt động với roomId này.");
        }

        AuctionSession session = new AuctionSession(roomId, item, seller, endTimeMillis);
        sessions.put(roomId, session);

        long delay = Math.max(0, startTimeMillis - System.currentTimeMillis());
        scheduler.schedule(session::begin, delay, TimeUnit.MILLISECONDS);
        return new AuctionResult(true, "Đã lên lịch đấu giá cho phiên " + roomId + " vào thời điểm " + startTimeMillis + ". Kết thúc: " + endTimeMillis + ".");
    }

    /**
     * Overload allowing passing DB auctionId so engine can persist bid updates.
     */
    public AuctionResult scheduleAuction(Item item, Users seller, String roomId, long startTimeMillis, long endTimeMillis, int auctionId) {
        AuctionResult r = scheduleAuction(item, seller, roomId, startTimeMillis, endTimeMillis);
        if (r.isAccepted() && auctionId > 0) {
            AuctionSession s = sessions.get(roomId);
            if (s != null) s.auctionId = auctionId;
        }
        return r;
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
        if (session == null && roomId.matches("\\d+")) {
            session = getSessionByAuctionId(Integer.parseInt(roomId));
        }
        if (session == null) {
            return new AuctionResult(false, "Không tìm thấy phiên đấu giá cho roomId này.");
        }
        return session.placeBid(bidAmount, bidder);
    }

    /**
     * Đăng ký auto-bid cho một phòng đấu giá cụ thể.
     */
    public AuctionResult registerAutoBid(String roomId, Users bidder, double maxBid, double increment) {
        if (roomId == null || roomId.isEmpty()) {
            return new AuctionResult(false, "Room ID không hợp lệ.");
        }
        AuctionSession session = sessions.get(roomId);
        if (session == null && roomId.matches("\\d+")) {
            session = getSessionByAuctionId(Integer.parseInt(roomId));
        }
        if (session == null) {
            return new AuctionResult(false, "Không tìm thấy phiên đấu giá cho roomId này.");
        }
        return session.registerAutoBid(bidder, maxBid, increment);
    }

    /**
     * Lấy thông tin phiên đấu giá theo roomId hoặc auctionId.
     * 
     * @param roomId ID của phòng đấu giá hoặc auctionId dạng chuỗi
     * @return AuctionSession nếu tìm thấy, null nếu không tồn tại
     */
    public AuctionSession getSession(String roomId) {
        AuctionSession session = sessions.get(roomId);
        if (session == null && roomId != null && roomId.matches("\\d+")) {
            session = getSessionByAuctionId(Integer.parseInt(roomId));
        }
        return session;
    }

    private AuctionSession getSessionByAuctionId(int auctionId) {
        for (AuctionSession session : sessions.values()) {
            if (session != null && session.getAuctionId() == auctionId) {
                return session;
            }
        }
        return null;
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
        sessions.remove(session.getRoomId(), session);
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
        // inner class uses outer class constants for durations
        private final String roomId;
        private final Item item;
        private final int sellerUserId;
        private final String sellerUsername;
        private final Lock bidLock = new ReentrantLock();
        private volatile AuctionStatus status;
        private volatile BigDecimal currentPrice;
        private volatile Integer highestBidderId;
        private volatile Users highestBidder;
        private volatile ScheduledFuture<?> endFuture;
        private volatile long auctionEndTimeMillis;
        // Nếu phiên có bản ghi DB tương ứng, lưu auctionId để ghi transaction
        private volatile int auctionId = -1;
        // Cấu hình auto-bid cho phiên: userId -> config
        private final Map<Integer, AutoBidConfig> autoBids = new ConcurrentHashMap<>();
        // Observers (bidders and watchers) nhận thông báo về auction events
        private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();
        private final Users seller;

        private class AutoBidConfig {
            final int userId;
            final Users user;
            final BigDecimal maxBid;
            final BigDecimal increment;

            AutoBidConfig(Users user, BigDecimal maxBid, BigDecimal increment) {
                this.userId = user.getId();
                this.user = user;
                this.maxBid = maxBid;
                this.increment = increment;
            }
        }

        /**
         * Đăng ký một observer (bidder hoặc watcher) để nhận thông báo auction events
         */
        public void registerObserver(AuctionObserver observer) {
            if (observer != null && !observers.contains(observer)) {
                observers.add(observer);
            }
        }

        /**
         * Broadcast event khi có bid mới
         */
        private void broadcastNewBid(int bidderId, double bidAmount, boolean isAutoBid, String description) {
            for (AuctionObserver observer : observers) {
                observer.onNewBid(auctionId, bidderId, bidAmount, isAutoBid, description);
            }
        }

        /**
         * Broadcast event khi đấu giá kết thúc với người chiến thắng
         */
        private void broadcastAuctionEnded(int winnerId, double finalPrice, String description) {
            for (AuctionObserver observer : observers) {
                observer.onAuctionEnded(auctionId, winnerId, finalPrice, description);
            }
        }

        /**
         * Broadcast event khi đấu giá bị hủy
         */
        private void broadcastAuctionCancelled(String reason) {
            for (AuctionObserver observer : observers) {
                observer.onAuctionCancelled(auctionId, reason);
            }
        }

        public AuctionSession(String roomId, Item item, Users seller, long scheduledEndTimeMillis) {
            this.roomId = roomId;
            this.item = item;
            this.sellerUserId = seller != null ? seller.getId() : item.getUserId();
            this.sellerUsername = seller != null ? seller.getUsername() : "";
            this.seller = seller;
            this.status = AuctionStatus.SCHEDULED;
            this.currentPrice = item.getFirstprice();
            this.highestBidderId = null;
            this.highestBidder = null;
            if (scheduledEndTimeMillis > 0) {
                this.auctionEndTimeMillis = scheduledEndTimeMillis;
            } else {
                this.auctionEndTimeMillis = System.currentTimeMillis() + DEFAULT_INITIAL_DURATION_MS;
            }
        }

        /**
         * Bắt đầu phiên đấu giá.
         * 
         * Quá trình:
         * 1. Thay đổi status thành RUNNING
         * 2. Thông báo cho tất cả observers (watchers)
         * 3. Lên lịch timeout cho phiên đấu giá
         */
        public void begin() {
            if (status == AuctionStatus.CANCELLED || status == AuctionStatus.ENDED) {
                return;
            }
            status = AuctionStatus.RUNNING;
            item.setStatus(com.bidding.shared.Item.STATUS_IN_AUCTION);
            if (seller != null) {
                registerObserver(seller);
            }
            broadcastNewBid(-1, currentPrice.doubleValue(), false,
                    String.format("Đấu giá cho item '%s' bắt đầu. Giá khởi điểm: %s", item.getItemName(), currentPrice));
            scheduleAuctionEnd();
        }

        public String getRoomId() {
            return roomId;
        }

        public Item getItem() {
            return item;
        }

        public int getSellerUserId() {
            return sellerUserId;
        }

        public String getSellerUsername() {
            return sellerUsername;
        }

        private void scheduleAuctionEnd() {
            if (endFuture != null && !endFuture.isDone()) {
                endFuture.cancel(false);
            }
            long delay = Math.max(0, auctionEndTimeMillis - System.currentTimeMillis());
            endFuture = scheduler.schedule(this::finishIfAuctionEnded, delay, TimeUnit.MILLISECONDS);
        }

        /**
         * Hoàn tất phiên nếu hết thời gian và không có bids mới.
         * 
         * Quá trình:
         * 1. Kiểm tra lock để tránh race condition
         * 2. Nếu không có bidders, kết thúc với trạng thái ENDED
         * 3. Nếu có bidders, hoàn tất đấu giá và phân phối tiền
         */
        private void finishIfAuctionEnded() {
            bidLock.lock();
            try {
                if (status != AuctionStatus.RUNNING) {
                    return;
                }
                if (System.currentTimeMillis() < auctionEndTimeMillis) {
                    scheduleAuctionEnd();
                    return;
                }
                if (highestBidderId == null) {
                    status = AuctionStatus.ENDED;
                    item.setStatus(com.bidding.shared.Item.STATUS_UNSOLD);
                    broadcastAuctionCancelled("Không có người đặt giá.");
                    archiveSession(this, AuctionHistory.create(roomId, String.valueOf(item.getItemId()), null, currentPrice, BigDecimal.ZERO, BigDecimal.ZERO, AuctionStatus.ENDED, "Không có người đặt giá."));
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
            item.setStatus(com.bidding.shared.Item.STATUS_SOLD);
            Balance winnerWallet = walletManager.getWalletByUserId(highestBidderId);
            Balance sellerWallet = walletManager.getWalletByUserId(sellerUserId);
            Balance adminWallet = adminUserId != null ? walletManager.getWalletByUserId(adminUserId) : null;

            if (winnerWallet == null || sellerWallet == null || adminWallet == null) {
                broadcastAuctionCancelled("Không thể hoàn tất đấu giá do ví không hợp lệ. Cần xử lý thủ công.");
                archiveSession(this, AuctionHistory.create(roomId, String.valueOf(item.getItemId()), String.valueOf(highestBidderId), currentPrice, BigDecimal.ZERO, BigDecimal.ZERO, AuctionStatus.CANCELLED, "Ví người tham gia hoặc seller/admin không tồn tại."));
                return;
            }

            BigDecimal paid;
            try {
                paid = winnerWallet.commitLockedAmount(currentPrice);
            } catch (IllegalArgumentException ex) {
                broadcastAuctionCancelled("Không thể trừ tiền người thắng: " + ex.getMessage());
                archiveSession(this, AuctionHistory.create(roomId, String.valueOf(item.getItemId()), String.valueOf(highestBidderId), currentPrice, BigDecimal.ZERO, BigDecimal.ZERO, AuctionStatus.CANCELLED, "Commit tiền lỗi: " + ex.getMessage()));
                return;
            }

            BigDecimal sellerShare = paid.multiply(new BigDecimal("0.90"));
            BigDecimal adminShare = paid.multiply(new BigDecimal("0.10"));

            try {
                sellerWallet.deposit(sellerShare);
                adminWallet.deposit(adminShare);
            } catch (IllegalArgumentException ex) {
                broadcastAuctionCancelled("Không thể phân phối tiền thắng: " + ex.getMessage() + ". Hoàn trả người thắng.");
                winnerWallet.deposit(paid);
                archiveSession(this, AuctionHistory.create(roomId, String.valueOf(item.getItemId()), String.valueOf(highestBidderId), currentPrice, BigDecimal.ZERO, BigDecimal.ZERO, AuctionStatus.CANCELLED, "Rollback do phân phối lỗi: " + ex.getMessage()));
                return;
            }

            String winnerName = highestBidder.getUsername();
            broadcastAuctionEnded(highestBidderId, paid.doubleValue(), String.format(
                "Đấu giá kết thúc. Người thắng: %s (ID: %s) với giá %s. Seller '%s' nhận %s, Admin nhận %s.",
                winnerName,
                highestBidderId,
                paid,
                sellerUsername.isEmpty() ? String.valueOf(sellerUserId) : sellerUsername,
                sellerShare,
                adminShare
            ));
            if (highestBidder != null) {
                highestBidder.onAuctionEnded(auctionId, highestBidderId, paid.doubleValue(), String.format("Chúc mừng! Bạn đã thắng phiên đấu giá '%s' với giá %s.", roomId, paid));
            }
            archiveSession(this, AuctionHistory.create(roomId, String.valueOf(item.getItemId()), String.valueOf(highestBidderId), paid, sellerShare, adminShare, AuctionStatus.ENDED, "Đấu giá hoàn tất thành công."));
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
                        ? item.getFirstprice().multiply(MINIMUM_INCREMENT_FACTOR)
                        : currentPrice.multiply(MINIMUM_INCREMENT_FACTOR);

                if (bid.compareTo(requiredMinimum) < 0) {
                    return new AuctionResult(false, String.format("Giá đặt phải ít nhất %s (tăng 5%% so với giá hiện tại).", requiredMinimum));
                }

                Balance bidderWallet = walletManager.getWalletByUserId(bidder.getId());
                if (bidderWallet == null) {
                    return new AuctionResult(false, "Không tìm thấy ví của bidder.");
                }

                if (highestBidderId != null && bidder.getId() == highestBidderId) {
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
                    if (auctionEndTimeMillis - System.currentTimeMillis() <= EXTENSION_WINDOW_MS) {
                        auctionEndTimeMillis = auctionEndTimeMillis + EXTENSION_DURATION_MS;
                    }
                    scheduleAuctionEnd();
                    broadcastNewBid(bidder.getId(), currentPrice.doubleValue(), false, String.format("Bidder '%s' đã tiếp tục giữ vị trí dẫn đầu với giá %s.", bidder.getUsername(), currentPrice));
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
                // unlock previous highest
                if (highestBidderId != null) {
                    Balance previousWallet = walletManager.getWalletByUserId(highestBidderId);
                    if (previousWallet != null) {
                        try {
                            previousWallet.unlockAmount(currentPrice);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                highestBidderId = bidder.getId();
                highestBidder = bidder;
                currentPrice = bid;
                if (auctionEndTimeMillis - System.currentTimeMillis() <= EXTENSION_WINDOW_MS) {
                    auctionEndTimeMillis = auctionEndTimeMillis + EXTENSION_DURATION_MS;
                }
                scheduleAuctionEnd();

                broadcastNewBid(bidder.getId(), currentPrice.doubleValue(), false, String.format("Bidder '%s' đã dẫn đầu phiên với giá %s.", bidder.getUsername(), currentPrice));
                registerObserver(bidder);
                // Ghi lên DB nếu có auctionId
                if (this.auctionId > 0) {
                    new com.bidding.dao.JdbcAuctionDAO().updateBidPrice(this.auctionId, currentPrice.doubleValue(), bidder.getId());
                    new com.bidding.dao.JdbcAuctionDAO().insertTransaction(this.auctionId, bidder.getId(), currentPrice.doubleValue(), String.valueOf(System.currentTimeMillis()), 0);
                }
                // Sau khi một bid thành công, cố gắng kích hoạt auto-bids nếu có
                runAutoBids();

                return new AuctionResult(true, "Đặt giá thành công. Bạn đang dẫn đầu phiên đấu giá.");
            } finally {
                bidLock.unlock();
            }
        }

        /**
         * Đăng ký auto-bid cho user trong phiên này.
         */
        public AuctionResult registerAutoBid(Users bidder, double maxBidDouble, double incrementDouble) {
            if (bidder == null) return new AuctionResult(false, "Người dùng không hợp lệ.");
            BigDecimal maxBid = BigDecimal.valueOf(maxBidDouble);
            BigDecimal increment = BigDecimal.valueOf(incrementDouble <= 0 ? 1 : incrementDouble);
            autoBids.put(bidder.getId(), new AutoBidConfig(bidder, maxBid, increment));
            // Persist auto-bid registration to DB when this session is linked to an auction record
            if (this.auctionId > 0) {
                try {
                    new com.bidding.dao.JdbcAuctionDAO().insertAutoBid(this.auctionId, bidder.getId(), maxBid.doubleValue(), increment.doubleValue(), String.valueOf(System.currentTimeMillis()));
                } catch (Exception ignored) {
                }
            }
            // Immediately try to trigger auto-bid if possible
            bidLock.lock();
            try {
                runAutoBids();
            } finally {
                bidLock.unlock();
            }
            return new AuctionResult(true, "Auto-bid đã được lưu.");
        }

        private void runAutoBids() {
            boolean progressed;
            do {
                progressed = false;
                BigDecimal requiredMinimum = currentPrice.multiply(MINIMUM_INCREMENT_FACTOR);
                AutoBidConfig best = null;
                for (AutoBidConfig cfg : autoBids.values()) {
                    if (cfg.userId == highestBidderId) continue;
                    if (cfg.maxBid.compareTo(requiredMinimum) >= 0) {
                        if (best == null || cfg.maxBid.compareTo(best.maxBid) > 0) {
                            best = cfg;
                        }
                    }
                }
                if (best == null) break;

                // Determine bid amount:
                // - At least the required minimum (5% rule)
                // - Prefer currentPrice + increment from auto-bid config
                // - Do not exceed user's maxBid
                BigDecimal stepBased = currentPrice.add(best.increment);
                BigDecimal candidate = requiredMinimum.max(stepBased);
                BigDecimal nextBid = candidate.min(best.maxBid);

                Balance bestWallet = walletManager.getWalletByUserId(best.userId);
                if (bestWallet == null) {
                    autoBids.remove(best.userId);
                    continue;
                }

                // unlock previous highest
                if (highestBidderId != null) {
                    Balance previousWallet = walletManager.getWalletByUserId(highestBidderId);
                    if (previousWallet != null) {
                        try { previousWallet.unlockAmount(currentPrice); } catch (IllegalArgumentException ignored) {}
                    }
                }

                try {
                    bestWallet.lockAmount(nextBid);
                } catch (IllegalArgumentException ex) {
                    // cannot lock, remove auto-bid or skip
                    autoBids.remove(best.userId);
                    continue;
                }

                highestBidderId = best.userId;
                highestBidder = best.user;
                currentPrice = nextBid;

                if (auctionEndTimeMillis - System.currentTimeMillis() <= EXTENSION_WINDOW_MS) {
                    auctionEndTimeMillis = auctionEndTimeMillis + EXTENSION_DURATION_MS;
                }
                scheduleAuctionEnd();
                broadcastNewBid(best.userId, currentPrice.doubleValue(), true, String.format("Auto-bid: '%s' đã dẫn đầu với giá %s.", best.user.getUsername(), currentPrice));
                registerObserver(best.user);
                // Persist auto-bid transaction if linked to DB auction
                if (this.auctionId > 0) {
                    new com.bidding.dao.JdbcAuctionDAO().updateBidPrice(this.auctionId, currentPrice.doubleValue(), best.userId);
                    new com.bidding.dao.JdbcAuctionDAO().insertTransaction(this.auctionId, best.userId, currentPrice.doubleValue(), String.valueOf(System.currentTimeMillis()), 1);
                }
                progressed = true;
            } while (progressed);
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
        public Integer getHighestBidderId() {
            return highestBidderId;
        }

        public int getAuctionId() {
            return auctionId;
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
