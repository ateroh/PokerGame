package game;


import game.controller.MenuController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml_files/PokerMenu.fxml"));
        Parent root = loader.load();

        // Få adgang til MenuController
        MenuController menuController = loader.getController();

        // Kald start for at indhente input og opsætte spillet
        menuController.start();

        Scene scene = new Scene(root);
        primaryStage.setTitle("Poker");
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}