package game.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import game.model.TableModel;
import game.model.TableModel.PlayerInfo;
import game.players.Host;
import game.players.PlayerClient;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

/**
 * Controller for Table-skærmen.
 * Viser spillere og håndterer spillet.
 */
public class TableController implements Initializable {

    @FXML
    private Rectangle playerRedCard1_1, playerRedCard1_2, playerRedCard2_1, playerRedCard2_2,
                    playerRedCard3_1, playerRedCard3_2, playerRedCard4_1, playerRedCard4_2;
    
    @FXML
    private Rectangle flopCard1, flopCard2, flopCard3, turnCard, riverCard, playerCard1, playerCard2;

    @FXML
    private Text player1Name, player2Name, player3Name, player4Name;

    @FXML
    private Text statusText;

    @FXML
    private Button readyButton;

    @FXML
    private VBox playerListBox;

    @FXML
    private Button kick1Button, kick2Button, kick3Button, kick4Button;

    private Text[] playerSlots;
    private Button[] kickButtons;
    private TableModel model;
    private List<PlayerInfo> currentPlayers;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        playerSlots = new Text[]{player1Name, player2Name, player3Name, player4Name};
        kickButtons = new Button[]{kick1Button, kick2Button, kick3Button, kick4Button};

        // Hent host eller client reference
        Host host = CreateController.getSharedHost();
        PlayerClient client = JoinController.getSharedClient();

        // Opret model
        model = new TableModel(host, client);

        // Setup callbacks
        model.setOnPlayersUpdated(this::updatePlayerSlots);
        model.setOnStatusUpdated(status -> statusText.setText(status));
        model.setOnKicked(() -> {
            showAlert("Kicked", "Du blev smidt ud af hosten.");
            goToMenu();
        });
        model.setOnServerShutdown(() -> {
            showAlert("Server lukket", "Hosten har lukket serveren.");
            goToMenu();
        });

        // Skjul kick-knapper hvis ikke host
        boolean isHost = model.isHost();
        for (Button kickBtn : kickButtons) {
            if (kickBtn != null) {
                kickBtn.setVisible(isHost);
                kickBtn.setManaged(isHost);
            }
        }

        // Opdater ready-knap tekst
        if (readyButton != null) {
            readyButton.setText("Ready");
        }

        // Start model
        model.startPlayerListUpdater();
    }

    /**
     * Opdaterer spiller-pladserne på bordet.
     */
    private void updatePlayerSlots(List<PlayerInfo> players) {
        currentPlayers = players;

        for (int i = 0; i < playerSlots.length; i++) {
            if (i < players.size()) {
                PlayerInfo player = players.get(i);
                String displayName = player.name;

                if (player.isMe) {
                    displayName += " (dig)";
                }
                if (player.isHost) {
                    displayName += " ★";
                }
                if (player.isReady) {
                    displayName += " ✓";
                }

                playerSlots[i].setText(displayName);

                // Vis/skjul kick-knap (ikke for host selv eller sig selv)
                if (kickButtons[i] != null && model.isHost()) {
                    boolean canKick = !player.isHost && !player.isMe;
                    kickButtons[i].setVisible(canKick);
                    kickButtons[i].setManaged(canKick);
                }
            } else {
                playerSlots[i].setText("Seat Available...");
                if (kickButtons[i] != null) {
                    kickButtons[i].setVisible(false);
                    kickButtons[i].setManaged(false);
                }
            }
        }
    }

    @FXML
    private void onReadyClicked() {
        model.toggleReady();
        if (readyButton != null) {
            readyButton.setText(model.isReady() ? "Not Ready" : "Ready");
            readyButton.setStyle(model.isReady()
                ? "-fx-background-color: #4CAF50; -fx-text-fill: white;"
                : "-fx-background-color: #2e4d3e; -fx-text-fill: #d4db51;");
        }
    }

    @FXML
    private void onKick1Clicked() { kickPlayerAtIndex(0); }
    @FXML
    private void onKick2Clicked() { kickPlayerAtIndex(1); }
    @FXML
    private void onKick3Clicked() { kickPlayerAtIndex(2); }
    @FXML
    private void onKick4Clicked() { kickPlayerAtIndex(3); }

    private void kickPlayerAtIndex(int index) {
        if (currentPlayers != null && index < currentPlayers.size()) {
            PlayerInfo player = currentPlayers.get(index);
            if (!player.isHost) {
                model.kickPlayer(player.id);
            }
        }
    }

    @FXML
    private void onBackClicked() {
        model.leave();
        goToMenu();
    }

    private void goToMenu() {
        try {
            SceneManager.getInstance().switchScene("menu");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * Kaldes når vinduet lukkes.
     */
    public void shutdown() {
        if (model != null) {
            model.shutdown();
        }
    }
}

