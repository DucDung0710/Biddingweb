package com.bidding.controller.admin;

/**
 * Simple DTO used by AdminUserController for demo/test data.
 */
public class User {
    private final int id;
    private final String fullName;
    private final String email;
    private final String role;
    private final String token;
    private final String createdDate;
    private final String lastLogin;
    private String status;

    public User(int id, String fullName, String email, String role, String token, String createdDate, String lastLogin, String status) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.token = token;
        this.createdDate = createdDate;
        this.lastLogin = lastLogin;
        this.status = status;
    }

    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getToken() { return token; }
    public String getCreatedDate() { return createdDate; }
    public String getLastLogin() { return lastLogin; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getInitials() {
        if (fullName == null || fullName.isEmpty()) return "";
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) sb.append(p.charAt(0));
            if (sb.length() >= 2) break;
        }
        return sb.toString().toUpperCase();
    }
}

