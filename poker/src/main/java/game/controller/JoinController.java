package game.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import game.players.PlayerClient;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;

/**
 * Controller for Join-skærmen.
 * Håndterer forbindelse til en eksisterende poker-server.
 */
public class JoinController implements Initializable {

    // Default værdier
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9001;
    private static final String DEFAULT_NAME = "Player";

    @FXML
    private TextField hostTextField;

    @FXML
    private TextField portTextField;

    @FXML
    private TextField nameTextField;

    private PlayerClient client;
    private static PlayerClient sharedClient; // Delt reference

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Sæt default værdier
        if (hostTextField != null) {
            hostTextField.setText(DEFAULT_HOST);
        }
        portTextField.setText(String.valueOf(DEFAULT_PORT));
        nameTextField.setText(DEFAULT_NAME);
    }

    @FXML
    private void onJoinClicked() {
        try {
            // Hent værdier fra tekstfelter
            String host = DEFAULT_HOST;
            if (hostTextField != null && !hostTextField.getText().trim().isEmpty()) {
                host = hostTextField.getText().trim();
            }

            int port = Integer.parseInt(portTextField.getText().trim());
            String name = nameTextField.getText().trim();

            if (name.isEmpty()) {
                name = DEFAULT_NAME;
            }

            // Opret og forbind client
            client = new PlayerClient(host, port, name);
            client.connect();
            sharedClient = client;

            System.out.println("Forbundet til " + host + ":" + port + " som " + name);

            // Skift til table scene
            SceneManager.getInstance().switchScene("table");

        } catch (NumberFormatException e) {
            System.err.println("Ugyldigt portnummer!");
        } catch (Exception e) {
            System.err.println("Kunne ikke forbinde: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onBackClicked() throws IOException {
        // Disconnect hvis forbundet
        if (client != null) {
            client.disconnect();
        }
        SceneManager.getInstance().switchScene("menu");
    }

    // Statisk metode så andre kan tilgå client
    public static PlayerClient getSharedClient() {
        return sharedClient;
    }
    
}

