package com.bidding.dao;

import com.bidding.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcWalletDepositRequestDAO implements WalletDepositRequestDAO {

    @Override
    public boolean insertDepositRequest(int userId, double amount, String status, String createdAt) {
        String sql = "INSERT INTO wallet_deposit_requests (user_id, amount, status, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDouble(2, amount);
            ps.setString(3, status);
            ps.setString(4, createdAt);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi insertDepositRequest: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<DepositRequest> getDepositRequestsByStatus(String status) {
        List<DepositRequest> requests = new ArrayList<>();
        String sql = "SELECT dr.id, dr.user_id, dr.amount, dr.status, dr.created_at, u.username FROM wallet_deposit_requests dr " +
                     "JOIN users u ON dr.user_id = u.id WHERE dr.status = ? ORDER BY dr.created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapRowToDepositRequest(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi getDepositRequestsByStatus: " + e.getMessage());
        }
        return requests;
    }

    @Override
    public List<DepositRequest> getDepositRequestsByUserId(int userId) {
        List<DepositRequest> requests = new ArrayList<>();
        String sql = "SELECT dr.id, dr.user_id, dr.amount, dr.status, dr.created_at, u.username FROM wallet_deposit_requests dr " +
                     "JOIN users u ON dr.user_id = u.id WHERE dr.user_id = ? ORDER BY dr.created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapRowToDepositRequest(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi getDepositRequestsByUserId: " + e.getMessage());
        }
        return requests;
    }

    @Override
    public DepositRequest getDepositRequestById(int requestId) {
        String sql = "SELECT dr.id, dr.user_id, dr.amount, dr.status, dr.created_at, u.username FROM wallet_deposit_requests dr " +
                     "JOIN users u ON dr.user_id = u.id WHERE dr.id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToDepositRequest(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi getDepositRequestById: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean updateDepositRequestStatus(int requestId, String status) {
        String sql = "UPDATE wallet_deposit_requests SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, requestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi updateDepositRequestStatus: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<DepositRequest> getPendingRequests() {
        return getDepositRequestsByStatus("PENDING");
    }

    @Override
    public int countPendingRequests() {
        String sql = "SELECT COUNT(*) as cnt FROM wallet_deposit_requests WHERE status = 'PENDING'";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi countPendingRequests: " + e.getMessage());
        }
        return 0;
    }

    private DepositRequest mapRowToDepositRequest(ResultSet rs) throws SQLException {
        return new DepositRequest(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getDouble("amount"),
                rs.getString("status"),
                rs.getString("created_at"),
                rs.getString("username")
        );
    }
}

