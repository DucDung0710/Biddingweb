package com.bidding;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import com.bidding.engine.AuctionOperator;
import com.bidding.shared.Balance;
import com.bidding.shared.Item;
import com.bidding.shared.ItemManager;
import com.bidding.shared.UserManager;
import com.bidding.shared.Users;
import com.bidding.shared.WalletManager;

/**
 * Lớp Main - Chương trình chính minh họa quy trình đấu giá.
 * 
 * Quy trình đấu giá:
 * 1. Tạo tài khoản cho 3 người dùng: Admin, Seller, Bidder
 * 2. Đăng ký ví và nạp tiền cho từng người dùng
 * 3. Seller đăng ký item muốn bán
 * 4. Admin duyệt item và tạo phòng đấu giá
 * 5. Mời Bidder tham gia phòng đấu giá
 * 6. Lên lịch bắt đầu phiên đấu giá
 * 7. Bidder chấp nhận lời mời và đặt giá
 * 8. Phiên tự động kết thúc sau 3 phút không có giá mới
 * 9. Xử lý thanh toán cho người thắng giá
 */
public class Main {
    /**
     * Phương thức main - điểm vào chương trình
     * Thực hiện quy trình đấu giá từ đầu đến cuối
     * 
     * @param args Tham số dòng lệnh (không sử dụng)
     */
    public static void main(String[] args) {
        // Khởi tạo các manager
        UserManager userManager = new UserManager();
        WalletManager walletManager = new WalletManager();
        ItemManager itemManager = new ItemManager();

        // ========== Bước 1: Tạo tài khoản ==========
        // Đăng ký ba tài khoản với 3 vai trò khác nhau
        userManager.signUp("admin", "adminpass", "25023196@vnu.edu.vn", "Admin");
        userManager.signUp("seller", "sellerpass", "seller@example.com", "Seller");
        userManager.signUp("bidder", "bidderpass", "bidder@example.com", "Bidder");

        // Đăng nhập để lấy đối tượng Users
        Users admin = userManager.signIn("admin", "adminpass");
        Users seller = userManager.signIn("seller", "sellerpass");
        Users bidder = userManager.signIn("bidder", "bidderpass");

        // Kiểm tra đăng nhập thành công
        if (admin == null || seller == null || bidder == null) {
            System.out.println("Không thể khởi tạo người dùng. Kiểm tra lại đăng ký và đăng nhập.");
            return;
        }

        // ========== Bước 2: Đăng ký ví và nạp tiền ==========
        // Đăng ký ví cho mỗi người dùng
        walletManager.registerNewWallet(admin);
        walletManager.registerNewWallet(seller);
        walletManager.registerNewWallet(bidder);

        // Nạp tiền thử nghiệm
        // Bidder cần tiền để đặt giá, seller nạp ít hơn (chỉ để kiểm tra)
        walletManager.depositDirectly(bidder.getId(), BigDecimal.valueOf(250));
        walletManager.depositDirectly(seller.getId(), BigDecimal.valueOf(50));
        walletManager.depositDirectly(admin.getId(), BigDecimal.valueOf(0));

        // Hiển thị số dư ban đầu
        System.out.println("\n=== Số dư ban đầu ===");
        printBalance(walletManager, seller);
        printBalance(walletManager, bidder);
        printBalance(walletManager, admin);

        // ========== Bước 3: Seller đăng ký item ==========
        // Seller đăng ký item cần bán: Máy ảnh Sony, giá khởi điểm 100
        itemManager.registerNewItem(seller.getId(), "Máy ảnh Sony", "Máy ảnh full-frame 24MP", 100.0);
        Item item = itemManager.getItemById(1);
        if (item == null) {
            System.out.println("Không tìm thấy item vừa đăng ký.");
            return;
        }

        // ========== Bước 4: Admin duyệt item và lên lịch phiên đấu giá ==========
        item.setStatus(Item.STATUS_APPROVED);
        AuctionOperator operator = new AuctionOperator(walletManager, String.valueOf(admin.getId()));
        AuctionOperator.AuctionResult scheduleResult = operator.scheduleAuction(item, seller, "ROOM-001", System.currentTimeMillis());
        System.out.println("[Scheduler] " + scheduleResult.getMessage());

        // Chờ phiên bắt đầu (cần ít nhất 1 giây)
        sleepMillis(1100);

        AuctionOperator.AuctionSession session = operator.getSession("ROOM-001");
        if (session == null || !session.isStarted()) {
            System.out.println("Phiên đấu giá chưa bắt đầu. Hãy kiểm tra lại trạng thái phiên đấu giá.");
            operator.shutdown();
            return;
        }

        // ========== Bước 5: Bidder tham gia phiên đấu giá ==========
        session.registerObserver(bidder);
        System.out.println("[Invitation] Bidder đã tham gia phiên đấu giá và sẽ nhận thông báo.");

        // ========== Bước 6: Bidder đặt giá ==========
        AuctionOperator.AuctionResult bidResult = operator.placeBid("ROOM-001", bidder, 110.0);
        System.out.println("[Bid] " + bidResult.getMessage());

        // Hiển thị số dư sau khi đặt giá
        // Lưu ý: tiền sẽ bị khóa tạm thời khi đặt giá
        System.out.println("\n=== Số dư sau khi đặt giá ===");
        printBalance(walletManager, bidder);
        printBalance(walletManager, seller);
        printBalance(walletManager, admin);

        // ========== Hướng dẫn tiếp ==========
        System.out.println("\nLưu ý: Phiên đấu giá sẽ kết thúc tự động sau khi không có thêm giá trong vòng 3 phút.");
        System.out.println("Bạn có thể tiếp tục mở rộng Main.java với các thao tác đặt giá và kiểm tra lịch sử đấu giá.");

        // Dừng AuctionOperator
        operator.shutdown();
    }

    /**
     * In thông tin số dư tài khoản của một người dùng.
     * Bao gồm số tiền hiện có và số tiền bị khóa.
     * 
     * @param walletManager Quản lý ví
     * @param user Người dùng cần in thông tin
     */
    private static void printBalance(WalletManager walletManager, Users user) {
        Balance wallet = walletManager.getWalletByUserId(user.getId());
        if (wallet != null) {
            System.out.println(user.getUsername() + " (" + user.getRole() + ") => current=" + wallet.getAmount() + ", locked=" + wallet.getLockedAmount());
        } else {
            System.out.println("Không tìm thấy ví của " + user.getUsername());
        }
    }

    /**
     * Làm cho thread chính sleep (ngủ) một khoảng thời gian nhất định.
     * Dùng để chờ các task bất đồng bộ hoàn thành.
     * 
     * @param millis Thời gian ngủ (milliseconds)
     */
    private static void sleepMillis(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
