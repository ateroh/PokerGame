package game.model;

import java.util.ArrayList;
import java.util.List;

// Inspiration herfra: Meget kan optimeres #todo Optimer
// https://www.cs.cornell.edu/courses/cs100/2003su/assignment5/solution/PokerHand.java

public class HandModel implements Comparable<HandModel> {

    private List<Card> cards;
    private int rank;

    public HandModel(List<Card> c) {
        cards = c;
        rank = calculateRank();
    }

    public HandModel(List<Card> communityCards, List<Card> holeCards) {
        cards = new ArrayList<>();
        cards.addAll(communityCards);
        cards.addAll(holeCards);
        rank = calculateRank();
    }

    public int getRank() {
        return rank;
    }

    public List<Card> getCards() {
        return cards;
    }

    public int calculateRank() {
        int temp = 0;

        if (isPair()) temp = 1;
        if (isTwoPair()) temp = 2;
        if (isThreeOfAKind()) temp = 3;
        if (isStraight()) temp = 4;
        if (isFlush()) temp = 5;
        if (isFullHouse()) temp = 6;
        if (isFourOfAKind()) temp = 7;
        if (isStraightFlush()) temp = 8;

        return temp;
    }

    @Override
    public int compareTo(HandModel otherHand) {
        int thisRank = this.getRank();
        int otherRank = otherHand.getRank();

        if (thisRank != otherRank) {
            return thisRank - otherRank;
        }

        // Create two new integer arrays to keep track of the
        // values of the cards in each hand.
        int[] thisCardValues = new int[13];
        for (int i = 0; i < this.cards.size(); i++) {
            thisCardValues[this.cards.get(i).getValue() - 2]++;
        }
        int[] otherCardValues = new int[13];
        for (int i = 0; i < otherHand.cards.size(); i++) {
            otherCardValues[otherHand.cards.get(i).getValue() - 2]++;
        }

        switch (thisRank) {
            case 0: { // two hands with "high card" only
                return this.compareKickers(otherHand);
            }
            case 1: { // two hands each with one pair
                int thisLoc = 12;
                int otherLoc = 12;
                while (thisCardValues[thisLoc] != 2) {
                    thisLoc--;
                }
                while (otherCardValues[otherLoc] != 2) {
                    otherLoc--;
                }
                if (thisLoc > otherLoc) {
                    return 1;
                }
                if (otherLoc > thisLoc) {
                    return -1;
                }
                return this.compareKickers(otherHand);
            }
            case 2: { // two hands each with two pair
                int thisLoc = 12;
                int otherLoc = 12;
                for (int i = 0; i < 2; i++) {
                    while (thisCardValues[thisLoc] != 2) {
                        thisLoc--;
                    }
                    while (otherCardValues[otherLoc] != 2) {
                        otherLoc--;
                    }
                    if (thisLoc > otherLoc) {
                        return 1;
                    }
                    if (otherLoc > thisLoc) {
                        return -1;
                    }
                    thisLoc--;
                    otherLoc--;
                }
                return this.compareKickers(otherHand);
            }
            case 3: { // two hands each with three of a kind
                int thisLoc = 12;
                int otherLoc = 12;
                while (thisCardValues[thisLoc] != 3) {
                    thisLoc--;
                }
                while (otherCardValues[otherLoc] != 3) {
                    otherLoc--;
                }
                if (thisLoc > otherLoc) {
                    return 1;
                }
                if (otherLoc > thisLoc) {
                    return -1;
                }
                return this.compareKickers(otherHand);
            }
            case 4: { // two hands each with a straight
                return this.compareKickers(otherHand);
            }
            case 5: { // two hands each with a flush
                return this.compareKickers(otherHand);
            }
            case 6: { // two hands each with a full house
                int thisLoc = 12;
                int otherLoc = 12;
                while (thisCardValues[thisLoc] != 3) {
                    thisLoc--;
                }
                while (otherCardValues[otherLoc] != 3) {
                    otherLoc--;
                }
                if (thisLoc > otherLoc) {
                    return 1;
                }
                if (otherLoc > thisLoc) {
                    return -1;
                }
                thisLoc = 12;
                otherLoc = 12;
                while (thisCardValues[thisLoc] != 2) {
                    thisLoc--;
                }
                while (otherCardValues[otherLoc] != 2) {
                    otherLoc--;
                }
                if (thisLoc > otherLoc) {
                    return 1;
                }
                if (otherLoc > thisLoc) {
                    return -1;
                }
                return 0;
            }
            case 7: { // two hands each with four of a kind
                int thisLoc = 12;
                int otherLoc = 12;
                while (thisCardValues[thisLoc] != 4) {
                    thisLoc--;
                }
                while (otherCardValues[otherLoc] != 4) {
                    otherLoc--;
                }
                if (thisLoc > otherLoc) {
                    return 1;
                }
                if (otherLoc > thisLoc) {
                    return -1;
                }
                return this.compareKickers(otherHand);
            }
            case 8: { // two hands each with a straight flush
                return this.compareKickers(otherHand);
            }
        }
        return 0;
    }

