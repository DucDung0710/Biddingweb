package com.bidding.dao;

import com.bidding.database.DatabaseConnection;
import com.bidding.shared.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcItemDAO implements ItemDAO {

    @Override
    public boolean insert(Item item) {
        String q = "INSERT INTO items (id, ItemName, description, type, firstprice, status, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setInt(1, item.getItemId());
                ps.setString(2, item.getItemName());
                ps.setString(3, item.getDescription());
                ps.setString(4, item.getType());
                ps.setBigDecimal(5, item.getFirstprice());
                ps.setString(6, item.getStatus());
                ps.setInt(7, item.getUserId());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            // Log exception instead of printing stack trace
            return false;
        }
    }

    @Override
    public List<Item> findAll() {
        List<Item> list = new ArrayList<>();
        String q = "SELECT id, ItemName, description, type, firstprice, status, user_id FROM items";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(q);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Item item = new Item();
                    item.setItemId(rs.getInt("id"));
                    item.setItemName(rs.getString("ItemName"));
                    item.setDescription(rs.getString("description"));
                    item.setType(rs.getString("type"));
                    item.setFirstprice(rs.getBigDecimal("firstprice"));
                    item.setStatus(rs.getString("status"));
                    item.setUserId(rs.getInt("user_id"));
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            // Log exception instead of printing stack trace
        }
        return list;
    }

    @Override
    public Item findById(int itemId) {
        String q = "SELECT id, ItemName, description, type, firstprice, status, user_id FROM items WHERE id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setInt(1, itemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Item item = new Item();
                        item.setItemId(rs.getInt("id"));
                        item.setItemName(rs.getString("ItemName"));
                        item.setDescription(rs.getString("description"));
                        item.setType(rs.getString("type"));
                        item.setFirstprice(rs.getBigDecimal("firstprice"));
                        item.setStatus(rs.getString("status"));
                        item.setUserId(rs.getInt("user_id"));
                        return item;
                    }
                }
            }
        } catch (SQLException e) {
            // Log exception instead of printing stack trace
        }
        return null;
    }

    @Override
    public boolean updateCurrentPrice(int itemId, double newPrice) {
        String q = "UPDATE items SET firstprice = ? WHERE id = ?"; // dùng firstprice đồng bộ cấu trúc của bạn
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setDouble(1, newPrice);
                ps.setInt(2, itemId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            // Log exception instead of printing stack trace
            return false;
        }
    }

    @Override
    public boolean updateStatus(int itemId, String newStatus) {
        // Phục vụ tính năng kiểm duyệt admin gửi sang từ Server
        String q = "UPDATE items SET status = ? WHERE id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setString(1, newStatus);
                ps.setInt(2, itemId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            // Log exception instead of printing stack trace
            return false;
        }
    }

    @Override
    public boolean deleteByUserId(int userId) {
        String q = "DELETE FROM items WHERE user_id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setInt(1, userId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            // Log exception instead of printing stack trace
            return false;
        }
    }
}