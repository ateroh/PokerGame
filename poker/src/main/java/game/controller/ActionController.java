package game.controller;

import game.model.TableModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;

public class ActionController {
    private TableModel model;
    private Button foldButton, checkButton, callButton, raiseButton;
    private Slider raiseSlider;

    @FXML
    private void onFoldClicked() {
        new Thread(() -> {
            try {
                model.getGameSpace().put("action", model.getMyName(), "fold", 0);
            } catch (InterruptedException e) {}
        }).start();
    }

    @FXML
    private void onCheckClicked() {
        new Thread(() -> {
            try {
                model.getGameSpace().put("action", model.getMyName(), "check", 0);
            } catch (InterruptedException e) {}
        }).start();
    }

    @FXML
    private void onCallClicked() {
        new Thread(() -> {
            try {
                model.getGameSpace().put("action", model.getMyName(), "call", 0);
            } catch (InterruptedException e) {}
        }).start();
    }

    @FXML
    private void onRaiseClicked() {
        new Thread(() -> {
            try {
                int amount = (int) raiseSlider.getValue();
                model.getGameSpace().put("action", model.getMyName(), "raise", amount);
            } catch (InterruptedException e) {}
        }).start();
    }



}
