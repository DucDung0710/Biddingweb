package com.bidding.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ItemManager {
    // Kho chứa tất cả mặt hàng của hệ thống (Key: userId, Value: Danh sách Items của user đó)
    private HashMap<String, List<Item>> allItems = new HashMap<>();
    private int nextItemId = 1;  // Counter tự động tăng cho itemId

    // Hành động: Đăng ký mặt hàng mới khi có Seller mới đăng bán
    public void registerNewItem(String userId, String itemName, String description, double price) {
        String itemId = String.valueOf(nextItemId++);  // Tự động gán itemId
        Item newItem = new Item(itemId, userId, itemName, description, price);
        
        // Nếu user chưa có item nào, tạo list mới
        allItems.computeIfAbsent(userId, k -> new ArrayList<>()).add(newItem);
        System.out.println("Đã đăng ký mặt hàng: " + itemName + " (ID: " + itemId + ")");
    }

    // Hành động: Lấy tất cả mặt hàng của một user
    public List<Item> getItemsByUserId(String userId) {
        return allItems.getOrDefault(userId, new ArrayList<>());
    }

    // Hành động: Tìm item cụ thể theo itemId
    public Item getItemById(String itemId) {
        for (List<Item> items : allItems.values()) {
            for (Item item : items) {
                if (item.getItemId().equals(itemId)) {
                    return item;
                }
            }
        }
        return null;
    }

    // Hành động: Cập nhật thông tin mặt hàng 
    public void updateItem(String itemId, String newItemName, String newDescription) {
        Item item = getItemById(itemId);
        if (item != null) {
            item.setItemName(newItemName);
            item.setDescription(newDescription);
            item.setStatus("Pending"); // Cập nhật trạng thái về "Pending" sau khi chỉnh sửa
            System.out.println("Cập nhật mặt hàng thành công! Mặt hàng sẽ được Admin duyệt lại.");
        } else {
            System.out.println("Lỗi: Không tìm thấy mặt hàng này.");
        }
    }

    public void reviewItem(Users currentUser, String itemId, boolean approve) {
        // BƯỚC 1: Kiểm tra xem có ai đang đăng nhập không
        if (currentUser == null) {
            System.out.println("Lỗi: Bạn phải đăng nhập để thực hiện thao tác này!");
            return;
        }

        // Kiểm tra quyền 
        if (!currentUser.getRole().equalsIgnoreCase("Admin")) {
            System.out.println("Lỗi: Chỉ Admin mới có quyền phê duyệt sản phẩm!");
            return;
        }

        // Nếu đúng là Admin thì mới thực hiện thay đổi trạng thái
        Item item = getItemById(itemId);
        if (item != null) {
            if (approve) {
                item.setStatus("APPROVED");
                System.out.println("Sản phẩm đã được duyệt và cho phép bán.");
            } else {
                item.setStatus("REJECTED");
                System.out.println("Sản phẩm đã bị từ chối.");
                deleteItemById(itemId);  // Xóa sản phẩm nếu bị từ chối
            }
        } else {
            System.out.println("Lỗi: Không tìm thấy sản phẩm này.");
        }
    }
    
    // Hành động: Xóa một item cụ thể theo itemId
    public void deleteItemById(String itemId) {
        for (String userId : allItems.keySet()) {
            List<Item> items = allItems.get(userId);
            items.removeIf(item -> item.getItemId().equals(itemId));
            // Nếu user không còn item nào, xóa user khỏi map
            if (items.isEmpty()) {
                allItems.remove(userId);
            }
        }
    }

    // Hành động: Xóa tất cả mặt hàng của một user
    public void deleteItemsByUserId(String userId) {
        if (allItems.containsKey(userId)) {
            allItems.remove(userId);
            System.out.println("Đã xóa tất cả mặt hàng liên quan đến người dùng ID: " + userId);
        }
    }

    // Hành động: Admin có thể cập nhật giá khởi điểm của mặt hàng
    public void updateItemPriceByAdmin(Users currentUser, String itemId, double newPrice) {
        // 1. Kiểm tra xem người đang thao tác có phải Admin không
        if (currentUser == null || !currentUser.getRole().equalsIgnoreCase("Admin")) {
            System.out.println("Lỗi: Bạn không có quyền thực hiện thao tác này!");
            return;
        }

        // 2. Nếu đúng là Admin, tìm và cập nhật giá
        Item item = getItemById(itemId);
        if (item != null) {
            item.setFirstprice(newPrice);
            System.out.println("Cập nhật giá mặt hàng thành công!");
        } else {
            System.out.println("Lỗi: Không tìm thấy sản phẩm này.");
        }
    }
}