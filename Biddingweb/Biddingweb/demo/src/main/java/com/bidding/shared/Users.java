package com.bidding.shared;

//AuctionObserver: Giao diện để các loại người dùng có thể nhận thông báo từ hệ thống đấu giá
interface AuctionObserver {
    void update(String message);
    String getUserId();
}

public class Users implements AuctionObserver {
    private String username;
    private String password;
    private String id;
    private String email;
    private String role;

    public Users(String username, String password, String id, String email) {
        this.username = username;
        this.password = password;
        this.id = id;
        this.email = email;
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

     public String getId(){
        return id;
    }

    public void setId(String id) {
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

    @Override
    public void update(String message) {
        // Ở đây bạn có thể thêm logic để xử lý thông báo, ví dụ: hiển thị trên UI hoặc lưu vào lịch sử thông báo của người dùng
        System.out.println("Thông báo cho " + username + ": " + message);
    }

    @Override
    public String getUserId() {
        return id;
    }

}

// Các lớp con kế thừa từ Users, đại diện cho các loại người dùng cụ thể
class Bidder extends Users {
    public Bidder(String username, String password, String id, String email) {
        super(username, password, id, email);
        setRole("Bidder");
    }
}

class Seller extends Users {
    public Seller(String username, String password, String id, String email) {
        super(username, password, id, email);
        setRole("Seller");
    }
}

class Admin extends Users {




    public Admin(String username, String password, String id, String email) {
        super(username, password, id, email);
        setRole("Admin");  
    }

}