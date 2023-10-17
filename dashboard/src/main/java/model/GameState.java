package model;

public record GameState(GameStatus status) {


    public enum GameStatus {
        off, alive, dead, saved
    }
}
