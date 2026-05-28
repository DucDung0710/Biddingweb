package com.bidding.shared;

import java.math.BigDecimal;

/**
 * Lớp Item đại diện cho một mặt hàng được đưa lên bán đấu giá.
 * 
 * Các thông tin chi tiết:
 * - itemId: ID duy nhất của item
 * - userId: ID người bán (chủ sở hữu)
 * - ItemName: Tên hàng hóa
 * - description: Mô tả chi tiết
 * - status: Trạng thái (Pending, Approved, Rejected)
 * - firstprice: Giá khởi điểm để bắt đầu đấu giá
 * 
 * Vòng đời Item:
 * 1. Pending: Người bán vừa đăng ký item, chờ Admin duyệt
 * 2. Approved: Admin đã duyệt, item có thể được lên lịch đấu giá
 * 3. Rejected: Admin từ chối, item không được đấu giá
 */
public class Item {
    // ID của sản phẩm (auto-increment)
    private String itemId;
    // ID của người bán (chủ sở hữu)
    private String userId;
    // Tên hàng hóa
    private String ItemName;
    // Mô tả chi tiết về item
    private String description;
    // Trạng thái: "Pending" (chờ duyệt), "Approved" (đã duyệt), "Rejected" (từ chối)
    private String status;
    // Giá khởi điểm để bắt đầu đấu giá
    private BigDecimal firstprice;

    /**
     * Khởi tạo Item từ double giá
     * @param itemId ID của item
     * @param userId ID của người bán
     * @param ItemName Tên item
     * @param description Mô tả item
     * @param price Giá khởi điểm (double)
     */
    public Item(String itemId, String userId, String ItemName, String description, double price) {
        this(itemId, userId, ItemName, description, BigDecimal.valueOf(price));
    }

    /**
     * Khởi tạo Item từ BigDecimal giá
     * @param itemId ID của item
     * @param userId ID của người bán
     * @param ItemName Tên item
     * @param description Mô tả item
     * @param price Giá khởi điểm (BigDecimal)
     */
    public Item(String itemId, String userId, String ItemName, String description, BigDecimal price) {
        this.itemId = itemId;
        this.userId = userId;
        this.ItemName = ItemName;
        this.description = description;
        this.status = "Pending"; // Mặc định trạng thái là "Pending"
        this.firstprice = price != null && price.compareTo(BigDecimal.ZERO) >= 0 ? price : BigDecimal.ZERO;
    }

    // ============= Getters và Setters =============

    /**
     * Lấy ID của item
     * @return Item ID
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * Thiết lập ID của item
     * @param itemId Item ID mới
     */
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    /**
     * Lấy ID của người bán
     * @return Seller user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Thiết lập ID của người bán
     * @param userId Seller user ID mới
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Lấy tên của item
     * @return Item name
     */
    public String getItemName() {
        return ItemName;
    }

    /**
     * Thiết lập tên của item
     * @param ItemName Item name mới
     */
    public void setItemName(String ItemName) {
        this.ItemName = ItemName;
    }

    /**
     * Lấy mô tả của item
     * @return Mô tả item
     */
    public String getDescription() {
        return description;
    }

    /**
     * Thiết lập mô tả của item
     * @param description Mô tả mới
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Lấy trạng thái của item
     * @return Trạng thái (Pending, Approved, Rejected)
     */
    public String getStatus() {
        return status;
    }

    /**
     * Thiết lập trạng thái của item
     * @param status Trạng thái mới
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Lấy giá khởi điểm
     * @return Giá khởi điểm
     */
    public BigDecimal getFirstprice() {
        return firstprice;
    }

    /**
     * Thiết lập giá khởi điểm.
     * Giá phải >= 0, nếu không sẽ ném exception.
     * 
     * @param firstprice Giá khởi điểm mới (phải >= 0)
     * @throws IllegalArgumentException nếu giá âm
     */
    public void setFirstprice(BigDecimal firstprice) {
        if (firstprice == null || firstprice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải là số không âm.");
        }
        this.firstprice = firstprice;
    }
}
