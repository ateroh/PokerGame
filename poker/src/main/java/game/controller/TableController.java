package game.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import game.model.GameModel;
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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

/**
 * Controller for Table-skærmen.
 * Håndterer kun UI-events og kalder model-metoder.
 */
public class TableController implements Initializable {

    @FXML private Rectangle playerCard1, playerCard2;
    @FXML private Text player1Name, player2Name, player3Name, player4Name;
    @FXML private Text playerChipText1, playerChipText2, playerChipText3, playerChipText4;
    @FXML private javafx.scene.control.ListView<String> chatListView;
    @FXML private javafx.scene.control.TextField chatInput;
    @FXML private Text statusText;
    @FXML private Button readyButton, startButton;
    @FXML private Text potText;
    @FXML private Button kick1Button, kick2Button, kick3Button, kick4Button;
    @FXML private Button foldButton, checkButton, callButton, raiseButton;
    @FXML private Slider raiseSlider;
    @FXML private Text playerBetText1, playerBetText2, playerBetText3, playerBetText4;
    @FXML private Circle player1TurnCircle, player2TurnCircle, player3TurnCircle, player4TurnCircle;
    @FXML private Text raiseAmountText;

    private Circle[] turnCircles;
    private Text[] playerSlots, playerChips, chipSlots, betSlots;
    private Button[] kickButtons;
    private TableModel model;
    private List<PlayerInfo> currentPlayers;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        playerSlots = new Text[]{player1Name, player2Name, player3Name, player4Name};
        playerChips = new Text[]{playerChipText1, playerChipText2, playerChipText3, playerChipText4};
        kickButtons = new Button[]{kick1Button, kick2Button, kick3Button, kick4Button};
        turnCircles = new Circle[]{player1TurnCircle, player2TurnCircle, player3TurnCircle, player4TurnCircle};
        chipSlots = new Text[]{playerChipText1, playerChipText2, playerChipText3, playerChipText4};
        betSlots = new Text[]{playerBetText1, playerBetText2, playerBetText3, playerBetText4};

        for (Text betSlot : betSlots) if (betSlot != null) { betSlot.setText(""); betSlot.setVisible(false); }

        Host host = CreateController.getSharedHost();
        PlayerClient client = (host != null) ? host : JoinController.getSharedClient();
        if (host != null) client = null;

        model = new TableModel(host, client);

        // Alle callbacks - Controller reagerer kun på model-events
        model.setOnPlayersUpdated(this::updatePlayerSlots);
        model.setOnStatusUpdated(status -> statusText.setText(status));
        model.setOnKicked(() -> { showAlert("Kicked", "Du blev smidt ud."); goToMenu(); });
        model.setOnServerShutdown(() -> { showAlert("Server lukket", "Hosten lukkede."); goToMenu(); });
        model.setOnCardsDealt(this::displayCards);
        model.setOnMyTurn(this::handleMyTurn);
        model.setOnPlayerAction(this::handlePlayerAction);

        // Skjul kick-knapper hvis man ikke er host
        boolean isHost = model.isHost();
        for (Button kickBtn : kickButtons) if (kickBtn != null) { kickBtn.setVisible(isHost); kickBtn.setManaged(isHost); }

        //Opdater ready-knap teksten
        if (readyButton != null) readyButton.setText("Ready");
        if (startButton != null) { startButton.setVisible(isHost); startButton.setManaged(isHost); }

        hideActionButtons();
        model.startPlayerListUpdater();
        model.startCardListener();
        model.startTurnListener();
        model.startStateListener();

