package game.model;

public class Card {
    
    private final String suit;  
    private final String rank;

    public Card(String suit, String rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public String getSuit() {
        return suit;
    }
    public String getRank() {
        return rank;
    }
    @Override
    public String toString() {
        return rank + " of " + suit;
    }

    public int getValue() {
        return switch (rank) {
            case "A" -> 14;
            case "K" -> 13;
            case "Q" -> 12;
            case "J" -> 11;
            case "10" -> 10;
            case "9" -> 9;
            case "8" -> 8;
            case "7" -> 7;
            case "6" -> 6;
            case "5" -> 5;
            case "4" -> 4;
            case "3" -> 3;
            case "2" -> 2;
            default -> 0;
        };
    }
    
    public boolean isFaceCard() {
        return rank.equals("J") || rank.equals("Q") 
        || rank.equals("K") || rank.equals("A");
    }

    
    
}
