package com.bidding.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ItemManager {
  // Kho chứa tất cả mặt hàng của hệ thống (Key: userID, Value: Đối tượng Item)
  private HashMap<String, List<Item>> allItems = new HashMap<>();

  // Hành động: Đăng ký mặt hàng mới khi có Seller mới đăng bán
  public void registerNewItem(
      String itemId, String userId, String itemName, String description, double price) {
    // Bước 1: Tạo đối tượng Item mới
    Item newItem = new Item(itemId, userId, itemName, description, price);

    // Bước 2: Nếu userId này chưa từng đăng bán, tự động khởi tạo 1 danh sách (List) rỗng cho họ
    allItems.computeIfAbsent(userId, k -> new ArrayList<>());

    // Bước 3: Lấy danh sách sản phẩm hiện có của User đó ra
    List<Item> sellerItems = allItems.get(userId);

    // Bước 4: Kiểm tra xem mã sản phẩm (itemId) này đã tồn tại trong kho của họ chưa
    for (Item item : sellerItems) {
      if (item.getItemId().equals(itemId)) {
        System.out.println("Lỗi: Mã sản phẩm " + itemId + " đã tồn tại cho người dùng này!");
        return; // Trùng mã thì dừng lại, không thêm nữa
      }
    }

    // Bước 5: Thêm sản phẩm mới vào danh sách của User
    sellerItems.add(newItem);
    System.out.println("Đăng ký thành công sản phẩm: " + itemName + " cho User: " + userId);
  }

  // Hành động: Tìm mặt hàng dựa trên ID người bán
  public List<Item> getItemByUserId(String userId) {
    return allItems.getOrDefault(userId, new ArrayList<>());
  }

  // Hành động: Cập nhật thông tin mặt hàng ( SELLER)
  public void updateItem(String userId, String itemId, String newItemName, String newDescription) {
    // Bước 1: Lấy ra toàn bộ danh sách sản phẩm của User này
    List<Item> sellerItems = allItems.get(userId);

    // Kiểm tra xem User này đã từng đăng sản phẩm nào chưa
    if (sellerItems == null || sellerItems.isEmpty()) {
      System.out.println("Lỗi: Người dùng này chưa có sản phẩm nào trong hệ thống.");
      return;
    }

    // Bước 2: Duyệt qua danh sách để tìm chính xác sản phẩm có itemId cần sửa
    for (Item item : sellerItems) {
      if (item.getItemId().equals(itemId)) {
        // Bước 3: Tiến hành cập nhật thông tin mới
        item.setItemName(newItemName);
        item.setDescription(newDescription);

        // Chuẩn hóa trạng thái về "PENDING" viết hoa để đồng bộ với bộ lọc Admin
        item.setStatus("PENDING");

        System.out.println(
            "Cập nhật mặt hàng [" + itemId + "] thành công! Mặt hàng sẽ được Admin duyệt lại.");
        return; // Đã tìm thấy và sửa xong thì thoát hàm ngay
      }
    }

    // Nếu chạy hết vòng lặp mà không trùng itemId
    System.out.println(
        "Lỗi: Không tìm thấy sản phẩm mã " + itemId + " thuộc sở hữu của User " + userId);
  }

  public void reviewItem(Users currentUser, String itemId, boolean approve) {
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
    // Duyệt qua toàn bộ kho đồ để tìm sản phẩm có itemId trùng khớp
    for (List<Item> sellerList : allItems.values()) {
      for (Item item : sellerList) {
        if (item.getItemId().equals(itemId)) {
          if (approve) {
            item.setStatus("APPROVED");
            System.out.println("Sản phẩm [" + item.getItemName() + "] đã ĐƯỢC DUYỆT.");
          } else {
            item.setStatus("REJECTED");
            System.out.println("Sản phẩm [" + item.getItemName() + "] đã BỊ TỪ CHỐI.");
            // Nếu bị từ chối, xóa sản phẩm đó ra khỏi List của Seller
            sellerList.remove(item);
          }
          return; // Đã tìm thấy và xử lý xong thì thoát hàm
        }
      }
    }
    System.out.println("Lỗi: Không tìm thấy sản phẩm nào có mã: " + itemId);
  }

    // Hành động: Lấy toàn bộ sản phẩm của tất cả các User trong hệ thống để phục vụ Admin hiển thị bảng
    public List<Item> getAllItemsInSystem() {
        List<Item> totalItems = new ArrayList<>();
        for (List<Item> sellerList : allItems.values()) {
            totalItems.addAll(sellerList);
        }
        return totalItems;
    }

  // Hành động: Xóa TOÀN BỘ mặt hàng khi người bán bị xóa khỏi hệ thống
  public void deleteItemsByUserId(String userId) {
    // Kiểm tra xem người dùng này có danh sách sản phẩm trong hệ thống không
    if (allItems.containsKey(userId)) {
      // .remove(userId) sẽ xóa sạch Key này và giải phóng toàn bộ List<Item> đi kèm của họ
      allItems.remove(userId);
      System.out.println("Đã xóa toàn bộ tất cả mặt hàng liên quan đến người dùng ID: " + userId);
    } else {
      System.out.println("Thông báo: Người dùng ID " + userId + " không có mặt hàng nào để xóa.");
    }
  }

  public void updateItemPriceByAdmin(Users currentUser, String itemId, double newPrice) {
    // 1. Kiểm tra xem người đang thao tác có phải Admin không
    if (currentUser == null || !currentUser.getRole().equalsIgnoreCase("Admin")) {
      System.out.println("Lỗi: Bạn không có quyền thực hiện thao tác này!");
      return;
    }

    /*2. Nếu đúng là Admin, gọi ItemManager để đổi giá( SỬA SAU)
    Item item = getItemByUserId(itemId);
    if (item != null) {
        item.setFirstprice(newPrice);
        }
     else {
        System.out.println("Lỗi: Không tìm thấy sản phẩm này.");
     }
    }*/
  }
}