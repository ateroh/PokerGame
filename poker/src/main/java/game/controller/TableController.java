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
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

/**
 * Controller for Table-skærmen.
 * Viser spillere og håndterer spillet.
 */
public class TableController implements Initializable {

    @FXML
    private Rectangle playerRedCard1_1, playerRedCard1_2, playerRedCard2_1, playerRedCard2_2;
    
    @FXML
    private Rectangle playerCard1, playerCard2;

    @FXML
    private Text player1Name, player2Name, player3Name, player4Name;

    @FXML
    private Text playerChipText1, playerChipText2, playerChipText3, playerChipText4;
    
    @FXML
    private javafx.scene.control.ListView<String> chatListView;
    
    @FXML
    private javafx.scene.control.TextField chatInput;

    @FXML
    private Text statusText;

    @FXML
    private Button readyButton;

    @FXML
    private VBox playerListBox;

    @FXML
    private Button startButton;

    @FXML private Text potText;

    @FXML
    private Button kick1Button, kick2Button, kick3Button, kick4Button;

    @FXML private Button foldButton, checkButton, callButton, raiseButton;
    @FXML private Slider raiseSlider;
   
    @FXML 
    private Text playerBetText1, playerBetText2, playerBetText3, playerBetText4;

    @FXML
    private Circle player1TurnCircle, player2TurnCircle, player3TurnCircle, player4TurnCircle;

    @FXML 
    private Text raiseAmountText;

    private Circle[] turnCircles;
    private Text[] playerSlots;
    private Text[] playerChips;
    private Button[] kickButtons;
    private Text[] chipSlots;
    private Text[] betSlots;
    private TableModel model;
    private List<PlayerInfo> currentPlayers;
    
    private volatile boolean running = true;
    private Thread displayThread;
    private Thread gameStateMonitorThread;

    

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        playerSlots = new Text[]{player1Name, player2Name, player3Name, player4Name};
        playerChips = new Text[]{playerChipText1, playerChipText2, playerChipText3, playerChipText4};
        kickButtons = new Button[]{kick1Button, kick2Button, kick3Button, kick4Button};
        turnCircles = new Circle[]{player1TurnCircle, player2TurnCircle, player3TurnCircle, player4TurnCircle};
        chipSlots = new Text[]{playerChipText1, playerChipText2, playerChipText3, playerChipText4};
        betSlots = new Text[]{playerBetText1, playerBetText2, playerBetText3, playerBetText4};
        for (Text betSlot : betSlots) {
            if (betSlot != null) {
                betSlot.setText("");
                betSlot.setVisible(false);
            }
        }


        // Hent host eller client reference
        Host host = CreateController.getSharedHost();
        PlayerClient client = (host != null) ? host : JoinController.getSharedClient();
        if (host != null) client = null;
        System.out.println("CreateController.getSharedHost() = " + CreateController.getSharedHost());
        System.out.println("JoinController.getSharedClient() = " + JoinController.getSharedClient());

        
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
        hideActionButtons();
        model.startPlayerListUpdater();
        DisplayCards();
        monitorForMyTurn();
        monitorGameState();

        System.out.println("TableController init: myName=" + model.getMyName());
        
