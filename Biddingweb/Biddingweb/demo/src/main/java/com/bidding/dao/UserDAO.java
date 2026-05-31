package com.bidding.dao;

import com.bidding.shared.Users;

public interface UserDAO {
    Users findByEmail(String email);
    Users findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean insert(Users user);
    boolean deleteById(String id);
    boolean updateBalance(int userId, double newBalance);
}

