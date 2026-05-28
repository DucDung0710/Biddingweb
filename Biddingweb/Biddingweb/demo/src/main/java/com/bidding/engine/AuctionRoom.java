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
     * Đăng ký một user làm observer để nhận thông báo mời.
     * Sử dụng Observer pattern để tách rời AuctionRoom khỏi chi tiết User.
     * 
     * @param user AuctionObserver (user) cần đăng ký
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
     * Bỏ đăng ký observer khỏi danh sách thông báo
     * 
     * @param user AuctionObserver cần bỏ đăng ký
     */
    public void removeObserver(AuctionObserver user) {
        observers.remove(user);
    }

    /**
     * Gửi thông báo tới tất cả observers đã đăng ký.
     * Thường dùng để thông báo mời tham gia, cập nhật trạng thái đấu giá, v.v.
     * 
     * @param message Nội dung thông báo gửi đến các observers
     */
    public void notifyObservers(String message) {
        for (AuctionObserver observer : observers) {
            observer.update(message);
        }
    }

    /**
     * Bước Admin: duyệt phép bán item và tạo phòng đấu giá.
     * 
     * Qui trình:
     * - Kiểm tra người yêu cầu có phải Admin không
     * - Kiểm tra item có hợp lệ không
     * - Cập nhật trạng thái item thành "Approved"
     * - Tạo phòng với roomId, password, lưu thời điểm duyệt
     * - Gửi thông báo mời tới tất cả observers
     * 
     * @param admin Đối tượng Users với vai trò Admin
     * @param roomId ID của phòng đấu giá (duy nhất)
     * @param password Mật khẩu để vào phòng
     * @return true nếu duyệt thành công, false nếu thất bại
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
     * Gửi lời mời tới một user cụ thể.
     * User sẽ được đăng ký làm observer nếu chưa có.
     * 
     * @param user AuctionObserver cần mời
     * @return true nếu gửi thành công (phòng đã duyệt), false nếu phòng chưa duyệt
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
     * Gửi lời mời tới tất cả observers đã đăng ký.
     * Thường được gọi sau khi Admin duyệt item.
     * 
     * @return true nếu gửi thành công, false nếu phòng chưa được duyệt
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
     * User chấp nhận lời mời vào phòng đấu giá.
     * 
     * Kiểm tra và xác minh:
     * - Phòng đã được duyệt (approved)
     * - Người dùng hợp lệ
     * - Room ID và password chính xác
     * - Thời hạn 5 tiếng chưa hết
     * - Số lượng tham gia chưa đạt giới hạn (100 người)
     * - Người dùng chưa tham gia trước đó
     * 
     * @param user AuctionObserver chấp nhận lời mời
     * @param roomId Room ID được cung cấp bởi người dùng
     * @param password Mật khẩu được cung cấp bởi người dùng
     * @return InvitationResult chứa kết quả và thông điệp
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

    /**
     * Phiên bản rút gọn của acceptInvitationWithResult (chỉ trả về kết quả boolean)
     * 
     * @param user AuctionObserver chấp nhận lời mời
     * @param roomId Room ID được cung cấp
     * @param password Mật khẩu được cung cấp
     * @return true nếu chấp nhận thành công, false nếu thất bại
     */
    public boolean acceptInvitation(AuctionObserver user, String roomId, String password) {
        return acceptInvitationWithResult(user, roomId, password).isAccepted();
    }

    /**
     * Kiểm tra phòng đấu giá có đang mở không.
     * Phòng mở = đã duyệt + chưa hết thời hạn + chưa đầy người
     * 
     * @return true nếu phòng đang mở, false nếu đóng
     */
    public boolean isRoomOpen() {
        return approved && !isExpired() && acceptedUserIds.size() < MAX_PARTICIPANTS;
    }

    /**
     * Kiểm tra thời hạn 5 tiếng kể từ khi Admin duyệt đã hết chưa.
     * 
     * @return true nếu hết hạn, false nếu vẫn còn thời hạn
     */
    public boolean isExpired() {
        if (!approved) {
            return false;
        }
        return System.currentTimeMillis() - approvedTimeMillis > INVITATION_DURATION_MS;
    }

    /**
     * Lớp đặng dữ biểu thể kết quả khi user chấp nhận lời mời.
     * Chứa thông tin thành công/thất bại và thông điệp chi tiết.
     */
    public static class InvitationResult {
        // Kết quả chấp nhận hay từ chối
        private final boolean accepted;
        // Thông điệp mô tả chi tiết lý do
        private final String message;

        /**
         * Khởi tạo InvitationResult
         * @param accepted true nếu chấp nhận thành công, false nếu thất bại
         * @param message Thông điệp mô tả lý do
         */
        public InvitationResult(boolean accepted, String message) {
            this.accepted = accepted;
            this.message = message;
        }

        /**
         * Kiểm tra xem lời mời có được chấp nhận hay không
         * @return true nếu chấp nhận, false nếu từ chối
         */
        public boolean isAccepted() {
            return accepted;
        }

        /**
         * Lấy thông điệp mô tả
         * @return Thông điệp
         */
        public String getMessage() {
            return message;
        }
    }

    // ============= Các getter tiện lợi =============

    /**
     * Lấy số lượng người đã chấp nhận tham gia
     * @return Số lượng người tham gia hiện tại
     */
    public int getAcceptedCount() {
        return acceptedUserIds.size();
    }

    /**
     * Lấy danh sách ID của những người đã chấp nhận (không thể sửa đổi)
     * @return Tập hợp ID của những người tham gia
     */
    public Set<String> getAcceptedUserIds() {
        return Collections.unmodifiableSet(acceptedUserIds);
    }

    /**
     * Lấy ID phòng đấu giá
     * @return Room ID
     */
    public String getRoomId() {
        return roomId;
    }

    /**
     * Lấy ID của người bán
     * @return Seller user ID
     */
    public String getSellerUserId() {
        return sellerUserId;
    }

    /**
     * Lấy tên của người bán
     * @return Seller username
     */
    public String getSellerUsername() {
        return sellerUsername;
    }

    /**
     * Lấy item được đấu giá
     * @return Đối tượng Item
     */
    public Item getItem() {
        return item;
    }

    /**
     * Lấy thời điểm lên lịch bắt đầu
     * @return Thời điểm bắt đầu (milliseconds)
     */
    public long getScheduledStartTimeMillis() {
        return scheduledStartTimeMillis;
    }

    /**
     * Thiết lập thời điểm bắt đầu phiên đấu giá
     * @param scheduledStartTimeMillis Thời điểm bắt đầu (milliseconds)
     */
    public void setScheduledStartTimeMillis(long scheduledStartTimeMillis) {
        this.scheduledStartTimeMillis = scheduledStartTimeMillis;
    }

    /**
     * Kiểm tra phòng có được duyệt hay không
     * @return true nếu đã duyệt, false nếu chưa
     */
    public boolean isApproved() {
        return approved;
    }
}

