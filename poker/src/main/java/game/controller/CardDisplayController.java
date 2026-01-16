package game.controller;

import game.model.TableModel.PlayerInfo;
import javafx.scene.shape.Circle;

public class CardDisplayController {



  /*  private void showTurnIndicator(String playerName) {
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
    }*/

}
