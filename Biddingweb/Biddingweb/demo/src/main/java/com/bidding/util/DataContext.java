package com.bidding.util;

public class DataContext {
    private static String searchKeyword = "";

    public static String getSearchKeyword() {
        return searchKeyword;
    }

    public static void setSearchKeyword(String keyword) {
        searchKeyword = keyword;
    }

    public static void clear() {
        searchKeyword = "";
    }
}