        // Setup chat
        model.getChatManager().setOnMessageReceived(msg -> {
            javafx.application.Platform.runLater(() -> {
                if (chatListView != null) {
                    chatListView.getItems().add(msg);
                    chatListView.scrollTo(chatListView.getItems().size() - 1);
                }
            });
        });
    }

    @FXML
    private void onSendChat() {
        String msg = chatInput.getText();
        if (msg != null && !msg.trim().isEmpty()) {
            model.getChatManager().sendGlobalMessage(msg.trim());
            chatInput.clear();
        }
    }

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

                String displayChips = "CHIPS: " + player.chips;
                playerChips[i].setText(displayChips);
                chipSlots[i].setText(String.valueOf(player.chips));

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
        hideActionButtons();
        if (model.isHost()) {
            model.startGame(); 
             //DisplayCards(); 
            
            if (startButton != null) {
                startButton.setDisable(true);
                startButton.setText("Game Running");
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
        if (CreateController.getSharedHost() != null) {
            game.controller.CreateController.clearSharedHost(); // host
        }
        
        if (JoinController.getSharedClient() != null) {
            game.controller.JoinController.clearSharedClient(); // er client
        }
        goToMenu();
    }

    private void goToMenu() {
        shutdown();
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
        running = false;
        if (displayThread != null) {
            displayThread.interrupt();
        }
        if (model != null) {
            model.shutdown();
        }
    }

    private void DisplayCards() {
        displayThread = new Thread(() -> {
            try {
                Space gameSpace = model.getGameSpace();
                while (running && gameSpace == null) {
                    gameSpace = model.getGameSpace();
                    Thread.sleep(50);
                }
                
                if (!running) return;

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
                
                if (!running) return;

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
                // Expected on shutdown
            }
        });
        displayThread.start();
    }

    private void monitorForMyTurn() {
        new Thread(() -> {
            while (true) {
                try {
                    Space gameSpace = model.getGameSpace();
                    if (gameSpace == null) {
                        Thread.sleep(100);
                        continue;
                    }
                    
                    String myName = model.getMyName();
                    
                    // tjek om min tur
                    Object[] turnInfo = gameSpace.get(
                        new ActualField("yourTurn"),
                        new ActualField(myName),
                        new FormalField(Integer.class),
                        new FormalField(Integer.class),
                        new FormalField(Integer.class)
                    );
                    
                    if (turnInfo != null) {
                        int currentBet = (Integer) turnInfo[2];
                        int myChips = (Integer) turnInfo[3];
                        int lastRaise = (Integer) turnInfo[4];
                        System.out.println("DEBUG RECEIVED: currentBet=" + currentBet + ", myChips=" + myChips + ", minRaiseIncrement=" + lastRaise);
                        javafx.application.Platform.runLater(() -> {
                            statusText.setText("DIN TUR!");
                            showActionButtons(currentBet, myChips, lastRaise);
                            showTurnIndicator(myName);
                        });
                    }
                    
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    @SuppressWarnings("unused")
    private void sendAction(String action, int amount) {
        new Thread(() -> {
            try {
                Space gameSpace = model.getGameSpace();
                gameSpace.put("action", model.getMyName(), action, amount);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void updatePotDisplay(int pot) {
        if (potText != null) {
            potText.setText("POT: " + pot);
        }
    }

    private void updatePlayerChipsDisplay(String playerName, int chips) {
        if (currentPlayers == null) return;
        
        for (int i = 0; i < currentPlayers.size(); i++) {
            PlayerInfo p = currentPlayers.get(i);
            if (p != null && p.name.equals(playerName)) {
                if (i < chipSlots.length && chipSlots[i] != null) {
                    chipSlots[i].setText(String.valueOf(chips));
                }
                break;
            }
        }
    }

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

    private void showTurnIndicator(String playerName) {
        // gem alle cirkler
        for (Circle circle : turnCircles) {
            if (circle != null) {
                circle.setVisible(false);
            }
        }
        
        // hvis player null: gem alle
        if (playerName == null || currentPlayers == null) {
            return;
        }
        
        for (int i = 0; i < currentPlayers.size(); i++) {
            PlayerInfo p = currentPlayers.get(i);
            if (p != null && p.name.equals(playerName)) {
                if (i < turnCircles.length && turnCircles[i] != null) {
                    turnCircles[i].setVisible(true);
                }
                break;
            }
        }
    }

    //med hjaelp af claude
    private void monitorGameState() {
        gameStateMonitorThread = new Thread(() -> {
            while (running) {
                try {
                    Space gameSpace = model.getGameSpace();
                    if (gameSpace == null) {
                        Thread.sleep(100);
                        continue;
                    }
                    
                    Object[] action = gameSpace.getp(
                        new ActualField("playerAction"),
                        new FormalField(String.class),
                        new FormalField(String.class),
                        new FormalField(Integer.class),
                        new FormalField(Integer.class),
                        new FormalField(Integer.class)
                    );
                    
                    if (action != null) {
                        String playerName = (String) action[1];
                        String actionType = (String) action[2];
                        int betAmount = (Integer) action[3];
                        int pot = (Integer) action[5];
                        int chipsLeft = (Integer) action[4];
                        
                        javafx.application.Platform.runLater(() -> {
                            updatePotDisplay(pot);
                            updatePlayerChipsDisplay(playerName, chipsLeft);
                            updatePlayerBetDisplay(playerName, chipsLeft);
                            
                            if (playerName.equals(model.getMyName())) {
                                hideActionButtons();
                            }
                            showTurnIndicator(null);
                        });
                    }
                    
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        gameStateMonitorThread.setDaemon(true);
        gameStateMonitorThread.start();
    }

    private void showActionButtons(int currentBet, int myChips, int lastRaise) {
        if (foldButton != null) foldButton.setVisible(true);
        if (callButton != null) {
            callButton.setVisible(true);
            callButton.setText("CALL " + currentBet);
        }

        int minRaise = currentBet + lastRaise;

        if (myChips <= currentBet || myChips < minRaise) {
            if (raiseButton != null) raiseButton.setVisible(false);
            if (raiseSlider != null) raiseSlider.setVisible(false);
            if (raiseAmountText != null) raiseAmountText.setVisible(false);
        } else {
            if (raiseButton != null) raiseButton.setVisible(true);
            if (raiseSlider != null) {
                raiseSlider.setVisible(true);
                raiseSlider.setMax(myChips);
                raiseSlider.setMin(minRaise);
                raiseSlider.setValue(minRaise);
                raiseSlider.setShowTickLabels(false);
                raiseSlider.setShowTickMarks(false);

                raiseSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (raiseAmountText != null) {
                        raiseAmountText.setText(String.valueOf(newVal.intValue()));
                    }
                });
                if (raiseAmountText != null) {
                    raiseAmountText.setVisible(true);
                    raiseAmountText.setText(String.valueOf(minRaise));
                }
            }
        }
        // Show check button only if currentBet is 0
        if (checkButton != null) {
            checkButton.setVisible(currentBet == 0);
        }
    }
    private void hideActionButtons() {
        if (foldButton != null) foldButton.setVisible(false);
        if (checkButton != null) checkButton.setVisible(false);
        if (callButton != null) callButton.setVisible(false);
        if (raiseButton != null) raiseButton.setVisible(false);
        if (raiseSlider != null) raiseSlider.setVisible(false);
        if (raiseAmountText != null) raiseAmountText.setVisible(false);
    }

    private void updatePlayerBetDisplay(String playerName, int betAmount) {
        if (currentPlayers == null) return;
        
        for (int i = 0; i < currentPlayers.size(); i++) {
            PlayerInfo p = currentPlayers.get(i);
            if (p != null && p.name.equals(playerName)) {
                if (i < betSlots.length && betSlots[i] != null) {
                    betSlots[i].setText(betAmount > 0 ? String.valueOf(betAmount) : "");
                }
                break;
            }
        }
    }
}