package com.bidding.util;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {
    private static Stage stage;

    public static void setStage(Stage stage) {
        SceneManager.stage = stage;
    }

    public static void switchToLogin() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SceneManager.class.getResource("/uilogin.view/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.setScene(scene);
    }

    public static void switchToSignUp() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SceneManager.class.getResource("/uilogin.view/sign-up.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.setScene(scene);
    }

    public static void switchToDashboard() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SceneManager.class.getResource("/bidder.view/dashboard.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.setScene(scene);
    }

    public static void switchToAdminDashboard() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SceneManager.class.getResource("/admin.view/admin_dashboard.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.setScene(scene);
    }

    public static void switchToSellerDashboard() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SceneManager.class.getResource("/seller.view/seller_dashboard.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.setScene(scene);
    }
}
