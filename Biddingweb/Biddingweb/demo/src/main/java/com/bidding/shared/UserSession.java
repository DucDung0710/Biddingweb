package com.bidding.shared;

import com.bidding.shared.Users;

public class UserSession {
    private static UserSession instance;
    private Users loggedInUser;

    private UserSession() {}

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public Users getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(Users loggedInUser) {
        this.loggedInUser = loggedInUser;
    }

    public void clear() {
        this.loggedInUser = null;
    }

    /**
     * Convenience static method for controllers to log out the current user.
     */
    public static void logout() {
        UserSession session = getInstance();
        session.clear();
    }
}