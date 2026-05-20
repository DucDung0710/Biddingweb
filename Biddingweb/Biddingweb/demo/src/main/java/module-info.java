module demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;
    requires javafx.base;

    exports com.bidding.util;
    exports com.bidding.engine;
    exports com.bidding.shared;
    opens com.bidding.util to javafx.graphics, javafx.fxml;

    exports com.bidding.controller;

    opens com.bidding.engine to javafx.fxml;
    opens com.bidding.shared to javafx.fxml;
    opens com.bidding.controller to javafx.fxml,javafx.graphics;
    exports com.bidding.app;
    opens com.bidding.app to javafx.fxml,javafx.graphics;
    exports com.bidding.controller.bidder;
    opens com.bidding.controller.bidder to javafx.fxml,javafx.graphics;

}