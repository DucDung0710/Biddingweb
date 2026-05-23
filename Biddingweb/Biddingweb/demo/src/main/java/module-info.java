module demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;
    requires javafx.base;

    // 1. Cấu hình các gói Tiện ích & Engine
    exports com.bidding.util;
    exports com.bidding.engine;
    exports com.bidding.shared;
    opens com.bidding.util to javafx.graphics, javafx.fxml;
    opens com.bidding.engine to javafx.fxml;
    opens com.bidding.shared to javafx.fxml;

    // 2. Cấu hình gói Main App
    exports com.bidding.app;
    opens com.bidding.app to javafx.fxml, javafx.graphics;

    // 3. Cấu hình các gói Controllers (Mở cho FXML và Graphics)
    exports com.bidding.controller;
    opens com.bidding.controller to javafx.fxml, javafx.graphics;

    exports com.bidding.controller.bidder;
    opens com.bidding.controller.bidder to javafx.fxml, javafx.graphics;

    exports com.bidding.controller.admin;
    opens com.bidding.controller.admin to javafx.fxml, javafx.graphics;

    exports com.bidding.model;
    opens com.bidding.model to javafx.base, javafx.fxml;
}