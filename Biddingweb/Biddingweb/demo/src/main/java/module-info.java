module com.bidding.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.base;
    requires transitive java.sql;
    requires com.zaxxer.hikari;
    requires com.google.gson;

    opens com.bidding.app to javafx.fxml;
    opens com.bidding.controller to javafx.fxml;
    opens com.bidding.controller.admin to javafx.fxml;
    opens com.bidding.controller.bidder to javafx.fxml;
    opens com.bidding.controller.seller to javafx.fxml;
    opens com.bidding.controller.wallet to javafx.fxml;
    opens com.bidding.model to com.google.gson;

    exports com.bidding.app;
    exports com.bidding.controller.admin;
    exports com.bidding.controller.bidder;
    exports com.bidding.controller.seller;
    exports com.bidding.controller;
    exports com.bidding.service;
    exports com.bidding.dao;
    exports com.bidding.shared;
    exports com.bidding.util;
    exports com.bidding.database;
    exports com.bidding.server;
    exports com.bidding.model;
}
