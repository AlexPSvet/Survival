package com.alexpsvet.gamble.blackjack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a deck of cards
 */
public class Deck {
    private final List<Card> cards;
    
    public Deck() {
        this.cards = new ArrayList<>();
        reset();
    }
    
    /**
     * Reset the deck with all 52 cards and shuffle
     */
    public void reset() {
        cards.clear();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        shuffle();
    }
    
    /**
     * Shuffle the deck
     */
    public void shuffle() {
        Collections.shuffle(cards);
    }
    
    /**
     * Draw a card from the deck
     */
    public Card draw() {
        if (cards.isEmpty()) {
            reset();
        }
        return cards.remove(0);
    }
    
    /**
     * Get remaining cards
     */
    public int size() {
        return cards.size();
    }
}
