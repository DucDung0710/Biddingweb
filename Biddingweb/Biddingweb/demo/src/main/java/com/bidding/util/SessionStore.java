package com.bidding.util;

import com.bidding.shared.Users;

public class SessionStore {
    private static Users currentUser;

    public static void setCurrentUser(Users user) {
        currentUser = user;
    }

    public static Users getCurrentUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
}

