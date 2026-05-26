package com.bidding;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import com.bidding.engine.AuctionOperator;
import com.bidding.engine.AuctionRoom;
import com.bidding.shared.Balance;
import com.bidding.shared.Item;
import com.bidding.shared.ItemManager;
import com.bidding.shared.UserManager;
import com.bidding.shared.Users;
import com.bidding.shared.WalletManager;

public class Main {
    public static void main(String[] args) {
        UserManager userManager = new UserManager();
        WalletManager walletManager = new WalletManager();
        ItemManager itemManager = new ItemManager();

        // Tạo tài khoản
        userManager.signUp("admin", "adminpass", "25023196@vnu.edu.vn", "Admin");
        userManager.signUp("seller", "sellerpass", "seller@example.com", "Seller");
        userManager.signUp("bidder", "bidderpass", "bidder@example.com", "Bidder");

        Users admin = userManager.signIn("admin", "adminpass");
        Users seller = userManager.signIn("seller", "sellerpass");
        Users bidder = userManager.signIn("bidder", "bidderpass");

        if (admin == null || seller == null || bidder == null) {
            System.out.println("Không thể khởi tạo người dùng. Kiểm tra lại đăng ký và đăng nhập.");
            return;
        }

        // Đăng ký ví cho người dùng
        walletManager.registerNewWallet(admin);
        walletManager.registerNewWallet(seller);
        walletManager.registerNewWallet(bidder);

        // Nạp tiền thử nghiệm
        walletManager.depositDirectly(bidder.getId(), BigDecimal.valueOf(250));
        walletManager.depositDirectly(seller.getId(), BigDecimal.valueOf(50));
        walletManager.depositDirectly(admin.getId(), BigDecimal.valueOf(0));

        System.out.println("\n=== Số dư ban đầu ===");
        printBalance(walletManager, seller);
        printBalance(walletManager, bidder);
        printBalance(walletManager, admin);

        // Seller đăng ký item cần đấu giá
        itemManager.registerNewItem(seller.getId(), "Máy ảnh Sony", "Máy ảnh full-frame 24MP", 100.0);
        Item item = itemManager.getItemById("1");
        if (item == null) {
            System.out.println("Không tìm thấy item vừa đăng ký.");
            return;
        }

        // Tạo phòng đấu giá và duyệt bởi Admin
        AuctionRoom room = new AuctionRoom(item, seller);
        boolean approved = room.adminApproveItem(admin, "ROOM-001", "secret123");
        if (!approved) {
            System.out.println("Admin không thể duyệt phòng đấu giá.");
            return;
        }

        room.inviteUser(bidder);

        // Tạo AuctionOperator và lập lịch đấu giá bắt đầu ngay lập tức
        AuctionOperator operator = new AuctionOperator(walletManager, admin.getId());
        AuctionOperator.AuctionResult scheduleResult = operator.scheduleAuction(room, System.currentTimeMillis());
        System.out.println("[Scheduler] " + scheduleResult.getMessage());

        // Chờ phòng đấu giá bắt đầu
        sleepMillis(1100);

        AuctionOperator.AuctionSession session = operator.getSession(room.getRoomId());
        if (session == null || !session.isStarted()) {
            System.out.println("Phiên đấu giá chưa bắt đầu. Hãy kiểm tra lại trạng thái room.");
            operator.shutdown();
            return;
        }

        AuctionRoom.InvitationResult inviteResult = room.acceptInvitationWithResult((com.bidding.shared.AuctionObserver) bidder, room.getRoomId(), "secret123");
        System.out.println("[Invitation] " + inviteResult.getMessage());

        AuctionOperator.AuctionResult bidResult = operator.placeBid(room.getRoomId(), bidder, 110.0);
        System.out.println("[Bid] " + bidResult.getMessage());

        System.out.println("\n=== Số dư sau khi đặt giá ===");
        printBalance(walletManager, bidder);
        printBalance(walletManager, seller);
        printBalance(walletManager, admin);

        System.out.println("\nLưu ý: Phiên đấu giá sẽ kết thúc tự động sau khi không có thêm giá trong vòng 3 phút.");
        System.out.println("Bạn có thể tiếp tục mở rộng Main.java với các thao tác đặt giá và kiểm tra lịch sử đấu giá.");

        operator.shutdown();
    }

    private static void printBalance(WalletManager walletManager, Users user) {
        Balance wallet = walletManager.getWalletByUserId(user.getId());
        if (wallet != null) {
            System.out.println(user.getUsername() + " (" + user.getRole() + ") => current=" + wallet.getAmount() + ", locked=" + wallet.getLockedAmount());
        } else {
            System.out.println("Không tìm thấy ví của " + user.getUsername());
        }
    }

    private static void sleepMillis(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
