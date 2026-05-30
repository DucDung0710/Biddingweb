package com.bidding.app;


import java.io.IOException;

import com.bidding.util.SceneManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BiddingApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // 1. Khởi tạo backend services
        AppInitializer.initialize();
        
        // 2. Setup GUI
        SceneManager.setStage(stage);
        FXMLLoader fxmlLoader = new FXMLLoader(BiddingApplication.class.getResource("/uilogin.view/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Bidding Application");
        stage.setScene(scene);
        
        // 3. Handle shutdown
        stage.setOnCloseRequest(event -> {
            AppInitializer.shutdown();
        });
        
        stage.show();
    }
    
    public static void main(String[] args) {
        launch();
    }
}
