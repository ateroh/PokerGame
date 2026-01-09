package game.controller;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Space;

import game.players.Host;
import game.players.PlayerClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.text.Text;

/**
 * Controller for Table-skærmen.
 * Viser spillere og håndterer spillet.
 */
public class TableController implements Initializable {

    @FXML
    private ListView<String> playerListView;

    @FXML
    private Text statusText;

    private Host host;
    private PlayerClient client;
    private Thread updateThread;
    private boolean running = true;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Hent host eller client reference
        host = CreateController.getSharedHost();
        client = JoinController.getSharedClient();

        if (host != null) {
            // Vi er host - hent spillere direkte fra Host objekt
            statusText.setText("Du er host - venter på spillere...");
            startHostPlayerListUpdater();
        } else if (client != null) {
            // Vi er client - hent spillere fra tuple space
            statusText.setText("Forbundet som: " + client.getUsername());
            startClientPlayerListUpdater();
        }
    }

    /**
     * Starter en tråd der opdaterer spillerlisten for Host.
     */
    private void startHostPlayerListUpdater() {
        updateThread = new Thread(() -> {
            while (running) {
                try {
                    // Hent spillere fra host
                    List<String> players = host.getPlayers();

                    // Opdater UI på JavaFX tråden
                    Platform.runLater(() -> {
                        playerListView.getItems().clear();
                        for (String player : players) {
                            if (player.equals(host.getUsername())) {
                                playerListView.getItems().add(player + " (dig/host)");
                            } else {
                                playerListView.getItems().add(player);
                            }
                        }
                        statusText.setText("Spillere: " + players.size());
                    });

                    // Vent lidt før næste opdatering
                    Thread.sleep(1000);

                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    /**
     * Starter en tråd der opdaterer spillerlisten for Client.
     * Henter spillerlisten fra tuple space.
     */
    private void startClientPlayerListUpdater() {
        updateThread = new Thread(() -> {
            Space gameSpace = client.getGameSpace();
            String myName = client.getUsername();

            while (running) {
                try {
                    // Query spillerlisten fra tuple space (uden at fjerne den)
                    Object[] result = gameSpace.query(
                        new ActualField("playerlist"),
                        new FormalField(String.class)
                    );

                    if (result != null) {
                        String playerListStr = (String) result[1];
                        List<String> players = Arrays.asList(playerListStr.split(","));

                        // Opdater UI på JavaFX tråden
                        Platform.runLater(() -> {
                            playerListView.getItems().clear();
                            for (String player : players) {
                                if (player.equals(myName)) {
                                    playerListView.getItems().add(player + " (dig)");
                                } else if (players.indexOf(player) == 0) {
                                    // Første spiller er typisk host
                                    playerListView.getItems().add(player + " (host)");
                                } else {
                                    playerListView.getItems().add(player);
                                }
                            }
                            statusText.setText("Spillere: " + players.size());
                        });
                    }

                    // Vent lidt før næste opdatering
                    Thread.sleep(1000);

                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    @FXML
    private void onBackClicked() {
        running = false;
        if (host != null) {
            host.stop();
        }
        if (client != null) {
            client.disconnect();
        }
        try {
            SceneManager.getInstance().switchScene("menu");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Kaldes når vinduet lukkes.
     */
    public void shutdown() {
        running = false;
        if (updateThread != null) {
            updateThread.interrupt();
        }
    }
}

