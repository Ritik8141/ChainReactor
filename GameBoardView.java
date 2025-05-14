package com.example.chainreaction;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import androidx.core.content.ContextCompat;

public class GameBoardView extends View {
    private static final String TAG = "GameBoardView";
    private static final int DEFAULT_BOARD_WIDTH = 6;
    private static final int DEFAULT_BOARD_HEIGHT = 12;

    private GameBoard gameBoard;
    private Paint cellPaint;
    private Paint textPaint;
    private float cellWidth;
    private float cellHeight;
    private OnGameStateChangeListener listener;

    // Bitmaps for different orb states
    private Bitmap orbRed1;
    private Bitmap orbRed2;
    private Bitmap orbRed3;
    private Bitmap orbGreen1;
    private Bitmap orbGreen2;
    private Bitmap orbGreen3;
    private Bitmap orbYellow1;
    private Bitmap orbYellow2;
    private Bitmap orbYellow3;

    private List<OrbAnimation> orbAnimations = new ArrayList<>();
    private Random shakeRandom = new Random();
    private static final float SHAKE_INTENSITY = 8f; // pixels
    private static final long ANIMATION_FRAME_DELAY = 16; // ms, ~60fps

    private final List<PendingMove> pendingMoves = new ArrayList<>();
    private boolean animating = false;

    private float globalOrbRotation = 0f;
    private static final float ORB_ROTATION_SPEED = 0.5f; // degrees per frame, adjust for speed

    private static class PendingMove {
        int row, col, playerId;
        PendingMove(int row, int col, int playerId) {
            this.row = row; this.col = col; this.playerId = playerId;
        }
    }

    public interface OnGameStateChangeListener {
        void onGameStateChanged();
        void onGameOver(int winner);
    }

    public interface AnimationEndListener {
        void onAnimationEnd();
    }
    private AnimationEndListener animationEndListener;

    public GameBoardView(Context context) {
        super(context);
        init();
    }

    public GameBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gameBoard = new GameBoard(DEFAULT_BOARD_WIDTH, DEFAULT_BOARD_HEIGHT);

        cellPaint = new Paint();
        cellPaint.setStyle(Paint.Style.STROKE);
        cellPaint.setStrokeWidth(3);
        cellPaint.setColor(Color.BLACK);
        cellPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        setBackgroundColor(Color.WHITE);

        // Load orb bitmaps with better quality
        orbRed1 = BitmapFactory.decodeResource(getResources(), R.drawable.orb_red_1);
        orbRed2 = BitmapFactory.decodeResource(getResources(), R.drawable.orb_red_2);
        orbRed3 = BitmapFactory.decodeResource(getResources(), R.drawable.orb_red_3);
        orbGreen1 = BitmapFactory.decodeResource(getResources(), R.drawable.orb_green_1);
        orbGreen2 = BitmapFactory.decodeResource(getResources(), R.drawable.orb_green_2);
        orbGreen3 = BitmapFactory.decodeResource(getResources(), R.drawable.orb_green_3);
        orbYellow1 = BitmapFactory.decodeResource(getResources(), R.drawable.orb_yellow_1);
        orbYellow2 = BitmapFactory.decodeResource(getResources(), R.drawable.orb_yellow_2);
        orbYellow3 = BitmapFactory.decodeResource(getResources(), R.drawable.orb_yellow_3);

        gameBoard.setOnGameStateChangeListener(new GameBoard.OnGameStateChangeListener() {
            @Override
            public void onGameStateChanged() {
                Log.d(TAG, "Game state changed, invalidating view");
                invalidate();
                if (listener != null) {
                    listener.onGameStateChanged();
                }
            }

            @Override
            public void onGameOver(int winner) {
                Log.d(TAG, "Game over, winner: " + winner);
                if (listener != null) {
                    listener.onGameOver(winner);
                }
            }
        });

