package model;

import java.util.Map;

public record GameEvent(GameEventType type, String runnerId, Map<String, String> data) {
    public GameEvent(GameEventType type) {
        this(type, null, Map.of());
    }

    public boolean forAll() {
        return runnerId == null;
    }

    public boolean forRunner(String id) {
        return forAll() || runnerId().equals(id);
    }


    public enum GameEventType {

        WARN_STOP_ROCKING("ControlsUpdate"),
        START_WATCH("ControlsUpdate"),
        STOP_WATCH("ControlsUpdate"),
        RESET("GameUpdate"),
        START("GameUpdate"),
        STOP("GameUpdate"),
        RUN("BoardUpdate"),

        GAME_OVER("GameUpdate"),
        NEW_RUNNER("BoardUpdate"),
        REASSIGN("NoOp"),
        SAVED("BoardUpdate"),
        DEAD("BoardUpdate");

        private final String sseEventName;

        GameEventType(String sseEventName) {
            this.sseEventName = sseEventName;
        }

        public String sseEventName() {
            return sseEventName;
        }
    }
}
