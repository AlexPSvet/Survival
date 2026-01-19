package com.alexpsvet.gamble.blackjack;

/**
 * Represents a playing card
 */
public class Card {
    private final Suit suit;
    private final Rank rank;
    
    public enum Suit {
        HEARTS("♥", "Coeur"),
        DIAMONDS("♦", "Carreau"),
        CLUBS("♣", "Trèfle"),
        SPADES("♠", "Pique");
        
        private final String symbol;
        private final String name;
        
        Suit(String symbol, String name) {
            this.symbol = symbol;
            this.name = name;
        }
        
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
    }
    
    public enum Rank {
        ACE("A", "As", 11),
        TWO("2", "Deux", 2),
        THREE("3", "Trois", 3),
        FOUR("4", "Quatre", 4),
        FIVE("5", "Cinq", 5),
        SIX("6", "Six", 6),
        SEVEN("7", "Sept", 7),
        EIGHT("8", "Huit", 8),
        NINE("9", "Neuf", 9),
        TEN("10", "Dix", 10),
        JACK("J", "Valet", 10),
        QUEEN("Q", "Dame", 10),
        KING("K", "Roi", 10);
        
        private final String symbol;
        private final String name;
        private final int value;
        
        Rank(String symbol, String name, int value) {
            this.symbol = symbol;
            this.name = name;
            this.value = value;
        }
        
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public int getValue() { return value; }
    }
    
    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }
    
    public Suit getSuit() { return suit; }
    public Rank getRank() { return rank; }
    
    @Override
    public String toString() {
        return rank.getSymbol() + suit.getSymbol();
    }
    
    public String getDisplayName() {
        return rank.getName() + " de " + suit.getName();
    }
}
