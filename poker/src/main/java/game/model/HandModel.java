package game.model;

public class HandModel {

    private Card card1;
    private Card card2;

    public HandModel() {
        this.card1 = null;
        this.card2 = null;
    }

    public HandModel(Card card1, Card card2) {
        this.card1 = card1;
        this.card2 = card2;
    }

    public void setCards(Card card1, Card card2) {
        this.card1 = card1;
        this.card2 = card2;
    }

    public Card getCard1() { 
        return card1;
    }
    public void setCard1(Card card1) { 
        this.card1 = card1;
    }
    public Card getCard2() { 
        return card2;
    }
    public void setCard2(Card card2) { 
        this.card2 = card2;
    }

    public boolean hasCards() {
        return card1 != null && card2 != null;
    }
    public Card[] getCards() {
        return new Card[]{card1, card2};
    }
    public void clear() {
        card1 = null;
        card2 = null;
    }

    
}
