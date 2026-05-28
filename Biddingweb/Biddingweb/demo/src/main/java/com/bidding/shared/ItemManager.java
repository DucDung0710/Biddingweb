package com.bidding.shared;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemManager {
    private final Map<Integer, List<Item>> allItems = new HashMap<>();
    private int nextItemId = 1;

    public void registerNewItem(int userId, String itemName, String description, double price) {
        Item newItem = new Item(nextItemId++, userId, itemName, "GENERAL", description, BigDecimal.valueOf(price));
        allItems.computeIfAbsent(userId, k -> new ArrayList<>()).add(newItem);
        System.out.println("Đã đăng ký mặt hàng: " + itemName + " (ID: " + newItem.getItemId() + ") cho User: " + userId);
    }

    public Item getItemById(int itemId) {
        for (List<Item> items : allItems.values()) {
            for (Item item : items) {
                if (item.getItemId() == itemId) {
                    return item;
                }
            }
        }
        return null;
    }

    public List<Item> getItemsByUserId(int userId) {
        return new ArrayList<>(allItems.getOrDefault(userId, new ArrayList<>()));
    }

    public void updateItem(int itemId, String newItemName, String newDescription) {
        Item item = getItemById(itemId);
        if (item == null) {
            System.out.println("Lỗi: Không tìm thấy mặt hàng mã " + itemId + ".");
            return;
        }
        item.setItemName(newItemName);
        item.setDescription(newDescription);
        item.setStatus("Pending");
        System.out.println("Cập nhật mặt hàng [" + itemId + "] thành công! Mặt hàng sẽ được Admin duyệt lại.");
    }

    public void reviewItem(Users currentUser, int itemId, boolean approve) {
        if (currentUser == null) {
            System.out.println("Lỗi: Bạn phải đăng nhập để thực hiện thao tác này!");
            return;
        }
        if (!currentUser.getRole().equalsIgnoreCase("Admin")) {
            System.out.println("Lỗi: Chỉ Admin mới có quyền phê duyệt sản phẩm!");
            return;
        }
        Item item = getItemById(itemId);
        if (item == null) {
            System.out.println("Lỗi: Không tìm thấy sản phẩm này.");
            return;
        }
        if (approve) {
            item.setStatus("Approved");
            System.out.println("Sản phẩm [" + item.getItemName() + "] đã ĐƯỢC DUYỆT.");
        } else {
            item.setStatus("Rejected");
            System.out.println("Sản phẩm [" + item.getItemName() + "] đã BỊ TỪ CHỐI.");
            deleteItemById(itemId);
        }
    }

    public void deleteItemById(int itemId) {
        for (List<Item> items : allItems.values()) {
            if (items.removeIf(item -> item.getItemId() == itemId)) {
                return;
            }
        }
    }

    public void deleteItemsByUserId(int userId) {
        if (allItems.remove(userId) != null) {
            System.out.println("Đã xóa toàn bộ tất cả mặt hàng liên quan đến người dùng ID: " + userId);
        } else {
            System.out.println("Thông báo: Người dùng ID " + userId + " không có mặt hàng nào để xóa.");
        }
    }

    public List<Item> getAllItemsInSystem() {
        List<Item> totalItems = new ArrayList<>();
        for (List<Item> sellerList : allItems.values()) {
            totalItems.addAll(sellerList);
        }
        return totalItems;
    }

    public void updateItemPriceByAdmin(Users currentUser, int itemId, double newPrice) {
        if (currentUser == null || !currentUser.getRole().equalsIgnoreCase("Admin")) {
            System.out.println("Lỗi: Bạn không có quyền thực hiện thao tác này!");
            return;
        }
        Item item = getItemById(itemId);
        if (item != null) {
            item.setFirstprice(BigDecimal.valueOf(newPrice));
            System.out.println("Cập nhật giá mặt hàng thành công!");
        } else {
            System.out.println("Lỗi: Không tìm thấy sản phẩm này.");
        }
    }
}
