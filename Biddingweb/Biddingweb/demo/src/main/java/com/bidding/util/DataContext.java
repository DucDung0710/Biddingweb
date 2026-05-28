package com.bidding.util;

import com.bidding.shared.Users;
import com.bidding.model.AuctionDisplayDTO;

public class DataContext {

    private static DataContext instance;
    private String searchKeyword = "";
    private Users currentUser;
    private AuctionDisplayDTO currentAuction;

    private DataContext() {}

    public static DataContext getInstance() {
        if (instance == null) {
            instance = new DataContext();
        }
        return instance;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    public void setSearchKeyword(String keyword) {
        this.searchKeyword = keyword;
    }

    public Users getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(Users user) {
        this.currentUser = user;
    }

    public AuctionDisplayDTO getCurrentAuction() {
        return currentAuction;
    }

    public void setCurrentAuction(AuctionDisplayDTO auction) {
        this.currentAuction = auction;
    }

    public void clear() {
        searchKeyword = "";
        currentUser = null;
        currentAuction = null; // 5. Xóa dữ liệu phiên khi clear session
    }
}