package com.bidding.shared;

// class Cha Users đại diện cho người dùng chung, có thể là Bidder, Seller hoặc Admin
public class Users {
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