    private int compareKickers(HandModel otherHand) {
        int[] thisCardValues = new int[13];
        for (int i = 0; i < this.cards.size(); i++) {
            thisCardValues[this.cards.get(i).getValue() - 2]++;
        }
        int[] otherCardValues = new int[13];
        for (int i = 0; i < otherHand.cards.size(); i++) {
            otherCardValues[otherHand.cards.get(i).getValue() - 2]++;
        }

        int thisLoc = 12;
        int otherLoc = 12;
        int numKickers = 5;
        int rank = this.getRank();

        // different kinds of hands have different numbers of kickers:
        if (rank == 1) numKickers = 3;
        if (rank == 2) numKickers = 1;
        if (rank == 3) numKickers = 2;
        if (rank == 6) numKickers = 0;
        if (rank == 7) numKickers = 1;

        for (int i = 0; i < numKickers; i++) {
            while (thisLoc >= 0 && thisCardValues[thisLoc] != 1) {
                thisLoc--;
            }
            while (otherLoc >= 0 && otherCardValues[otherLoc] != 1) {
                otherLoc--;
            }
            if (thisLoc < 0 || otherLoc < 0) break;
            if (thisLoc > otherLoc) {
                return 1;
            }
            if (otherLoc > thisLoc) {
                return -1;
            }
            thisLoc--;
            otherLoc--;
        }
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder temp = new StringBuilder("Card 1: ");
        for (int i = 0; i < cards.size() - 1; i++) {
            temp.append(cards.get(i)).append("\nCard ").append(i + 2).append(": ");
        }
        temp.append(cards.get(cards.size() - 1));
        return temp.toString();
    }

