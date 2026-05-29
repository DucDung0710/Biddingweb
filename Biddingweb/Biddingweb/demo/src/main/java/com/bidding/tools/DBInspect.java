package com.bidding.tools;

import com.bidding.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBInspect {
    public static void main(String[] args) throws Exception {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement st = conn.createStatement()) {
            System.out.println("Users in DB:");
            try (ResultSet rs = st.executeQuery("SELECT id, username, email, password, role, balance FROM users")) {
                while (rs.next()) {
                    System.out.printf("id=%d, username=%s, email=%s, password=%s, role=%s, balance=%f\n",
                            rs.getInt("id"), rs.getString("username"), rs.getString("email"), rs.getString("password"), rs.getString("role"), rs.getDouble("balance"));
                }
            }
        }
    }
}
