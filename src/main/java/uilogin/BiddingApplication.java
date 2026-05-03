package uilogin;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class BiddingApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        SceneManager.setStage(stage);
        FXMLLoader fxmlLoader = new FXMLLoader(BiddingApplication.class.getResource("/uilogin.view/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("Bidding Application");
        stage.setScene(scene);
        stage.show();
        stage.setResizable(false); // Ngăn người dùng nhấn nút phóng to hoặc kéo giãn cửa sổ
    }
    public static void main(String[] args) {
        launch();
    }
}
