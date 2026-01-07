package game.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SceneManager {
    private static SceneManager instance;
    private Stage stage;
    private Map<String, String> scenes = new HashMap<>();
    
    private SceneManager() {
        scenes.put("menu", "/fxml_files/PokerMenu.fxml");
        scenes.put("create", "/fxml_files/PokerCreate.fxml");
        scenes.put("join", "/fxml_files/PokerJoin.fxml");
        scenes.put("table", "/fxml_files/PokerTable.fxml");
    }
    
    public static SceneManager getInstance() {
        if (instance == null) instance = new SceneManager();
        return instance;
    }
    
    public void setStage(Stage stage) { this.stage = stage; }
    
    public void switchScene(String name) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(scenes.get(name)));
        stage.setScene(new Scene(root));
    }
}