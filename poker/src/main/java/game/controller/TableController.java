package game.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Space;

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
    private Button startButton;

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
        if (startButton != null) {
            startButton.setVisible(isHost);
            startButton.setManaged(isHost);
        }
        // Start model
        model.startPlayerListUpdater();
        DisplayCards();
        System.out.println("TableController init: host=" + (host != null) +
                   ", client=" + (client != null) +
                   ", myName=" + model.getMyName());

    }

    /**
     * Opdaterer spiller-pladserne på bordet.
     */
    private void updatePlayerSlots(List<PlayerInfo> players) {
        // Find index of current player
        int meIndex = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).isMe) {
                meIndex = i;
                break;
            }
        }

        // Rotate the list so that the current player is at position 1 (player2), with clockwise positioning
        // Slot 0 (player1) = sidste spiller clockwise (modsat dig) - kun hvis 4 spillere
        // Slot 1 (player2) = dig selv
        // Slot 2 (player3) = næste spiller clockwise - kun hvis 2+ spillere
        // Slot 3 (player4) = næste spiller clockwise efter det - kun hvis 3+ spillere
        List<PlayerInfo> rotatedPlayers = new ArrayList<>();
        if (meIndex != -1) {
            int n = players.size();

            // Slot 0: kun vis hvis der er 4 spillere (spilleren før dig), ellers null placeholder
            if (n >= 4) {
                int slot0Index = (meIndex - 1 + n) % n;
                rotatedPlayers.add(players.get(slot0Index));
            } else {
                rotatedPlayers.add(null); // Placeholder for tom plads
            }

            // Slot 1: dig selv (altid)
            rotatedPlayers.add(players.get(meIndex));

            // Slot 2: næste spiller clockwise (kun hvis 2+ spillere)
            if (n >= 2) {
                int slot2Index = (meIndex + 1) % n;
                rotatedPlayers.add(players.get(slot2Index));
            }

            // Slot 3: spilleren 2 efter dig (kun hvis 3+ spillere)
            if (n >= 3) {
                int slot3Index = (meIndex + 2) % n;
                rotatedPlayers.add(players.get(slot3Index));
            }
        } else {
            rotatedPlayers.addAll(players);
        }

        currentPlayers = rotatedPlayers;

        for (int i = 0; i < playerSlots.length; i++) {
            if (i < rotatedPlayers.size() && rotatedPlayers.get(i) != null) {
                PlayerInfo player = rotatedPlayers.get(i);
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
    private void onStartClicked() {
        if (model.isHost()) {
            model.startGame(); 
             //DisplayCards(); 
            
            if (startButton != null) {
                startButton.setDisable(true);
                startButton.setText("Game Running...");
            }
            
            if (statusText != null) {
                statusText.setText("Game Started");
            }
            
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

    private void DisplayCards() {
        new Thread(() -> {
            try {
                Space gameSpace = model.getGameSpace();
                while (gameSpace == null) {
                    gameSpace = model.getGameSpace();
                    Thread.sleep(50);
                }

                String myName = model.getMyName();
                System.out.println("waiting for dealtCards for " + myName);

                Object[] cards = gameSpace.get(
                    new ActualField("dealtCards"),
                    new ActualField(myName),
                    new FormalField(String.class), // suit1
                    new FormalField(String.class), // rank1
                    new FormalField(String.class), // suit2
                    new FormalField(String.class)  // rank2
                );

                String suit1 = (String) cards[2];
                String rank1 = (String) cards[3];
                String suit2 = (String) cards[4];
                String rank2 = (String) cards[5];

                String file1 = rank1 + "_of_" + suit1 + ".png";
                String file2 = rank2 + "_of_" + suit2 + ".png";

                var thing1 = TableController.class.getResourceAsStream("/cards/" + file1);
                var thing2 = TableController.class.getResourceAsStream("/cards/" + file2);

                var img1 = new javafx.scene.image.Image(thing1);
                var img2 = new javafx.scene.image.Image(thing2);

                
                playerCard1.setFill(new javafx.scene.paint.ImagePattern(img1));
                playerCard1.setVisible(true);

                playerCard2.setFill(new javafx.scene.paint.ImagePattern(img2));
                playerCard2.setVisible(true);


            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

}