        gameBoard.setGameBoardView(this);
    }

    public void setOnGameStateChangeListener(OnGameStateChangeListener listener) {
        this.listener = listener;
        gameBoard.setOnGameStateChangeListener(new GameBoard.OnGameStateChangeListener() {
            @Override
            public void onGameStateChanged() {
                post(() -> {
                    invalidate();
                    if (listener != null) {
                        listener.onGameStateChanged();
                    }
                });
            }

            @Override
            public void onGameOver(int winner) {
                post(() -> {
                    invalidate();
                    if (listener != null) {
                        Log.d("GameBoardView", "Game Over triggered for winner: " + winner);
                        listener.onGameOver(winner);
                    }
                });
            }
        });
    }

    public GameBoard getGameBoard() {
        return gameBoard;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Fill the entire view, even if cells are not perfectly square
        cellWidth = w / (float) gameBoard.getWidth();
        cellHeight = h / (float) gameBoard.getHeight();
        setPadding(0, 0, 0, 0); // Remove any padding
        textPaint.setTextSize(cellHeight * 0.4f);
        Log.d(TAG, "View size changed: " + w + "x" + h + ", cell size: " + cellWidth + "x" + cellHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        boolean hasAnimating = false;
        
        // Update global rotation
        globalOrbRotation += ORB_ROTATION_SPEED;
        if (globalOrbRotation >= 360f) globalOrbRotation -= 360f;
        
        // Draw cells with improved appearance
        for (int row = 0; row < gameBoard.getHeight(); row++) {
            for (int col = 0; col < gameBoard.getWidth(); col++) {
                float left = col * cellWidth;
                float top = row * cellHeight;
                float right = left + cellWidth;
                float bottom = top + cellHeight;
                
                // Draw cell border with rounded corners
                Path path = new Path();
                float radius = Math.min(cellWidth, cellHeight) * 0.1f;
                path.addRoundRect(new RectF(left, top, right, bottom), radius, radius, Path.Direction.CW);
                canvas.drawPath(path, cellPaint);
                
                // Draw cell content
                GameCell cell = gameBoard.getCell(row, col);
                if (cell.getPlayerId() != 0) {
                    Bitmap orbBitmap = getOrbBitmap(cell.getPlayerId(), cell.getOrbs());
                    if (orbBitmap != null) {
                        float shakeX = 0, shakeY = 0;
                        if (cell.getOrbs() == cell.getThreshold() - 1) {
                            shakeX = (shakeRandom.nextFloat() - 0.5f) * SHAKE_INTENSITY;
                            shakeY = (shakeRandom.nextFloat() - 0.5f) * SHAKE_INTENSITY;
                            hasAnimating = true;
                        }
                        
                        float scale = Math.min(cellWidth, cellHeight) * 0.7f / Math.max(orbBitmap.getWidth(), orbBitmap.getHeight());
                        float scaledWidth = orbBitmap.getWidth() * scale;
                        float scaledHeight = orbBitmap.getHeight() * scale;
                        float centerX = left + cellWidth / 2 + shakeX;
                        float centerY = top + cellHeight / 2 + shakeY;
                        
                        canvas.save();
                        canvas.rotate(globalOrbRotation, centerX, centerY);
                        Rect destRect = new Rect(
                            (int) (centerX - scaledWidth / 2),
                            (int) (centerY - scaledHeight / 2),
                            (int) (centerX + scaledWidth / 2),
                            (int) (centerY + scaledHeight / 2)
                        );
                        canvas.drawBitmap(orbBitmap, null, destRect, null);
                        canvas.restore();
                    }
                }
            }
        }
        
        // Draw animating orbs
        Iterator<OrbAnimation> it = orbAnimations.iterator();
        while (it.hasNext()) {
            OrbAnimation anim = it.next();
            if (anim.finished) {
                it.remove();
                continue;
            }
            hasAnimating = true;
            drawAnimatedOrb(canvas, anim);
        }
        
        postInvalidateOnAnimation();
        if (!hasAnimating && animationEndListener != null) {
            animationEndListener.onAnimationEnd();
        }
    }

    private Bitmap getOrbBitmap(int playerId, int orbCount) {
        switch (playerId) {
            case 1:
                switch (orbCount) {
                    case 1: return orbRed1;
                    case 2: return orbRed2;
                    case 3: return orbRed3;
                }
                break;
            case 2:
                switch (orbCount) {
                    case 1: return orbGreen1;
                    case 2: return orbGreen2;
                    case 3: return orbGreen3;
                }
                break;
            case 3:
                switch (orbCount) {
                    case 1: return orbYellow1;
                    case 2: return orbYellow2;
                    case 3: return orbYellow3;
                }
                break;
        }
        return null;
    }

    private void drawAnimatedOrb(Canvas canvas, OrbAnimation anim) {
        float fromX = anim.fromCol * cellWidth + cellWidth / 2;
        float fromY = anim.fromRow * cellHeight + cellHeight / 2;
        float toX = anim.toCol * cellWidth + cellWidth / 2;
        float toY = anim.toRow * cellHeight + cellHeight / 2;
        float x = fromX + (toX - fromX) * anim.progress;
        float y = fromY + (toY - fromY) * anim.progress;
        
        Bitmap orbBitmap = getOrbBitmap(anim.playerId, 1);
        if (orbBitmap != null) {
            float scale = Math.min(cellWidth, cellHeight) * 0.7f / Math.max(orbBitmap.getWidth(), orbBitmap.getHeight());
            float scaledWidth = orbBitmap.getWidth() * scale;
            float scaledHeight = orbBitmap.getHeight() * scale;
            
            canvas.save();
            canvas.rotate(globalOrbRotation, x, y);
            Rect destRect = new Rect(
                (int) (x - scaledWidth / 2),
                (int) (y - scaledHeight / 2),
                (int) (x + scaledWidth / 2),
                (int) (y + scaledHeight / 2)
            );
            canvas.drawBitmap(orbBitmap, null, destRect, null);
            canvas.restore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP || gameBoard.isGameOver()) {
            return true;
        }

        float x = event.getX();
        float y = event.getY();

        // Convert touch coordinates to grid position
        int col = (int) (x / cellWidth);
        int row = (int) (y / cellHeight);

        // Ensure coordinates are within bounds
        if (row >= 0 && row < gameBoard.getHeight() && col >= 0 && col < gameBoard.getWidth()) {
            if (gameBoard.makeMove(row, col)) {
                invalidate();
            }
        }

        return true;
    }

    // Call this to start an orb movement animation
    public void startOrbAnimation(int fromRow, int fromCol, int toRow, int toCol, int playerId, int orbCount) {
        orbAnimations.add(new OrbAnimation(fromRow, fromCol, toRow, toCol, playerId, orbCount));
        animateOrbs();
    }

    // Animation loop
    private void animateOrbs() {
        for (OrbAnimation anim : orbAnimations) {
            if (!anim.finished) {
                anim.update(0.08f); // ~12 frames per second
            }
        }
        invalidate();
    }

    public void setAnimationEndListener(AnimationEndListener listener) {
        this.animationEndListener = listener;
    }

    public void updateGridLineColorForPlayer(int playerId) {
        int color;
        switch (playerId) {
            case 1:
                color = Color.RED;
                break;
            case 2:
                color = Color.GREEN;
                break;
            case 3:
                color = Color.YELLOW;
                break;
            default:
                color = Color.BLACK;
                break;
        }
        cellPaint.setColor(color);
        Log.d(TAG, "Grid line color changed for player: " + playerId);
        invalidate();
    }
} 