package com.example.chainreaction;

import android.util.Log;

public class GameCell {
    private static final String TAG = "GameCell";
    private int orbs;
    private int playerId; // 0 for empty, 1 for player 1, 2 for player 2
    private final int threshold;
    private final int row;
    private final int col;
    private int borderColor; // 0 for no border, 1 for red (player 1), 2 for green (player 2)

    public static final int BORDER_NONE = 0;
    public static final int BORDER_PLAYER1 = 1; // Red
    public static final int BORDER_PLAYER2 = 2; // Green
    public static final int BORDER_PLAYER3 = 3; // Yellow

    public GameCell(int row, int col, int boardWidth, int boardHeight) {
        this.row = row;
        this.col = col;
        this.orbs = 0;
        this.playerId = 0;
        this.borderColor = BORDER_NONE;
        
        // Set threshold based on position
        if ((row == 0 && col == 0) || 
            (row == 0 && col == boardWidth - 1) || 
            (row == boardHeight - 1 && col == 0) || 
            (row == boardHeight - 1 && col == boardWidth - 1)) {
            this.threshold = 2; // Corners
        } else if (row == 0 || row == boardHeight - 1 || col == 0 || col == boardWidth - 1) {
            this.threshold = 3; // Edges
        } else {
            this.threshold = 4; // Center
        }
        Log.d(TAG, "Cell created at (" + row + "," + col + ") with threshold " + threshold);
    }

    public int getOrbs() {
        return orbs;
    }

    public int getPlayerId() {
        return playerId;
    }

    public int getThreshold() {
        return threshold;
    }

    public boolean addOrb(int playerId) {
        Log.d(TAG, "Adding orb for Player " + playerId + " at (" + row + "," + col + 
              "). Current state - Player: " + this.playerId + ", Orbs: " + orbs);
        
        // If cell is empty or belongs to the same player
        if (this.playerId == 0 || this.playerId == playerId) {
            this.playerId = playerId;
            this.orbs++;
            Log.d(TAG, "Orb added. New state - Player: " + this.playerId + ", Orbs: " + orbs);
            return this.orbs >= threshold;
        }
        
        // If cell belongs to opponent, capture it and add existing orbs, then add the new orb
        int existingOrbs = this.orbs;
        this.playerId = playerId;
        this.orbs = existingOrbs; // Take over the orbs
        this.orbs++; // Add the new orb
        Log.d(TAG, "Cell captured. New state - Player: " + this.playerId + ", Orbs: " + orbs);
        return this.orbs >= threshold;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int color) {
        this.borderColor = color;
        Log.d(TAG, "Cell at (" + row + "," + col + ") border color set to " + color);
    }

    public void reset() {
        Log.d(TAG, "Resetting cell at (" + row + "," + col + ")");
        this.orbs = 0;
        this.playerId = 0;
        this.borderColor = BORDER_NONE;
    }

    public void explode() {
        Log.d(TAG, "Cell exploding at (" + row + "," + col + ")");
        this.orbs = 0;
        this.playerId = 0;
    }
} 