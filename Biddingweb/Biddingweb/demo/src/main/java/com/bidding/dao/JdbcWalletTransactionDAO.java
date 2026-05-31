package com.bidding.dao;

import com.bidding.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcWalletTransactionDAO implements WalletTransactionDAO {

    @Override
    public boolean insertTransaction(int userId, String type, double amount, double balanceAfter, Integer refId, String note, String createdAt) {
        String sql = "INSERT INTO wallet_transactions (user_id, type, amount, balance_after, ref_id, note, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.setDouble(4, balanceAfter);
            if (refId != null) {
                ps.setInt(5, refId);
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }
            ps.setString(6, note);
            ps.setString(7, createdAt);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi insertTransaction: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<WalletTransaction> getTransactionsByUserId(int userId) {
        List<WalletTransaction> transactions = new ArrayList<>();
        String sql = "SELECT id, user_id, type, amount, balance_after, ref_id, note, created_at FROM wallet_transactions WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi getTransactionsByUserId: " + e.getMessage());
        }
        return transactions;
    }

    @Override
    public List<WalletTransaction> getTransactionsByType(int userId, String type) {
        List<WalletTransaction> transactions = new ArrayList<>();
        String sql = "SELECT id, user_id, type, amount, balance_after, ref_id, note, created_at FROM wallet_transactions WHERE user_id = ? AND type = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi getTransactionsByType: " + e.getMessage());
        }
        return transactions;
    }

    @Override
    public int getTransactionCount(int userId) {
        String sql = "SELECT COUNT(*) as cnt FROM wallet_transactions WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi getTransactionCount: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public List<WalletTransaction> getTransactionHistory(int userId, int limit) {
        List<WalletTransaction> transactions = new ArrayList<>();
        String sql = "SELECT id, user_id, type, amount, balance_after, ref_id, note, created_at FROM wallet_transactions WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi getTransactionHistory: " + e.getMessage());
        }
        return transactions;
    }

    private WalletTransaction mapRowToTransaction(ResultSet rs) throws SQLException {
        return new WalletTransaction(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("type"),
                rs.getDouble("amount"),
                rs.getDouble("balance_after"),
                rs.getObject("ref_id") != null ? rs.getInt("ref_id") : null,
                rs.getString("note"),
                rs.getString("created_at")
        );
    }

}

