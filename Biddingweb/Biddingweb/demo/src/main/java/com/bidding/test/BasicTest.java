package com.bidding.test;

import java.math.BigDecimal;

import com.bidding.shared.Balance;
import com.bidding.shared.Item;
import com.bidding.shared.ItemManager;
import com.bidding.shared.UserManager;
import com.bidding.shared.Users;
import com.bidding.shared.WalletManager;

/**
 * BasicTest - Các test cơ bản cho hệ thống Bidding
 */
public class BasicTest {
    
    public static void main(String[] args) {
        System.out.println("=== Bắt đầu test hệ thống ===\n");
        
        testUserSignUp();
        testWalletOperations();
        testItemRegistration();
        
        System.out.println("\n=== Kết thúc test ===");
    }
    
    private static void testUserSignUp() {
        System.out.println("[TEST] Kiểm tra đăng ký người dùng");
        UserManager userManager = new UserManager();
        
        // Test đăng ký thành công
        boolean result1 = userManager.signUp("alice", "pass123", "alice@example.com", "Bidder");
        assert result1 : "Đăng ký người dùng Bidder thất bại";
        
        // Test đăng ký duplicated
        boolean result2 = userManager.signUp("alice", "pass456", "alice2@example.com", "Seller");
        assert !result2 : "Hệ thống cho phép đăng ký username duplicated";
        
        // Test Admin unauthorized email
        boolean result3 = userManager.signUp("unauthorized_admin", "pass", "unknown@email.com", "Admin");
        assert !result3 : "Hệ thống cho phép Admin với email chưa được authorize";
        
        System.out.println("✓ Kiểm tra đăng ký thành công\n");
    }
    
    private static void testWalletOperations() {
        System.out.println("[TEST] Kiểm tra thao tác ví");
        UserManager userManager = new UserManager();
        WalletManager walletManager = new WalletManager();
        
        // Tạo user
        userManager.signUp("bob", "pass123", "bob@example.com", "Bidder");
        Users bob = userManager.signIn("bob", "pass123");
        
        // Đăng ký ví
        walletManager.registerNewWallet(bob);
        Balance wallet = walletManager.getWalletByUserId(bob.getId());
        assert wallet != null : "Không thể tạo ví";
        assert wallet.getAmount().compareTo(BigDecimal.ZERO) == 0 : "Ví ban đầu phải có 0 đồng";
        
        // Nạp tiền
        walletManager.depositDirectly(bob.getId(), BigDecimal.valueOf(1000));
        assert wallet.getAmount().compareTo(BigDecimal.valueOf(1000)) == 0 : "Nạp tiền thất bại";
        
        // Rút tiền
        boolean withdrew = wallet.withdraw(BigDecimal.valueOf(300));
        assert withdrew : "Rút tiền thất bại";
        assert wallet.getAmount().compareTo(BigDecimal.valueOf(700)) == 0 : "Số dư sau rút không chính xác";
        
        // Test rút tiền không đủ
        boolean failedWithdraw = wallet.withdraw(BigDecimal.valueOf(1000));
        assert !failedWithdraw : "Hệ thống cho rút tiền khi không đủ số dư";
        
        // Test khóa tiền
        wallet.lockAmount(BigDecimal.valueOf(200));
        assert wallet.getAmount().compareTo(BigDecimal.valueOf(500)) == 0 : "Khóa tiền không chính xác";
        assert wallet.getLockedAmount().compareTo(BigDecimal.valueOf(200)) == 0 : "Số tiền khóa không chính xác";
        
        System.out.println("✓ Kiểm tra ví thành công\n");
    }
    
    private static void testItemRegistration() {
        System.out.println("[TEST] Kiểm tra đăng ký item");
        UserManager userManager = new UserManager();
        ItemManager itemManager = new ItemManager();
        
        // Tạo seller
        userManager.signUp("seller1", "pass123", "seller1@example.com", "Seller");
        Users seller = userManager.signIn("seller1", "pass123");
        
        // Đăng ký item
        itemManager.registerNewItem(seller.getId(), "Laptop", "Laptop Dell XPS 13", 500.0);
        Item item = itemManager.getItemById(1);
        
        assert item != null : "Không thể lấy item vừa đăng ký";
        assert item.getItemName().equals("Laptop") : "Tên item không chính xác";
        assert item.getFirstprice().compareTo(BigDecimal.valueOf(500.0)) == 0 : "Giá khởi điểm không chính xác";
        assert item.getUserId() == seller.getId() : "Seller ID không chính xác";
        
        System.out.println("✓ Kiểm tra item đăng ký thành công\n");
    }
}
