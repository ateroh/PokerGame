package game.model;

import java.util.ArrayList;

public class PlayerModel {
    

    private String name;
    private int chips;
    private int betAmount;
    private String status;
    private String role;
    private HandModel hand;

    public PlayerModel(String name, int startingChips) {
        this.name = name;
        this.chips = startingChips;
        this.betAmount = 0;
        this.hand = new HandModel(new ArrayList<>());
        this.role = "player";
        this.status = "Active";
    }

    public int placeBet(int amount) {
        if (amount > chips) {
            int temp = chips;
            betAmount += chips;
            chips = 0;
            status = "ALLIN";
            return temp;

        } else {
            chips -= amount;
            betAmount += amount;
            return amount;
        }
    }
    
    public void addPotToPlayer(int amount) {
        chips += amount;
    }

    public int call(int amountToMatch) {
        int chipsNeeded = amountToMatch - betAmount;

        if (chipsNeeded >= chips) {
            int temp = chips;
            placeBet(chips); //all in
            return temp;
        } else {
            placeBet(chipsNeeded);
            return chipsNeeded;
        }
    }

    public void resetForNewHand() {
        betAmount = 0;
        
        if (chips > 0) {
            status = "Active";  // har vi brug for active?
        } else {
            status = "Busted";
        }
        
        hand.clear();
    }
    public void resetForNewRound() {
        betAmount = 0;
    }

    public int raise(int raiseAmount) {
        return placeBet(raiseAmount);

    }

    public boolean hasFolded() {
        return status.equals("FOLD");
    }
    public boolean isAllIn() {
        return status.equals("ALLIN");
    }
    public void fold() {
        status = "FOLD";
    }
    

    public String getName() {
        return name;
    }
    
    public int getChips() {
        return chips;
    }
    
    public void setChips(int chips) {
        this.chips = chips;
    }
    
    public int getBetAmount() {
        return betAmount;
    }
    
    public void setBetAmount(int betAmount) {
        this.betAmount = betAmount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public HandModel getHand() {
        return hand;
    }
    
    public void setHand(HandModel hand) {
        this.hand = hand;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setPosition(String role) {
        this.role = role;
    }

}
