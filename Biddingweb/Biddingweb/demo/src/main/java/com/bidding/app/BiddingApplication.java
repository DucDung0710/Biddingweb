package com.bidding.app;

import java.io.IOException;

import com.bidding.util.SceneManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class BiddingApplication extends Application {
    @Override
    public void start(Stage stage) {
        try {
            // 1. Khởi tạo backend services
            AppInitializer.initialize();
        } catch (Exception e) {
            System.err.println("[BiddingApplication] Startup error: " + e.getMessage());
            e.printStackTrace();
        }

        // 2. Setup GUI
        SceneManager.setStage(stage);
        Parent root = loadLoginScreen();
        Scene scene = new Scene(root);
        stage.setTitle("Bidding Application");
        stage.setScene(scene);

        // 3. Handle shutdown
        stage.setOnCloseRequest(event -> AppInitializer.shutdown());
        stage.show();
    }

    private Parent loadLoginScreen() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(BiddingApplication.class.getResource("/uilogin.view/login.fxml"));
            return fxmlLoader.load();
        } catch (IOException | NullPointerException e) {
            System.err.println("[BiddingApplication] Không thể tải login.fxml: " + e.getMessage());
            e.printStackTrace();
            Label errorLabel = new Label("Không thể tải giao diện đăng nhập. Kiểm tra lỗi console.");
            StackPane fallbackPane = new StackPane(errorLabel);
            fallbackPane.setStyle("-fx-padding: 24; -fx-background-color: white;");
            return fallbackPane;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
