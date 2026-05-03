package uilogin;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {
    private static Stage stage;

    public static void setStage(Stage stage) {
        SceneManager.stage = stage;
    }

    public static void switchToLogin() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SceneManager.class.getResource("/uilogin.view/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setScene(scene);
    }

    public static void switchToSignUp() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SceneManager.class.getResource("/uilogin.view/sign-up.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setScene(scene);
    }
}

