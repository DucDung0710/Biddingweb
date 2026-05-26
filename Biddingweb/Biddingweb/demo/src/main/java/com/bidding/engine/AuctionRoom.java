package com.bidding.engine;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.bidding.shared.AuctionObserver;
import com.bidding.shared.Item;
import com.bidding.shared.Users;

/**
 * Lớp AuctionRoom chịu trách nhiệm:
 * 1) Cho phép Admin duyệt Item và tạo phòng đấu giá (roomId, password)
 * 2) Gửi lời mời đến các user đã đăng ký (observer pattern)
 * 3) Cho phép user chấp nhận tham gia với giới hạn và thời hạn quy định
 */
public class AuctionRoom {
    // Số lượng tối đa người tham gia
    public static final int MAX_PARTICIPANTS = 100;
    // Thời hạn lời mời: 5 tiếng (tính bằng milliseconds)
    public static final long INVITATION_DURATION_MS = 5 * 60 * 60 * 1000L; // 5 tiếng

    // Item liên quan đến phòng đấu giá này
    private final Item item;  //AuctionRoom gắn cho 1 item
    private final String sellerUserId; // Id của người bán
    private final String sellerUsername; // Tên người bán
    private long scheduledStartTimeMillis; // Thời điểm bắt đầu đấu giá đã được hẹn trước

    // Thông tin phòng
    private String roomId;
    private String password;
    // Trạng thái đã được Admin duyệt chưa
    private boolean approved;
    // Thời điểm (milli) khi Admin duyệt -> dùng để kiểm tra thời hạn 5 tiếng
    private long approvedTimeMillis;

    // Danh sách observers (users sẽ nhận thông báo mời) — dùng interface để giảm coupling
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();
    // Danh sách userId đã được chấp nhận vào phòng
    private final Set<String> acceptedUserIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Khởi tạo AuctionRoom với một Item cụ thể
     * @param item Item cần đấu giá
     */
    public AuctionRoom(Item item) {
        this(item, null);
    }

    public AuctionRoom(Item item, Users seller) {
        this.item = item;
        this.sellerUserId = seller != null ? seller.getId() : item.getUserId();
        this.sellerUsername = seller != null ? seller.getUsername() : "";
        this.approved = false;
        this.roomId = "";
        this.password = "";
        this.approvedTimeMillis = 0;
        this.scheduledStartTimeMillis = 0;
    }

    /**
     * Đăng ký một user làm observer để nhận thông báo mời
     */
    public void registerObserver(AuctionObserver user) {
        if (user == null) {
            return;
        }
        if (!observers.contains(user)) {
            observers.add(user);
        }
    }

    /**
     * Bỏ đăng ký observer
     */
    public void removeObserver(AuctionObserver user) {
        observers.remove(user);
    }

    /**
     * Gửi thông báo tới tất cả observers
     * @param message nội dung thông báo
     */
    public void notifyObservers(String message) {
        for (AuctionObserver observer : observers) {
            observer.update(message);
        }
    }

    /**
     * Bước Admin: duyệt phép bán item và tạo phòng đấu giá
     * - Chỉ Admin mới được gọi
     * - Không chỉnh sửa tên/mô tả item tại đây
     * - Thiết lập roomId, password, thời điểm duyệt
     * - Gửi lời mời tới tất cả observers
     */
    public boolean adminApproveItem(Users admin, String roomId, String password) {
        if (admin == null || !"Admin".equalsIgnoreCase(admin.getRole())) {
            System.out.println("Lỗi: Chỉ Admin mới có quyền duyệt item và tạo phòng đấu giá.");
            return false;
        }
        if (item == null) {
            System.out.println("Lỗi: Không có item nào để duyệt.");
            return false;
        }

        // Chỉ cập nhật trạng thái bán/duyệt để item được phép đưa vào auction
        item.setStatus("Approved");

        // Thiết lập phòng đấu giá
        this.roomId = roomId;
        this.password = password;
        this.approved = true;
        this.approvedTimeMillis = System.currentTimeMillis();
        this.acceptedUserIds.clear(); // reset danh sách tham gia

        // Thông điệp mời (hiển thị tóm tắt cho users)
        String inviteMessage = String.format(
            "Item '%s' (ID: %s) của người bán '%s' đã được duyệt. Phòng đấu giá '%s' đã tạo. Mật khẩu: %s. Thời hạn tham gia: 5 tiếng. Giới hạn: %d người.",
            item.getItemName(),
            item.getItemId(),
            sellerUsername.isEmpty() ? sellerUserId : sellerUsername,
            roomId,
            password,
            MAX_PARTICIPANTS
        );

        notifyObservers(inviteMessage);
        return true;
    }

