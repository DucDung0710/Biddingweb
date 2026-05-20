// Explanation: Switch UserService to use UserValidator instead of shared.UserManager
package com.bidding.service;

import com.bidding.dao.JdbcUserDAO;
import com.bidding.dao.UserDAO;
import com.bidding.shared.Users;
import com.bidding.validator.UserValidator;

/**
 * Service layer for user-related operations. Uses UserDAO for persistence and UserValidator for validation.
 */
public class UserService {

    private final UserDAO userDao = new JdbcUserDAO();
    private final UserValidator validator = new UserValidator();

    /**
     * Attempt to log in user. Returns Users object on success, null on failure.
     */
    public Users login(String emailOrUsername, String password) {
        Users u = userDao.findByEmail(emailOrUsername);
        if (u == null) {
            u = userDao.findByUsername(emailOrUsername);
        }
        if (u == null) return null;
        // plaintext compare for now (consider hashing)
        if (u.getPassword().equals(password)) return u;
        return null;
    }

    /**
     * Register a new user. Returns true if registration succeeded.
     */
    public boolean register(String fullName, String email, String password, String confirmPassword, String role) {
        // validation
        if (!validator.validateSignUpData(fullName, email, password, confirmPassword, role)) {
            return false;
        }
        // ensure uniqueness
        if (userDao.existsByEmail(email) || userDao.existsByUsername(fullName)) {
            return false;
        }
        // create user id (simple timestamp-based id; replace with UUID if desired)
        String id = String.valueOf(System.currentTimeMillis());
        Users user = new Users(fullName, password, id, email);
        user.setRole(role);
        return userDao.insert(user);
    }
}
