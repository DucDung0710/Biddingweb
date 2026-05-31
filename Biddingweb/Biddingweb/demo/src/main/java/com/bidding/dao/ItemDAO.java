package com.bidding.dao;

import com.bidding.shared.Item;
import java.util.List;

public interface ItemDAO {
    boolean insert(Item item);
    List<Item> findAll();
    Item findById(int itemId);
    boolean deleteByUserId(int userId); // Phục vụ hàm xóa sạch sản phẩm của user khi xóa user
    boolean updateCurrentPrice(int itemId, double newPrice); // Phục vụ khi có người bid giá mới
    boolean updateStatus(int itemId, String newStatus);
}