    /**
     * Gửi lời mời tới một user cụ thể (đăng ký observer nếu chưa có)
     */
    public boolean inviteUser(AuctionObserver user) {
        if (user == null) {
            return false;
        }
        registerObserver(user);
        if (approved) {
            user.update(String.format(
                "Mời bạn tham gia phòng đấu giá '%s' cho item '%s' (ID: %s). Seller: %s. Mật khẩu: %s. Thời hạn 5 tiếng. Giới hạn %d người.",
                roomId,
                item.getItemName(),
                item.getItemId(),
                sellerUsername.isEmpty() ? sellerUserId : sellerUsername,
                password,
                MAX_PARTICIPANTS
            ));
            return true;
        }
        return false;
    }

    /**
     * Gửi lời mời tới tất cả observers (dùng sau khi Admin duyệt)
     */
    public boolean inviteAllObservers() {
        if (!approved) {
            System.out.println("Lỗi: Chỉ có thể gửi lời mời khi phòng đã được tạo và item được duyệt.");
            return false;
        }
        notifyObservers(String.format(
            "Lời mời tham gia phòng đấu giá '%s' cho item '%s' (ID: %s), Seller: %s. Mật khẩu: %s. Thời hạn 5 tiếng. Giới hạn %d người.",
            roomId,
            item.getItemName(),
            item.getItemId(),
            sellerUsername.isEmpty() ? sellerUserId : sellerUsername,
            password,
            MAX_PARTICIPANTS
        ));
        return true;
    }

    /**
     * User chấp nhận lời mời vào phòng
     * - Kiểm tra roomId/password
     * - Kiểm tra thời hạn 5 tiếng
     * - Kiểm tra giới hạn 100 người
     */
    public InvitationResult acceptInvitationWithResult(AuctionObserver user, String roomId, String password) {
        if (!approved) {
            return new InvitationResult(false, "Lỗi: Phòng đấu giá chưa được tạo hoặc item chưa được duyệt.");
        }
        if (user == null) {
            return new InvitationResult(false, "Lỗi: Người dùng không hợp lệ.");
        }
        if (!this.roomId.equals(roomId) || !this.password.equals(password)) {
            return new InvitationResult(false, "Lỗi: Room ID hoặc mật khẩu không đúng.");
        }
        if (isExpired()) {
            return new InvitationResult(false, "Lỗi: Thời gian tham gia phòng đấu giá đã hết (5 tiếng).");
        }
        if (acceptedUserIds.size() >= MAX_PARTICIPANTS) {
            return new InvitationResult(false, "Lỗi: Phòng đấu giá đã đầy, không thể chấp nhận thêm người tham gia.");
        }
        if (acceptedUserIds.contains(user.getUserId())) {
            return new InvitationResult(true, "Bạn đã tham gia phòng đấu giá này trước đó.");
        }

        // Thêm user vào danh sách tham gia
        acceptedUserIds.add(user.getUserId());
        user.update(String.format("Bạn đã được chấp nhận vào phòng đấu giá '%s'. Chúc bạn may mắn!", this.roomId));
        return new InvitationResult(true, String.format("Bạn đã được chấp nhận vào phòng đấu giá '%s'. Chúc bạn may mắn!", this.roomId));
    }

    public boolean acceptInvitation(AuctionObserver user, String roomId, String password) {
        return acceptInvitationWithResult(user, roomId, password).isAccepted();
    }

    /**
     * Kiểm tra phòng còn mở không (đã duyệt, chưa hết hạn, chưa đầy)
     */
    public boolean isRoomOpen() {
        return approved && !isExpired() && acceptedUserIds.size() < MAX_PARTICIPANTS;
    }

    /**
     * Kiểm tra thời hạn 5 tiếng đã hết chưa
     */
    public boolean isExpired() {
        if (!approved) {
            return false;
        }
        return System.currentTimeMillis() - approvedTimeMillis > INVITATION_DURATION_MS;
    }

    public static class InvitationResult {
        private final boolean accepted;
        private final String message;

        public InvitationResult(boolean accepted, String message) {
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

    // Các getter tiện lợi
    public int getAcceptedCount() {
        return acceptedUserIds.size();
    }

    public Set<String> getAcceptedUserIds() {
        return Collections.unmodifiableSet(acceptedUserIds);
    }

    public String getRoomId() {
        return roomId;
    }

    public String getSellerUserId() {
        return sellerUserId;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public Item getItem() {
        return item;
    }

    public long getScheduledStartTimeMillis() {
        return scheduledStartTimeMillis;
    }

    public void setScheduledStartTimeMillis(long scheduledStartTimeMillis) {
        this.scheduledStartTimeMillis = scheduledStartTimeMillis;
    }

    public boolean isApproved() {
        return approved;
    }
}

