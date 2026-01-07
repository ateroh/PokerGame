package game.controller;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import game.players.Host;
import game.players.PlayerClient;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;


public class MenuController implements Initializable {

    @FXML
    private Button createButton;

    @FXML
    private Button joinButton;

    private Host host;
    private PlayerClient client;

    public void start() throws IOException {
        // Initialization logic if needed
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Controller is ready
    }

    @FXML
    private void onCreateClicked() {
        // Dialog til at oprette server
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Create Game");
        dialog.setHeaderText("Host a new poker game");

        // Knapper
        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        // Form layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setText("Host");

        TextField portField = new TextField();
        portField.setPromptText("Port");
        portField.setText("9001");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Port:"), 0, 1);
        grid.add(portField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                return new String[]{usernameField.getText(), portField.getText()};
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();

        result.ifPresent(values -> {
            String username = values[0];
            int port = Integer.parseInt(values[1]);

            try {
                // Start server
                host = new Host(port, username);
                host.start();

                showInfo("Server Started", "Game hosted on port " + port + "\nWaiting for players to join...\nYour username: " + username);

            } catch (Exception e) {
                showError("Error", "Could not start server: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void onJoinClicked() {
        // Dialog til at joine server
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Join Game");
        dialog.setHeaderText("Join an existing poker game");

        // Knapper
        ButtonType joinButtonType = new ButtonType("Join", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(joinButtonType, ButtonType.CANCEL);

        // Form layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setText("Player");

        TextField ipField = new TextField();
        ipField.setPromptText("IP Address");
        ipField.setText("localhost");

        TextField portField = new TextField();
        portField.setPromptText("Port");
        portField.setText("9001");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("IP Address:"), 0, 1);
        grid.add(ipField, 1, 1);
        grid.add(new Label("Port:"), 0, 2);
        grid.add(portField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == joinButtonType) {
                return new String[]{usernameField.getText(), ipField.getText(), portField.getText()};
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();

        result.ifPresent(values -> {
            String username = values[0];
            String ip = values[1];
            int port = Integer.parseInt(values[2]);

            try {
                // Connect to server
                client = new PlayerClient(ip, port, username);
                client.connect();

                showInfo("Connected", "Successfully connected to " + ip + ":" + port + "\nYour username: " + username);

            } catch (Exception e) {
                showError("Connection Failed", "Could not connect to server: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
