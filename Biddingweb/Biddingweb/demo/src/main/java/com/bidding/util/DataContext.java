package com.bidding.util;

import com.bidding.shared.Users;

/**
 * Singleton lưu trữ trạng thái điều hướng giữa các màn hình.
 * Dùng để truyền dữ liệu khi chuyển scene mà không cần inject Controller.
 */
public class DataContext {
    private static DataContext instance;

    private String searchKeyword    = "";
    private Users  currentUser;
    private int    selectedAuctionId = -1; // ← thêm mới: id phiên đấu giá đang xem

    private DataContext() {}

    public static DataContext getInstance() {
        if (instance == null) {
            instance = new DataContext();
        }
        return instance;
    }

    // ── Search ───────────────────────────────────────────────────

    public String getSearchKeyword() { return searchKeyword; }

    public void setSearchKeyword(String keyword) {
        this.searchKeyword = (keyword != null) ? keyword : "";
    }

    // ── User ─────────────────────────────────────────────────────

    public Users getCurrentUser() { return currentUser; }

    public void setCurrentUser(Users user) { this.currentUser = user; }

    // ── Selected Auction (truyền từ ItemCard → RealtimeBidding) ──

    public int getSelectedAuctionId() { return selectedAuctionId; }

    public void setSelectedAuctionId(int id) { this.selectedAuctionId = id; }

    // ── Reset khi logout ─────────────────────────────────────────

    public void clear() {
        searchKeyword     = "";
        currentUser       = null;
        selectedAuctionId = -1;
    }
}