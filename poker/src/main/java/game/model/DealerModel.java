package game.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspace.Space;

public class DealerModel {

    private Space gameSpace;
    private DeckModel deck;

    // Gemmer kortene som er dealt til hver spiller (for at kunne beregne vinderen)
    private Map<String, List<Card>> playerHoleCards = new HashMap<>();
    private List<Card> communityCards = new ArrayList<>();

    public DealerModel(Space gameSpace, DeckModel deck) {
        this.gameSpace = gameSpace;
        this.deck = deck;
    }

    public void dealCards(List<String> players) throws InterruptedException {
        playerHoleCards.clear();
        communityCards.clear();

        for (String p : players) {
            Card card1 = deck.drawCard();
            Card card2 = deck.drawCard();
            System.out.println("putting cards in gamespace for " + p);
            gameSpace.put("dealtCards",p,card1.getSuit(), card1.getRank(), card2.getSuit(), card2.getRank());
            
            // Gem kortene så vi kan bruge dem til at bestemme vinderen
            List<Card> holeCards = new ArrayList<>();
            holeCards.add(card1);
            holeCards.add(card2);
            playerHoleCards.put(p, holeCards);
        }
    }

    public void dealFlop(List<String> players) {
        System.out.println("dealing 1st three");
        try {
            Card card1 = deck.drawCard();
            Card card2 = deck.drawCard();
            Card card3 = deck.drawCard();
            System.out.println(card1 + ", " + card2 + ", " + card3);

            // Gem community cards
            communityCards.add(card1);
            communityCards.add(card2);
            communityCards.add(card3);

            for (String p : players) {
                gameSpace.put("flop",p, card1.getSuit(), card1.getRank(), card2.getSuit(), card2.getRank(), card3.getSuit(), card3.getRank());
            }
        } catch (InterruptedException e) {
        }
        
    }

    public void dealTurn(List<String> players) throws InterruptedException {
        Card card = deck.drawCard();
        communityCards.add(card);
        for (String p : players) {
            gameSpace.put("turnCard", p, card.getSuit(), card.getRank());
        }
    }

    public void dealRiver(List<String> players) throws InterruptedException {
        Card card = deck.drawCard();
        communityCards.add(card);
        for (String p : players) {
            gameSpace.put("riverCard", p, card.getSuit(), card.getRank());
        }
    }
    
    public int cardsRemaining() {
        return deck.cardsRemaining();
    }

    public List<Card> getPlayerHoleCards(String playerName) {
        return playerHoleCards.getOrDefault(playerName, new ArrayList<>());
    }

    public List<Card> getCommunityCards() {
        return new ArrayList<>(communityCards);
    }

    public void resetForNewHand() {
        playerHoleCards.clear();
        communityCards.clear();
        try {
            deck.reset();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
