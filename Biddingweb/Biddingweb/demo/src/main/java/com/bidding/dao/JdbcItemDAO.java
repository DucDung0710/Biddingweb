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
        String q = "INSERT INTO items (id, name, description, type, seller_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, item.getItemId());
            ps.setString(2, item.getItemName());
            ps.setString(3, item.getDescription());
            ps.setString(4, item.getType());
            ps.setInt(5, item.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi insert item: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<Item> findAll() {
        List<Item> list = new ArrayList<>();
        String q = "SELECT id, name, description, type, seller_id FROM items";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(q);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Item item = new Item();
                item.setItemId(rs.getInt("id"));
                item.setItemName(rs.getString("name"));
                item.setDescription(rs.getString("description"));
                item.setType(rs.getString("type"));
                item.setUserId(rs.getInt("seller_id"));
                list.add(item);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi findAll item: " + e.getMessage());
        }
        return list;
    }

    @Override
    public Item findById(int itemId) {
        String q = "SELECT id, name, description, type, seller_id FROM items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Item item = new Item();
                    item.setItemId(rs.getInt("id"));
                    item.setItemName(rs.getString("name"));
                    item.setDescription(rs.getString("description"));
                    item.setType(rs.getString("type"));
                    item.setUserId(rs.getInt("seller_id"));
                    return item;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi findById item: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean updateCurrentPrice(int itemId, double newPrice) {
        String q = "UPDATE auctions SET current_price = ? WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setDouble(1, newPrice);
            ps.setInt(2, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi updateCurrentPrice: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateStatus(int itemId, String newStatus) {
        String q = "UPDATE auctions SET status = ? WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setString(1, newStatus);
            ps.setInt(2, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi updateStatus item: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteByUserId(int userId) {
        String q = "DELETE FROM items WHERE seller_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi deleteByUserId: " + e.getMessage());
            return false;
        }
    }
}