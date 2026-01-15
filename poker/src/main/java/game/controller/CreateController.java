package game.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import game.players.Host;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

/**
 * Controller for Create-skærmen.
 * Håndterer oprettelse af en ny poker-server.
 */
public class CreateController implements Initializable {

    // Default værdier
    private static final int DEFAULT_PORT = 9001;
    private static final String DEFAULT_NAME = "Host";

    @FXML
    private TextField portTextField;

    @FXML
    private TextField nameTextField;

    @FXML
    private Text statusText;

    private Host host;
    private static Host sharedHost; // Delt reference så andre kan tilgå

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Sæt default værdier
        portTextField.setText(String.valueOf(DEFAULT_PORT));
        nameTextField.setText(DEFAULT_NAME);
    }

    @FXML
    private void onCreateClicked() {
        try {
            // Hent værdier fra tekstfelter
            int port = Integer.parseInt(portTextField.getText().trim());
            String name = nameTextField.getText().trim();

            if (name.isEmpty()) {
                name = DEFAULT_NAME;
            }

            // Opret og start host
            host = new Host(port, name);
            host.start();
            sharedHost = host;
            

            System.out.println("Server oprettet på port " + port + " som " + name);

            // Skift til table scene
            SceneManager.getInstance().switchScene("table");

        } catch (NumberFormatException e) {
            System.err.println("Ugyldigt portnummer!");
        } catch (Exception e) {
            System.err.println("Kunne ikke starte server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onBackClicked() throws IOException {
        // Stop host hvis den kører
        if (host != null) {
            host.stop();
        }
        SceneManager.getInstance().switchScene("menu");
    }

    // Statisk metode så andre kan tilgå host
    public static Host getSharedHost() {
        return sharedHost;
    }
}

