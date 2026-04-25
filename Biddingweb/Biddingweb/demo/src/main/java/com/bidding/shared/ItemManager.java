package com.bidding.shared;

import java.util.HashMap;

public class ItemManager {
    // Kho chứa tất cả mặt hàng của hệ thống (Key: userID, Value: Đối tượng Item)
    private HashMap<String, Item> allItems = new HashMap<>();

    // Hành động: Đăng ký mặt hàng mới khi có Seller mới đăng bán
    public void registerNewItem(String userId, String itemName, String description, double price) {
        if (!allItems.containsKey(userId)) {
            allItems.put(userId, new Item(itemName, description, price));
        }
    }

    // Hành động: Tìm mặt hàng dựa trên ID người bán
    public Item getItemByUserId(String userId) {
        return allItems.get(userId);
    }

    // Hành động: Cập nhật thông tin mặt hàng 
    public void updateItem(String userId, String newItemName, String newDescription) {
        Item item = getItemByUserId(userId);
        if (item != null) {
            item.setItemName(newItemName);
            item.setDescription(newDescription);
            item.setStatus("Pending"); // Cập nhật trạng thái về "Pending" sau khi chỉnh sửa
            System.out.println("Cập nhật mặt hàng thành công! Mặt hàng sẽ được Admin duyệt lại.");
        }
    }

    public void reviewItem(Users currentUser, String userId, boolean approve) {
    // BƯỚC 1: Kiểm tra xem có ai đang đăng nhập không
    if (currentUser == null) {
        System.out.println("Lỗi: Bạn phải đăng nhập để thực hiện thao tác này!");
        return;
    }

    // Kiểm tra quyền 
    // Lưu ý: getRole() đã viết trong class Users
    if (!currentUser.getRole().equalsIgnoreCase("Admin")) {
        System.out.println("Lỗi: Chỉ Admin mới có quyền phê duyệt sản phẩm!");
        return;
    }

    // Nếu đúng là Admin thì mới thực hiện thay đổi trạng thái
    Item item = getItemByUserId(userId); // Hàm tìm item theo ID trong list
    if (item != null) {
        if (approve) {
            item.setStatus("APPROVED");
            System.out.println("Sản phẩm đã được duyệt và cho phép bán.");
        } else {
            item.setStatus("REJECTED");
            System.out.println("Sản phẩm đã bị từ chối.");
            if (allItems.containsKey(userId)) {
                allItems.remove(userId); // Xóa sản phẩm khỏi hệ thống nếu bị từ chối
            }
        }
    }
     else {
        System.out.println("Lỗi: Không tìm thấy sản phẩm này.");
    }
}
    
 // Hành động: Xóa mặt hàng khi người bán bị xóa hoặc Admin từ chối sản phẩm   
    public void deleteItemsByUserId(String userId) {
        if (allItems.containsKey(userId)) {
            allItems.remove(userId);
            System.out.println("Đã xóa tất cả mặt hàng liên quan đến người dùng ID: " + userId);
        }
    }

    public void updateItemPriceByAdmin(Users currentUser, String itemId, double newPrice) {
    // 1. Kiểm tra xem người đang thao tác có phải Admin không
    if (currentUser == null || !currentUser.getRole().equalsIgnoreCase("Admin")) {
        System.out.println("Lỗi: Bạn không có quyền thực hiện thao tác này!");
        return;
        }

    // 2. Nếu đúng là Admin, gọi ItemManager để đổi giá
    Item item = getItemByUserId(itemId);
    if (item != null) {
        item.setFirstprice(newPrice);
        }
     else {
        System.out.println("Lỗi: Không tìm thấy sản phẩm này.");
     }
    }
}