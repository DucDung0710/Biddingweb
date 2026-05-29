package com.bidding.shared;

/**
 * Lớp Users đại diện cho một người dùng trong hệ thống đấu giá.
 */
public class Users implements AuctionObserver {
    private String username;
    private String password;
    private int id;
    private String email;
    protected String role;
    private double balance;

    public Users(String username, String password, int id, String email) {
        this.username = username;
        this.password = password;
        this.id = id;
        this.email = email;
    }

    public Users() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public void update(String message) {
        System.out.println("Thông báo cho " + username + ": " + message);
    }

    @Override
    public int getUserId() {
        return id;
    }
}

class Bidder extends Users {
    public Bidder(String username, String password, int id, String email) {
        super(username, password, id, email);
        this.role = "Bidder";
    }
}

class Seller extends Users {
    public Seller(String username, String password, int id, String email) {
        super(username, password, id, email);
        this.role = "Seller";
    }
}

class Admin extends Users {
    public Admin(String username, String password, int id, String email) {
        super(username, password, id, email);
        this.role = "Admin";
    }
}