        //Sætter chatten op
        model.getChatManager().setOnMessageReceived(msg ->
            javafx.application.Platform.runLater(() -> {
                if (chatListView != null) {
                    chatListView.getItems().add(msg);
                    chatListView.scrollTo(chatListView.getItems().size() - 1);
                }
            })
        );
    }


    private void displayCards(String[] files) {
        var img1 = new javafx.scene.image.Image(getClass().getResourceAsStream("/cards/" + files[0]));
        var img2 = new javafx.scene.image.Image(getClass().getResourceAsStream("/cards/" + files[1]));
        playerCard1.setFill(new javafx.scene.paint.ImagePattern(img1));
        playerCard1.setVisible(true);
        playerCard2.setFill(new javafx.scene.paint.ImagePattern(img2));
        playerCard2.setVisible(true);
    }

    private void handleMyTurn(int[] info) {
        statusText.setText("DIN TUR!");
        showActionButtons(info[0], info[1], info[2]);
        showTurnIndicator(model.getMyName());
    }

    private void handlePlayerAction(Object[] info) {
        String playerName = (String) info[0];
        int chipsLeft = (Integer) info[1];
        int pot = (Integer) info[2];
        if (potText != null) potText.setText("POT: " + pot);
        updatePlayerChipsDisplay(playerName, chipsLeft);
        if (playerName.equals(model.getMyName())) hideActionButtons();
        showTurnIndicator(null);
    }

    // ============ FXML Event Handlers ============

    @FXML private void onSendChat() {
        String msg = chatInput.getText();
        if (msg != null && !msg.trim().isEmpty()) { model.getChatManager().sendGlobalMessage(msg.trim()); chatInput.clear(); }
    }

    @FXML private void onReadyClicked() {
        model.toggleReady();
        if (readyButton != null) {
            readyButton.setText(model.isReady() ? "Not Ready" : "Ready");
            readyButton.setStyle(model.isReady() ? "-fx-background-color: #4CAF50;" : "-fx-background-color: #2e4d3e;");
        }
    }

    @FXML private void onStartClicked() {
        hideActionButtons();
        if (model.isHost()) {
            model.startGame();
            if (startButton != null) { startButton.setDisable(true); startButton.setText("Game Running"); }
            if (statusText != null) statusText.setText("Game Started");
        }
    }

    @FXML private void onKick1Clicked() { kickPlayerAtIndex(0); }
    @FXML private void onKick2Clicked() { kickPlayerAtIndex(1); }
    @FXML private void onKick3Clicked() { kickPlayerAtIndex(2); }
    @FXML private void onKick4Clicked() { kickPlayerAtIndex(3); }

    private void kickPlayerAtIndex(int i) {
        if (currentPlayers != null && i < currentPlayers.size() && currentPlayers.get(i) != null && !currentPlayers.get(i).isHost)
            model.kickPlayer(currentPlayers.get(i).id);
    }

    @FXML private void onBackClicked() {
        model.leave();
        CreateController.clearSharedHost();
        JoinController.clearSharedClient();
        goToMenu();
    }

    @FXML private void onFoldClicked() { model.sendAction("fold", 0); }
    @FXML private void onCheckClicked() { model.sendAction("check", 0); }
    @FXML private void onCallClicked() { model.sendAction("call", 0); }
    @FXML private void onRaiseClicked() { model.sendAction("raise", (int) raiseSlider.getValue()); }

    // ============ UI Helper Methods ============

    private void updatePlayerSlots(List<PlayerInfo> players) {
        int meIndex = -1;
        for (int i = 0; i < players.size(); i++) if (players.get(i).isMe) { meIndex = i; break; }

        // Roterer listen så man altid vil være ved midten (player 2 position)
        List<PlayerInfo> rotated = new ArrayList<>();
        if (meIndex != -1) {
            int n = players.size();

            rotated.add(n >= 4 ? players.get((meIndex - 1 + n) % n) : null);
            rotated.add(players.get(meIndex));
            if (n >= 2) rotated.add(players.get((meIndex + 1) % n));
            if (n >= 3) rotated.add(players.get((meIndex + 2) % n));
        } else rotated.addAll(players);

        currentPlayers = rotated;

        for (int i = 0; i < playerSlots.length; i++) {
            if (i < rotated.size() && rotated.get(i) != null) {
                PlayerInfo p = rotated.get(i);
                String name = p.name + (p.isMe ? " (dig)" : "") + (p.isHost ? " ★" : "") + (p.isReady ? " ✓" : "");
                playerSlots[i].setText(name);
                playerChips[i].setText("CHIPS: " + p.chips);
                chipSlots[i].setText(String.valueOf(p.chips));
                if (kickButtons[i] != null && model.isHost()) {
                    boolean canKick = !p.isHost && !p.isMe;
                    kickButtons[i].setVisible(canKick); kickButtons[i].setManaged(canKick);
                }
            } else {
                playerSlots[i].setText("Seat Available...");
                if (kickButtons[i] != null) { kickButtons[i].setVisible(false); kickButtons[i].setManaged(false); }
            }
        }
    }

    private void updatePlayerChipsDisplay(String name, int chips) {
        if (currentPlayers == null) return;
        for (int i = 0; i < currentPlayers.size(); i++) {
            PlayerInfo p = currentPlayers.get(i);
            if (p != null && p.name.equals(name) && i < chipSlots.length && chipSlots[i] != null) {
                chipSlots[i].setText(String.valueOf(chips)); break;
            }
        }
    }

    private void showTurnIndicator(String playerName) {
        for (Circle c : turnCircles) if (c != null) c.setVisible(false);
        if (playerName == null || currentPlayers == null) return;
        for (int i = 0; i < currentPlayers.size(); i++) {
            PlayerInfo p = currentPlayers.get(i);
            if (p != null && p.name.equals(playerName) && i < turnCircles.length && turnCircles[i] != null) {
                turnCircles[i].setVisible(true); break;
            }
        }
    }
    // bare hvis vi ikke har minraiseincrement
    private void showActionButtons(int currentBet, int myChips) {
        showActionButtons(currentBet, myChips, GameModel.BIG_BLIND);  
    }

    private void showActionButtons(int currentBet, int myChips, int minRaiseIncrement) {
        if (foldButton != null) foldButton.setVisible(true);
        if (callButton != null) {
            callButton.setVisible(true);
            callButton.setText("CALL " + currentBet);
        }

        if (myChips < minRaiseIncrement) {
            if (raiseButton != null) raiseButton.setVisible(false);
            if (raiseSlider != null) raiseSlider.setVisible(false);
            if (raiseAmountText != null) raiseAmountText.setVisible(false);
        } else {
            if (raiseButton != null) raiseButton.setVisible(true);
            if (raiseSlider != null) {
                raiseSlider.setVisible(true);
                raiseSlider.setMax(myChips);
                raiseSlider.setMin(minRaiseIncrement);  // Use minRaiseIncrement directly
                raiseSlider.setValue(minRaiseIncrement);
                raiseSlider.setShowTickLabels(false);
                raiseSlider.setShowTickMarks(false);
                //hmm
                raiseSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (raiseAmountText != null) {
                        raiseAmountText.setText("+" + newVal.intValue());
                    }
                });
                
                if (raiseAmountText != null) {
                    raiseAmountText.setVisible(true);
                    raiseAmountText.setText("+" + minRaiseIncrement);  
                }
            }
        }
        
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

    private void goToMenu() {
        if (model != null) model.shutdown();
        try { SceneManager.getInstance().switchScene("menu"); } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(String title, String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    public void shutdown() { if (model != null) model.shutdown(); }
}
