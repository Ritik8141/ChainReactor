package com.example.chainreaction;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class GameActivity extends AppCompatActivity {
    private GameBoardView gameBoardView;
    private TextView playerTurnText;
    private TextView playerScoreText;
    private View statusBar;
    private View blurOverlay;
    private View gameOverOverlay;
    private TextView winnerText;
    private Button restartButton;
    private boolean isGameOver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set theme and hide action bar
        setTheme(R.style.Theme_ChainReactor_NoActionBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_game);

        // Initialize views
        gameBoardView = findViewById(R.id.gameBoardView);
        statusBar = findViewById(R.id.statusBar);
        playerTurnText = findViewById(R.id.playerTurnText);
        playerScoreText = findViewById(R.id.playerScoreText);
        blurOverlay = findViewById(R.id.blurOverlay);
        gameOverOverlay = findViewById(R.id.gameOverOverlay);
        winnerText = findViewById(R.id.winnerText);
        restartButton = findViewById(R.id.restartButton);

        // Make sure status bar is visible
        if (statusBar != null) {
            statusBar.setVisibility(View.VISIBLE);
            statusBar.bringToFront();
        }

        // Set up restart button
        restartButton.setOnClickListener(v -> restartGame());

        // Set up game board listener
        gameBoardView.setOnGameStateChangeListener(new GameBoardView.OnGameStateChangeListener() {
            @Override
            public void onGameStateChanged() {
                updateGameStatus();
            }

            @Override
            public void onGameOver(int winner) {
                isGameOver = true;
                showGameOver(winner);
            }
        });

        updateGameStatus();
    }

    private void restartGame() {
        // Hide overlays with animation
        blurOverlay.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    blurOverlay.setVisibility(View.GONE);
                    blurOverlay.setAlpha(1f);
                });
        
        gameOverOverlay.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    gameOverOverlay.setVisibility(View.GONE);
                    gameOverOverlay.setAlpha(1f);
                });

        // Reset game state
        isGameOver = false;
        gameBoardView.getGameBoard().reset();
        updateGameStatus();
    }

    private void showGameOver(int winner) {
        String winnerColor = winner == 1 ? "Red" : 
                           winner == 2 ? "Green" : "Yellow";
        winnerText.setText(winnerColor + " Player Wins!");
        winnerText.setTextColor(ContextCompat.getColor(this,
            winner == 1 ? R.color.player_red :
            winner == 2 ? R.color.player_green :
            R.color.player_yellow));
        
        // Show blur overlay with fade animation
        blurOverlay.setAlpha(0f);
        blurOverlay.setVisibility(View.VISIBLE);
        blurOverlay.animate()
                .alpha(1f)
                .setDuration(300);

        // Show game over overlay with animation
        gameOverOverlay.setVisibility(View.VISIBLE);
        gameOverOverlay.setScaleX(0.8f);
        gameOverOverlay.setScaleY(0.8f);
        gameOverOverlay.setAlpha(0f);
        
        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(
            ObjectAnimator.ofFloat(gameOverOverlay, "scaleX", 0.8f, 1f),
            ObjectAnimator.ofFloat(gameOverOverlay, "scaleY", 0.8f, 1f),
            ObjectAnimator.ofFloat(gameOverOverlay, "alpha", 0f, 1f)
        );
        animSet.setInterpolator(new OvershootInterpolator());
        animSet.setDuration(500);
        animSet.start();
        
        gameOverOverlay.bringToFront();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure status bar remains visible
        if (statusBar != null) {
            statusBar.setVisibility(View.VISIBLE);
            statusBar.bringToFront();
        }
    }

    private void updateGameStatus() {
        GameBoard gameBoard = gameBoardView.getGameBoard();
        int currentPlayer = gameBoard.getCurrentPlayer();
        
        // Update current player text
        String playerColor = currentPlayer == 1 ? "Red" : 
                           currentPlayer == 2 ? "Green" : "Yellow";
        playerTurnText.setText(playerColor + " Player's Turn");
        playerTurnText.setTextColor(ContextCompat.getColor(this,
            currentPlayer == 1 ? R.color.player_red :
            currentPlayer == 2 ? R.color.player_green :
            R.color.player_yellow));

        // Update scores for all players
        StringBuilder scores = new StringBuilder();
        for (int player = 1; player <= gameBoard.getNumPlayers(); player++) {
            String color = player == 1 ? "Red" : 
                         player == 2 ? "Green" : "Yellow";
            int score = gameBoard.getPlayerScore(player);
            scores.append(color).append(": ").append(score);
            if (player < gameBoard.getNumPlayers()) {
                scores.append(" | ");
            }
        }
        playerScoreText.setText(scores.toString());
    }
} 