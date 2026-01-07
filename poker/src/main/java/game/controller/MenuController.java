package game.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;


public class MenuController implements Initializable{
    static SceneManager sceneManager = SceneManager.getInstance();

    public void start() throws IOException {
    }

    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    public void onCreateClicked(ActionEvent event) throws IOException {
        sceneManager.switchScene("create");
    }

    public void onJoinClicked(ActionEvent event) throws IOException {
        sceneManager.switchScene("join");
    }

    public void onBackClicked(ActionEvent event) throws IOException {
        sceneManager.switchScene("menu");
    }

    public void onToTableClicked(ActionEvent event) throws IOException {
        sceneManager.switchScene("table");
    }

}

