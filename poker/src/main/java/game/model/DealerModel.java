package game.model;

import java.util.List;

import org.jspace.Space;

public class DealerModel {

    private Space gameSpace;
    private DeckModel deck;

    public DealerModel(Space gameSpace, DeckModel deck) {
        this.gameSpace = gameSpace;
        this.deck = deck;
    }

    public void dealCards(List<String> players) throws InterruptedException {
        for (String p : players) {
            Card card1 = deck.drawCard();
            Card card2 = deck.drawCard();

            gameSpace.put("dealtCards",p,card1.getSuit(), card1.getRank(), card2.getSuit(), card2.getRank());
            
        }
    }

    public void dealFlop() {
        System.out.println("dealing 1st three");
        try {
            Card card1 = deck.drawCard();
            Card card2 = deck.drawCard();
            Card card3 = deck.drawCard();
            
            gameSpace.put("flop", card1.getSuit(), card1.getRank(), card2.getSuit(), card2.getRank(), card3.getSuit(), card3.getRank());
        } catch (InterruptedException e) {
        }
        
    }

    public void postFlopDeal() throws InterruptedException {
        Card card = deck.drawCard();
                
        gameSpace.put("postFlopDeal", card.getSuit(),card.getRank());

    }
    
    public int cardsRemaining() {
        return deck.cardsRemaining();
    }

}
