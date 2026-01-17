package game;


import game.controller.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    
    @Override

    public void start(Stage stage) throws Exception {
        SceneManager sceneManager = SceneManager.getInstance();
        sceneManager.setStage(stage);

        sceneManager.switchScene("menu"); // initial scene
        stage.setTitle("Poker Game");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}