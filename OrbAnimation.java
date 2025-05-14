package com.example.chainreaction;

public class OrbAnimation {
    public final int fromRow, fromCol, toRow, toCol, playerId;
    public final int orbCount;
    public float progress; // 0.0 to 1.0
    public boolean finished;

    public OrbAnimation(int fromRow, int fromCol, int toRow, int toCol, int playerId, int orbCount) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.playerId = playerId;
        this.orbCount = orbCount;
        this.progress = 0f;
        this.finished = false;
    }

    public void update(float delta) {
        progress += delta;
        if (progress >= 1f) {
            progress = 1f;
            finished = true;
        }
    }
} 