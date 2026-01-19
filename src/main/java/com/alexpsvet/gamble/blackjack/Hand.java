package com.alexpsvet.gamble.blackjack;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a hand of cards in blackjack
 */
public class Hand {
    private final List<Card> cards;
    
    public Hand() {
        this.cards = new ArrayList<>();
    }
    
    /**
     * Add a card to the hand
     */
    public void addCard(Card card) {
        cards.add(card);
    }
    
    /**
     * Get all cards
     */
    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }
    
    /**
     * Calculate hand value
     */
    public int getValue() {
        int value = 0;
        int aces = 0;
        
        for (Card card : cards) {
            if (card.getRank() == Card.Rank.ACE) {
                aces++;
                value += 11;
            } else {
                value += card.getRank().getValue();
            }
        }
        
        // Adjust for aces
        while (value > 21 && aces > 0) {
            value -= 10;
            aces--;
        }
        
        return value;
    }
    
    /**
     * Check if hand is busted (over 21)
     */
    public boolean isBusted() {
        return getValue() > 21;
    }
    
    /**
     * Check if hand is blackjack (21 with 2 cards)
     */
    public boolean isBlackjack() {
        return cards.size() == 2 && getValue() == 21;
    }
    
    /**
     * Clear the hand
     */
    public void clear() {
        cards.clear();
    }
    
    /**
     * Get card count
     */
    public int size() {
        return cards.size();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(cards.get(i).toString());
        }
        sb.append(" (").append(getValue()).append(")");
        return sb.toString();
    }
}