    public boolean hasAce() {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getValue() == 14) {
                return true;
            }
        }
        return false;
    }

    // Returns true if the hand is a flush (and not a straight flush)
    public boolean isFlush() {
        int hearts = 0, diamonds = 0, clubs = 0, spades = 0;
        for (int i = 0; i < cards.size(); i++) {
            switch (cards.get(i).getSuit()) {
                case "hearts" -> hearts++;
                case "diamonds" -> diamonds++;
                case "clubs" -> clubs++;
                case "spades" -> spades++;
            }
        }
        if (hearts >= 5 || diamonds >= 5 || clubs >= 5 || spades >= 5) {
            if (!isStraightFlush()) {
                return true;
            }
        }
        return false;
    }

    // Returns true if hand is a straight (and not a straight flush)
    public boolean isStraight() {
        int[] cardValues = new int[13];
        for (int i = 0; i < cards.size(); i++) {
            cardValues[cards.get(i).getValue() - 2]++;
        }
        int firstValue = -1, inARow = 0, lastValue = -5;
        for (int i = 0; i < cardValues.length; i++) {
            if (firstValue == -1 && cardValues[i] != 0) {
                firstValue = i;
                lastValue = firstValue;
                inARow++;
            }
            if (cardValues[i] != 0 && (i - 1) == lastValue) {
                lastValue = i;
                inARow++;
            }
        }
        if (inARow >= 5) {
            if (!isStraightFlush()) {
                return true;
            }
        }
        // Check for A-2-3-4-5 (wheel)
        if (cardValues[12] >= 1 && cardValues[0] >= 1 && cardValues[1] >= 1
                && cardValues[2] >= 1 && cardValues[3] >= 1) {
            if (!isStraightFlush()) {
                return true;
            }
        }
        return false;
    }

    // Returns true if hand is four of a kind
    public boolean isFourOfAKind() {
        int[] cardValues = new int[13];
        for (int i = 0; i < cards.size(); i++) {
            cardValues[cards.get(i).getValue() - 2]++;
        }
        for (int i = 0; i < cardValues.length; i++) {
            if (cardValues[i] == 4) {
                return true;
            }
        }
        return false;
    }

    // Returns true if hand is three of a kind (and not full house)
    public boolean isThreeOfAKind() {
        int[] cardValues = new int[13];
        for (int i = 0; i < cards.size(); i++) {
            cardValues[cards.get(i).getValue() - 2]++;
        }
        for (int i = 0; i < cardValues.length; i++) {
            if (cardValues[i] == 3) {
                if (!isFullHouse()) {
                    return true;
                }
            }
        }
        return false;
    }

    // Returns true if hand is one pair (not two pair, not 3/4 of a kind, not full house)
    public boolean isPair() {
        int[] cardValues = new int[13];
        for (int i = 0; i < cards.size(); i++) {
            cardValues[cards.get(i).getValue() - 2]++;
        }
        for (int i = 0; i < cardValues.length; i++) {
            if (cardValues[i] == 2) {
                if (!isTwoPair() && !isFullHouse()) {
                    return true;
                }
            }
        }
        return false;
    }

    // Returns true if hand is two pair (not full house)
    public boolean isTwoPair() {
        int[] cardValues = new int[13];
        int numberOfPairs = 0;
        for (int i = 0; i < cards.size(); i++) {
            cardValues[cards.get(i).getValue() - 2]++;
        }
        for (int i = 0; i < cardValues.length; i++) {
            if (cardValues[i] == 2) {
                numberOfPairs++;
            }
        }
        if (numberOfPairs >= 2) {
            return true;
        }
        return false;
    }

    // Returns true if hand is a Full House
    public boolean isFullHouse() {
        int[] cardValues = new int[13];
        boolean hasThreeOfAKind = false;
        boolean hasAPair = false;
        for (int i = 0; i < cards.size(); i++) {
            cardValues[cards.get(i).getValue() - 2]++;
        }
        for (int i = 0; i < cardValues.length; i++) {
            if (cardValues[i] == 3) {
                hasThreeOfAKind = true;
            }
            if (cardValues[i] == 2) {
                hasAPair = true;
            }
        }
        // Handle case with two three of a kinds (counts as full house)
        int threeCount = 0;
        for (int i = 0; i < cardValues.length; i++) {
            if (cardValues[i] >= 3) threeCount++;
        }
        if (threeCount >= 2) return true;

        if (hasAPair && hasThreeOfAKind) {
            return true;
        }
        return false;
    }

    // Returns true if hand is a straight flush
    public boolean isStraightFlush() {
        String[] suits = {"hearts", "diamonds", "clubs", "spades"};

        for (String suit : suits) {
            int[] cardValues = new int[13];
            int suitCount = 0;

            for (int i = 0; i < cards.size(); i++) {
                if (cards.get(i).getSuit().equals(suit)) {
                    cardValues[cards.get(i).getValue() - 2]++;
                    suitCount++;
                }
            }

            if (suitCount < 5) continue;

            int firstValue = -1, inARow = 0, lastValue = -5;
            for (int i = 0; i < cardValues.length; i++) {
                if (firstValue == -1 && cardValues[i] != 0) {
                    firstValue = i;
                    lastValue = firstValue;
                    inARow++;
                }
                if (cardValues[i] != 0 && (i - 1) == lastValue) {
                    lastValue = i;
                    inARow++;
                }
            }
            if (inARow >= 5) {
                return true;
            }
            // Check for A-2-3-4-5 in same suit (wheel)
            if (cardValues[12] >= 1 && cardValues[0] >= 1 && cardValues[1] >= 1
                    && cardValues[2] >= 1 && cardValues[3] >= 1) {
                return true;
            }
        }
        return false;
    }

    public String getHandName() {
        return switch (rank) {
            case 8 -> "Straight Flush";
            case 7 -> "Four of a Kind";
            case 6 -> "Full House";
            case 5 -> "Flush";
            case 4 -> "Straight";
            case 3 -> "Three of a Kind";
            case 2 -> "Two Pair";
            case 1 -> "Pair";
            default -> "High Card";
        };
    }

    public void clear() {
        cards.clear();
        rank = 0;
    }
}
