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
        START_WATCH,
        STOP_WATCH,
        START,
        STOP,
        RUN,
        NEW_RUNNER,
        SAVED,
        DEAD;
    }
}
