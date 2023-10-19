package model;

public record GameEvent(GameEventType type, String runnerId) {
    public GameEvent(GameEventType type) {
        this(type, null);
    }

    public boolean forAll() {
        return runnerId == null;
    }

    public boolean forRunner(String id) {
        return forAll() || runnerId().equals(id);
    }


    public enum GameEventType {
        START_WATCH("ControlsUpdate"),
        STOP_WATCH("ControlsUpdate"),
        START("GameUpdate"),
        STOP("GameUpdate"),
        RUN("BoardUpdate"),
        NEW_RUNNER("BoardUpdate"),
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
