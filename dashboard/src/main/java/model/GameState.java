package model;

import java.util.Map;

public record GameState(GameStatus status, Map<String, String> data) {


    public enum GameStatus {
        off, alive, dead, saved
    }
}
