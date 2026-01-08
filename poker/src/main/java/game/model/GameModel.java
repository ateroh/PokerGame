package game.model;

import org.jspace.Space;


public class GameModel {
    
    private Space gameSpace;
    private int pot;
    private DealerModel dealer;

    public GameModel(Space gameSpace, DeckModel deck) {
        this.gameSpace = gameSpace;
        // create the dealer for the game
        this.dealer = new DealerModel(gameSpace, deck);
        this.pot = 0;

    }
    //public void canStart


    //public void startNewRound

    //public void preFlopBets

    //public void dealFlopGameModel
        //dealer.java call

    //public void dealCardsOut
        //dealer.java call

    //public void bettingTime

    //public void revealCards

    //getters
    public int getPot() {
        return pot;
    }
    
    /*public int cardsRemaining() { 
        return dealer.cardsRemaining(); 
    }*/


    
}
