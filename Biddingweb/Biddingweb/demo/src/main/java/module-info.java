module demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;

    exports com.bidding.uilogin;
    exports com.bidding.engine;
    exports com.bidding.shared;
    opens com.bidding.uilogin to javafx.graphics, javafx.fxml;
    opens com.bidding.uilogin.controller to javafx.fxml;

    exports com.bidding.uilogin.controller;

    opens com.bidding.engine to javafx.fxml;
    opens com.bidding.shared to javafx.fxml;

}