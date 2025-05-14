package com.example.chainreaction;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private GameBoardView gameBoardView;
    private int selectedPlayers = 0;
    private boolean gameStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Full screen
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );
        showPlayerSelection();
    }

    private void showPlayerSelection() {
        setContentView(R.layout.activity_start);
        Button twoPlayerButton = findViewById(R.id.twoPlayerButton);
        Button threePlayerButton = findViewById(R.id.threePlayerButton);
        Button playGameButton = findViewById(R.id.playGameButton);

        twoPlayerButton.setOnClickListener(v -> {
            selectedPlayers = 2;
            twoPlayerButton.setAlpha(1f);
            threePlayerButton.setAlpha(0.5f);
            playGameButton.setEnabled(true);
        });
        threePlayerButton.setOnClickListener(v -> {
            selectedPlayers = 3;
            threePlayerButton.setAlpha(1f);
            twoPlayerButton.setAlpha(0.5f);
            playGameButton.setEnabled(true);
        });
        playGameButton.setOnClickListener(v -> {
            if (selectedPlayers == 2 || selectedPlayers == 3) {
                startGame(selectedPlayers);
            }
        });
    }

    private void startGame(int numPlayers) {
        setContentView(R.layout.activity_main);
        gameBoardView = findViewById(R.id.gameBoardView);
        gameBoardView.getGameBoard().setNumPlayers(numPlayers);
        gameBoardView.setOnGameStateChangeListener(new GameBoardView.OnGameStateChangeListener() {
            @Override
            public void onGameStateChanged() {
                int currentPlayer = gameBoardView.getGameBoard().getCurrentPlayer();
                gameBoardView.updateGridLineColorForPlayer(currentPlayer);
            }
            @Override
            public void onGameOver(int winner) {
                String message = "Player " + winner + " wins!";
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                gameBoardView.setEnabled(false);
            }
        });
        gameStarted = true;
    }
} 