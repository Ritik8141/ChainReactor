package com.example.chainreaction;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class GameBoard {
    private static final String TAG = "GameBoard";
    private final int width;
    private final int height;
    private final GameCell[][] cells;
    private int currentPlayer;
    private boolean gameOver;
    private OnGameStateChangeListener listener;
    private int moveCount; // Track number of moves made
    private GameBoardView gameBoardView;
    private float globalOrbRotation = 0f;
    private static final float ORB_ROTATION_SPEED = 0.5f; // degrees per frame, adjust for speed
    private int numPlayers = 3; // Default to 3 for backward compatibility
    private int[] playerScores;

    public interface OnGameStateChangeListener {
        void onGameStateChanged();
        void onGameOver(int winner);
    }

    public GameBoard(int width, int height) {
        this.width = width;
        this.height = height;
        this.cells = new GameCell[height][width];
        this.currentPlayer = 1;
        this.gameOver = false;
        this.moveCount = 0;
        this.playerScores = new int[4]; // Index 0 unused, 1-3 for players
        
        // Initialize cells
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                cells[row][col] = new GameCell(row, col, width, height);
            }
        }
        updateBorderColors(); // Set initial border colors
        Log.d(TAG, "GameBoard initialized with Player " + currentPlayer + " starting");
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setOnGameStateChangeListener(OnGameStateChangeListener listener) {
        this.listener = listener;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public GameCell getCell(int row, int col) {
        return cells[row][col];
    }

    public boolean makeMove(int row, int col) {
        Log.d(TAG, "Player " + currentPlayer + " attempting move at (" + row + "," + col + ")");
        
        if (gameOver) {
            Log.d(TAG, "Game is over, move rejected");
            return false;
        }
        
        if (!isValidMove(row, col)) {
            Log.d(TAG, "Invalid move for Player " + currentPlayer);
            return false;
        }

        GameCell cell = cells[row][col];
        Log.d(TAG, "Cell before move - Player: " + cell.getPlayerId() + ", Orbs: " + cell.getOrbs());
        
        boolean willExplode = cell.addOrb(currentPlayer);
        if (willExplode) {
            Log.d(TAG, "Cell exploded, handling chain reaction");
            handleExplosion(row, col);
        }

        moveCount++;
        Log.d(TAG, "Move count: " + moveCount);

        // Update scores and check for game over
        updatePlayerScore();
        checkAndHandleGameOver();

        // Only switch player if game is not over
        if (!gameOver) {
            switchPlayer();
            Log.d(TAG, "Turn switched to Player " + currentPlayer);
        }

        if (listener != null) {
            listener.onGameStateChanged();
        }
        return true;
    }

    private boolean isValidMove(int row, int col) {
        if (row < 0 || row >= height || col < 0 || col >= width) {
            return false;
        }
        GameCell cell = cells[row][col];
        boolean isValid = cell.getPlayerId() == 0 || cell.getPlayerId() == currentPlayer;
        Log.d(TAG, "Move validation for Player " + currentPlayer + " at (" + row + "," + col + "): " + isValid);
        return isValid;
    }

    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < height && col >= 0 && col < width;
    }

    public boolean isGameOver() {
        return gameOver;
    }


    private void handleExplosion(int row, int col) {
        GameCell cell = cells[row][col];
        int playerId = cell.getPlayerId();
        cell.reset();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            if (isValidPosition(newRow, newCol)) {
                GameCell adjacentCell = cells[newRow][newCol];
                if (adjacentCell.addOrb(playerId)) {
                    handleExplosion(newRow, newCol);
                }
            }
        }
    }

    private List<int[]> getAdjacentCells(int row, int col) {
        List<int[]> adjacent = new ArrayList<>();
        if (row > 0) adjacent.add(new int[]{row - 1, col});
        if (row < height - 1) adjacent.add(new int[]{row + 1, col});
        if (col > 0) adjacent.add(new int[]{row, col - 1});
        if (col < width - 1) adjacent.add(new int[]{row, col + 1});
        return adjacent;
    }

    private void updateBorderColors() {
        int borderColor = currentPlayer == 1 ? GameCell.BORDER_PLAYER1 : (currentPlayer == 2 ? GameCell.BORDER_PLAYER2 : GameCell.BORDER_PLAYER3);
        Log.d(TAG, "Updating all cell borders to color " + borderColor + " for Player " + currentPlayer);
        
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                cells[row][col].setBorderColor(borderColor);
            }
        }
    }

    private void switchPlayer() {
        currentPlayer++;
        if (currentPlayer > numPlayers) currentPlayer = 1;
        updateBorderColors();
        Log.d(TAG, "Player switched to " + currentPlayer);
    }

    private void checkAndHandleGameOver() {
        if (moveCount < numPlayers) {
            return;  // Game can't be over before all players have made at least one move
        }

        // Count orbs for each player
        int[] orbCounts = new int[4]; // 0 unused, 1-3 for players
        int playersWithOrbs = 0;
        int lastPlayerWithOrbs = 0;

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                GameCell cell = cells[row][col];
                if (cell.getPlayerId() > 0) {
                    orbCounts[cell.getPlayerId()] += cell.getOrbs();
                }
            }
        }

        // Count players with orbs
        for (int i = 1; i <= numPlayers; i++) {
            if (orbCounts[i] > 0) {
                playersWithOrbs++;
                lastPlayerWithOrbs = i;
            }
        }

        // Check if game is over
        if (playersWithOrbs == 1) {
            gameOver = true;
            Log.d(TAG, "Game Over! Winner: Player " + lastPlayerWithOrbs + " with " + orbCounts[lastPlayerWithOrbs] + " orbs");
            if (listener != null) {
                listener.onGameOver(lastPlayerWithOrbs);
            }
        }
    }

    private boolean checkGameOver() {
        if (moveCount < numPlayers) {
            return false;
        }

        int playersWithOrbs = 0;
        for (int i = 1; i <= numPlayers; i++) {
            if (getPlayerScore(i) > 0) {
                playersWithOrbs++;
            }
        }
        return playersWithOrbs == 1;
    }

    private int getWinner() {
        for (int i = 1; i <= numPlayers; i++) {
            if (getPlayerScore(i) > 0) {
                return i;
            }
        }
        return 0;
    }

    public void reset() {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                cells[row][col].reset();
            }
        }
        currentPlayer = 1;
        gameOver = false;
        moveCount = 0;
        updateBorderColors();
        updatePlayerScore();
        Log.d(TAG, "Game reset, Player 1 starting");
        if (listener != null) {
            listener.onGameStateChanged();
        }
    }

    public void setGameBoardView(GameBoardView view) {
        this.gameBoardView = view;
    }

    public void setNumPlayers(int numPlayers) {
        this.numPlayers = numPlayers;
    }

    public int getNumPlayers() {
        return numPlayers;
    }

    public int getPlayerScore(int playerId) {
        if (playerId >= 1 && playerId <= 3) {
            return playerScores[playerId];
        }
        return 0;
    }

    private void updatePlayerScore() {
        // Reset scores
        for (int i = 1; i <= 3; i++) {
            playerScores[i] = 0;
        }

        // Count cells owned by each player
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                GameCell cell = cells[row][col];
                if (cell.getPlayerId() > 0) {
                    playerScores[cell.getPlayerId()] += cell.getOrbs();
                }
            }
        }
    }
}