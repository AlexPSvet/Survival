package com.alexpsvet.gamble.blackjack;

/**
 * Represents a blackjack game session
 */
public class BlackjackGame {
    private final Deck deck;
    private final Hand playerHand;
    private final Hand dealerHand;
    private final double bet;
    private GameState state;
    
    public enum GameState {
        PLAYING,
        PLAYER_BLACKJACK,
        PLAYER_BUSTED,
        DEALER_TURN,
        DEALER_BUSTED,
        PLAYER_WIN,
        DEALER_WIN,
        PUSH
    }
    
    public BlackjackGame(double bet) {
        this.deck = new Deck();
        this.playerHand = new Hand();
        this.dealerHand = new Hand();
        this.bet = bet;
        this.state = GameState.PLAYING;
        
        // Deal initial cards
        playerHand.addCard(deck.draw());
        dealerHand.addCard(deck.draw());
        playerHand.addCard(deck.draw());
        dealerHand.addCard(deck.draw());
        
        // Check for blackjack
        if (playerHand.isBlackjack()) {
            state = GameState.PLAYER_BLACKJACK;
        }
    }
    
    /**
     * Player hits (draws a card)
     */
    public void hit() {
        if (state != GameState.PLAYING) return;
        
        playerHand.addCard(deck.draw());
        
        if (playerHand.isBusted()) {
            state = GameState.PLAYER_BUSTED;
        }
    }
    
    /**
     * Player stands (dealer's turn)
     */
    public void stand() {
        if (state != GameState.PLAYING) return;
        
        state = GameState.DEALER_TURN;
        playDealerTurn();
    }
    
    /**
     * Dealer plays according to rules (hit until 17+)
     */
    private void playDealerTurn() {
        while (dealerHand.getValue() < 17) {
            dealerHand.addCard(deck.draw());
        }
        
        if (dealerHand.isBusted()) {
            state = GameState.DEALER_BUSTED;
        } else {
            // Compare hands
            int playerValue = playerHand.getValue();
            int dealerValue = dealerHand.getValue();
            
            if (playerValue > dealerValue) {
                state = GameState.PLAYER_WIN;
            } else if (dealerValue > playerValue) {
                state = GameState.DEALER_WIN;
            } else {
                state = GameState.PUSH;
            }
        }
    }
    
    /**
     * Calculate winnings based on game state
     */
    public double getWinnings() {
        switch (state) {
            case PLAYER_BLACKJACK:
                return bet * 2.5; // Blackjack pays 3:2
            case PLAYER_WIN:
            case DEALER_BUSTED:
                return bet * 2; // Regular win pays 1:1
            case PUSH:
                return bet; // Push returns bet
            default:
                return 0; // Loss
        }
    }
    
    public Hand getPlayerHand() { return playerHand; }
    public Hand getDealerHand() { return dealerHand; }
    public double getBet() { return bet; }
    public GameState getState() { return state; }
    
    public boolean isGameOver() {
        return state != GameState.PLAYING && state != GameState.DEALER_TURN;
    }
}
