package com.bidding.validator;

import java.util.Arrays;
import java.util.List;

public class UserValidator {

    private final List<String> AUTHORIZED_ADMIN_EMAILS = Arrays.asList(
            "25023196@vnu.edu.vn",
            "25023427@vnu.edu.vn",
            "25023249@vnu.edu.vn",
            "bbuoi812@gmail.com"
    );

    public boolean validateSignUpData(String username, String email, String password, String confirmPassword, String role) {
        if (username == null || username.isEmpty()) return false;
        if (email == null || email.isEmpty()) return false;
        if (password == null || password.isEmpty()) return false;
        if (!password.equals(confirmPassword)) return false;
        if (role == null || role.isEmpty()) return false;
        if (role.equalsIgnoreCase("Admin") && !AUTHORIZED_ADMIN_EMAILS.contains(email)) return false;
        return true;
    }

    public boolean isAuthorizedAdminEmail(String email) {
        return AUTHORIZED_ADMIN_EMAILS.contains(email);
    }
}

