package com.bidding.shared;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemManager {
    private final Map<Integer, List<Item>> allItems = new HashMap<>();
    private int nextItemId = 1;

    public int registerNewItem(int userId, String itemName, String description, double price) {
        Item newItem = new Item(nextItemId++, userId, itemName, "GENERAL", description, BigDecimal.valueOf(price));
        newItem.setStatus(Item.STATUS_PENDING);
        newItem.setCurrentPrice(newItem.getFirstprice());
        allItems.computeIfAbsent(userId, k -> new ArrayList<>()).add(newItem);
        System.out.println("Đã đăng ký mặt hàng: " + itemName + " (ID: " + newItem.getItemId() + ") cho User: " + userId);
        return newItem.getItemId();
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

    public Item getItemByUserId(int userId) {
        List<Item> items = allItems.getOrDefault(userId, new ArrayList<>());
        return items.isEmpty() ? null : items.get(0);
    }

    public List<Item> getItemsByUserId(int userId) {
        return new ArrayList<>(allItems.getOrDefault(userId, new ArrayList<>()));
    }

    public void updateItem(int userId, int itemId, String newItemName, String newDescription) {
        Item item = getItemById(itemId);
        if (item == null) {
            System.out.println("Lỗi: Không tìm thấy mặt hàng mã " + itemId + ".");
            return;
        }
        if (item.getUserId() != userId) {
            System.out.println("Lỗi: Bạn chỉ có thể cập nhật sản phẩm của chính mình.");
            return;
        }
        if (Item.STATUS_SOLD.equalsIgnoreCase(item.getStatus())) {
            System.out.println("Lỗi: Không thể cập nhật sản phẩm đã được bán.");
            return;
        }
        item.setItemName(newItemName);
        item.setDescription(newDescription);
        item.setStatus(Item.STATUS_PENDING);
        item.setCurrentPrice(item.getFirstprice());
        System.out.println("Cập nhật mặt hàng [" + itemId + "] thành công! Mặt hàng sẽ được Admin duyệt lại.");
    }

    public boolean reviewItem(Users currentUser, int itemId, boolean approve) {
        if (currentUser == null) {
            System.out.println("Lỗi: Bạn phải đăng nhập để thực hiện thao tác này!");
            return false;
        }
        if (!currentUser.getRole().equalsIgnoreCase("Admin")) {
            System.out.println("Lỗi: Chỉ Admin mới có quyền phê duyệt sản phẩm!");
            return false;
        }
        Item item = getItemById(itemId);
        if (item == null) {
            System.out.println("Lỗi: Không tìm thấy sản phẩm này.");
            return false;
        }
        if (approve) {
            item.setStatus(Item.STATUS_APPROVED);
            System.out.println("Sản phẩm [" + item.getItemName() + "] đã ĐƯỢC DUYỆT.");
            return true;
        } else {
            item.setStatus(Item.STATUS_REJECTED);
            System.out.println("Sản phẩm [" + item.getItemName() + "] đã BỊ TỪ CHỐI.");
            return false;
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

    public List<Item> getItemsByStatus(String status) {
        List<Item> result = new ArrayList<>();
        for (Item item : getAllItemsInSystem()) {
            if (status != null && status.equalsIgnoreCase(item.getStatus())) {
                result.add(item);
            }
        }
        return result;
    }

    public void markItemInAuction(int itemId) {
        Item item = getItemById(itemId);
        if (item != null) {
            item.setStatus(Item.STATUS_IN_AUCTION);
        }
    }

    public void markItemSold(int itemId) {
        Item item = getItemById(itemId);
        if (item != null) {
            item.setStatus(Item.STATUS_SOLD);
        }
    }

    public void markItemUnsold(int itemId) {
        Item item = getItemById(itemId);
        if (item != null) {
            item.setStatus(Item.STATUS_UNSOLD);
        }
    }
}
