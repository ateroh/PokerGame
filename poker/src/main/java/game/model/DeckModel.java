package game.model;

import java.util.ArrayList;
import java.util.List;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Space;

public class DeckModel {
    
    private Space deckSpace;
    
    private static final String[] SUITS = {"hearts", "diamonds", "clubs", "spades"};
    private static final String[] RANKS = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
    
    // get space from host
    public DeckModel(Space deckSpace) {
        this.deckSpace = deckSpace;
    }
    
    // init deck
    public void initialize() throws InterruptedException {
        System.out.println("Creating deck");
        
        for (String suit : SUITS) {
            for (String rank : RANKS) {
                deckSpace.put("card", suit, rank);
            }
        }

        System.out.println("deck complete");
    }

    
    // draw card
    public Card drawCard() throws InterruptedException {
        Object[] card = deckSpace.get(
            new ActualField("card"),
            new FormalField(String.class),
            new FormalField(String.class)
        );
      
        if (card == null) {
            throw new IllegalStateException("ikke okay");
        }

        String suit = (String) card[1];
        String rank = (String) card[2];

        return new Card(suit, rank);
    }
    
    public int cardsRemaining() {
        int count = 0;
        List<Object[]> cards = new ArrayList<>();
        
        try {
            Object[] card;
            while ((card = deckSpace.getp(
                new ActualField("card"),
                new FormalField(String.class),
                new FormalField(String.class)
            )) != null) {
                cards.add(card);
                count++;
            }
            //put back in
            for (Object[] c : cards) {
                deckSpace.put(c);
            }
        } catch (InterruptedException e) {
        
        }
        return count;
    }
    public boolean isEmpty() {
        return cardsRemaining() == 0;
    }
    
    public void reset() throws InterruptedException {
        while (deckSpace.getp(
            new ActualField("card"),
            new FormalField(String.class),
            new FormalField(String.class)
        ) != null) {
            // bye bye card
        }
        // Reinitialize
        initialize();
    }
}