package service;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import model.GameEvent;
import model.GameEvent.GameEventType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import utils.NamesUtil;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static io.smallrye.mutiny.helpers.Subscriptions.complete;
import static java.util.Comparator.comparing;
import static model.GameEvent.GameEventType.*;
import static utils.RockingDukeExtensions.randomName;
import static service.GameService.RockingStatus.OFF;
import static service.GameService.RockingStatus.ROCKING;
import static service.GameService.RockingStatus.WARNING;
import static service.GameService.RockingStatus.NOBODY_MOVE;

@Singleton
@Named("game")
public class GameService {

    private final AtomicReference<MultiEmitter<? super GameEvent>> eventsEmitter = new AtomicReference<>();

    private final Multi<GameEvent> events = Multi.createFrom()
            .<GameEvent>emitter(this.eventsEmitter::set).broadcast().toAllSubscribers();

    // State
    private final AtomicInteger gameId = new AtomicInteger();
    private final AtomicInteger runnersCount = new AtomicInteger();
    private final Map<String, RunnerState> runners = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> started = new AtomicReference<>();
    private final AtomicReference<List<Runner>> rank = new AtomicReference<>();
    private final AtomicReference<Instant> noBodyMoveStart = new AtomicReference<>();
    private final AtomicReference<String> rockingDuke = new AtomicReference<>(randomName());
    private final AtomicReference<RockingStatus> rockingStatus = new AtomicReference<>(OFF);

    // Configs

    @ConfigProperty(name = "game.target-distance")
    public int targetDistance;

    @ConfigProperty(name = "game.watch-min-duration", defaultValue = "3")
    public int watchMinDuration;
    @ConfigProperty(name = "game.watch-max-duration", defaultValue = "7")
    public int watchMaxDuration;
    @ConfigProperty(name = "game.rock-min-duration", defaultValue = "4")
    public int rockMinDuration;
    @ConfigProperty(name = "game.rock-max-duration", defaultValue = "11")
    public int rockMaxDuration;

    @ConfigProperty(name = "game.time-margin-millis")
    public int timeMarginMillis;

    @ConfigProperty(name = "game.warn-watch-duration", defaultValue = "1")
    public int warnWatchDuration;

    @ConfigProperty(name = "game.max-duration", defaultValue = "90")
    public int maxDuration;

    @Inject
    ScoreService scoreService;

    public void start() {
        rank.set(null);
        noBodyMoveStart.set(null);
        rockingStatus.set(ROCKING);
        started.set(Instant.now());
        emitEvent(START);
        final AtomicLong next = new AtomicLong();
        final Random r = new Random();
        next.set(3);
        final int currentGameId = gameId.incrementAndGet();

        Multi.createFrom().ticks().every(Duration.ofMillis(1000))
                .select().where(t -> {
                    if (!isStarted() || gameId.get() != currentGameId) {
                        return false;
                    }
                    if (isGameOver()) {
                        var prev = rank.getAndUpdate(l -> {
                            if (l == null) {
                                rockingStatus.set(RockingStatus.GAME_OVER);
                                List<Runner> computedRank = computeRank();
                                Log.infof("Game Over: " + computedRank);
                                return computedRank;
                            }
                            return l;
                        });
                        if (prev == null) {
                            scoreService.persistRank(rank());
                        }
                        if (prev == null || t > next.get()) {
                            emitRank();
                            next.set(5 + t);
                        }
                        return true;
                    }
                    if (t > next.get()) {
                        long c;
                        if (getNoBodyMoveStart()) {
                            c = r.nextLong(rockMaxDuration - rockMinDuration) + rockMinDuration;
                            rockingTime();
                        } else {
                            c = r.nextLong(watchMaxDuration - watchMinDuration) + watchMinDuration;
                            noBodyMoves();
                        }
                        Log.infof("waiting: %ss", c);
                        next.set(c + t);
                    } else if (t > (next.get() - warnWatchDuration) && watchStatus() == ROCKING) {
                        warnRockers();
                    }
                    return true;
                })
                .subscribe().with(Unchecked.consumer(t -> {
                }), t -> {
                    Log.error("Timer error:", t);
                });
    }

    private void emitRank() {
        final List<Runner> rank = rank();
        if (rank == null) {
            return;
        }
        for (int i = 0; i < rank.size(); i++) {
            emitEvent(GameEventType.GAME_OVER, rank.get(i).id, Map.of("rank", String.valueOf(i + 1)));
        }
    }

    public void timeoutGame() {
        for (Map.Entry<String, RunnerState> entry : runners.entrySet()) {
            if (entry.getValue().alive()) {
                final Instant startedTime = started.get();
                final long duration = startedTime != null ? Instant.now().toEpochMilli() - startedTime.toEpochMilli() : 0;
                runners.put(entry.getKey(),
                        entry.getValue().runner().newState(entry.getValue().distance, duration, RunnerState.Status.dead));
                emitEvent(DEAD, entry.getKey());
            }
        }
    }

    public void stop() {
        started.set(null);
        rank.set(null);
        noBodyMoveStart.set(null);
        rockingStatus.set(OFF);
        runners.replaceAll((r, v) -> v.active() ? v.runner().initialState() : v.runner().setInactive());
        emitEvent(STOP);
    }

    public void reset() {
        started.set(null);
        noBodyMoveStart.set(null);
        rockingStatus.set(OFF);
        rank.set(null);
        runners.replaceAll((r, v) -> v.runner().setInactive());
        emitEvent(RESET);
    }

    public void warnRockers() {
        rockingStatus.set(WARNING);
        emitEvent(WARN_STOP_ROCKING);
    }

    public void noBodyMoves() {
        noBodyMoveStart.set(Instant.now());
        rockingStatus.set(NOBODY_MOVE);
        rockingDuke.set(randomName());
        Log.infof("Start Nobody Move: %s", noBodyMoveStart.get().toEpochMilli());
        emitEvent(START_WATCH);
    }

    public void rockingTime() {
        noBodyMoveStart.set(null);
        rockingStatus.set(ROCKING);
        Log.infof("Rocking time: %s", Instant.now().toEpochMilli());
        emitEvent(STOP_WATCH);
    }

    private void emitEvent(GameEventType type) {
        this.emitEvent(type, null);
    }

    private void emitEvent(GameEventType type, String runnerId) {
        emitEvent(type, runnerId, Map.of());
    }

    private void emitEvent(GameEventType type, String runnerId, Map<String, String> data) {
        Log.debugf("game-event: %s -> %s", type, runnerId == null ? "*" : runnerId);
        final MultiEmitter<? super GameEvent> emitter = eventsEmitter.get();
        if (emitter != null) {
            emitter.emit(new GameEvent(type, runnerId, data));
        }
    }

    public Multi<GameEvent> events() {
        return events;
    }

    public Collection<RunnerState> runners() {
        return runners.values();
    }

    public Collection<RunnerState> activeRunners() {
        return runners.values().stream().filter(RunnerState::active).toList();
    }

    public RockingStatus watchStatus() {
        return rockingStatus.get();
    }

    public boolean getNoBodyMoveStart() {
        return noBodyMoveStart.get() != null;
    }

    public Runner newRunner(String prevId) {
        final Runner runner;
        if (prevId != null && runners.containsKey(prevId)) {
            final RunnerState state = runners.get(prevId);
            runner = state.runner();
            if (!state.active()) {
                runners.put(runner.id(), runner.initialState());
            }
        } else {
            runner = new Runner(runnersCount.incrementAndGet());
            runners.put(runner.id(), runner.initialState());
        }

        emitEvent(NEW_RUNNER);
        return runner;
    }

    private static String shortId(int index) {
        return Long.toString(ByteBuffer.wrap(UUID.randomUUID().toString().getBytes()).getLong(), Character.MAX_RADIX) + "-"
               + index;
    }

    private static int extractIndex(String id) {
        return Integer.parseInt(id.split("-")[1]);
    }

    public RunnerState getRunner(String runnerId) {
        return this.runners.get(runnerId);
    }

    public void run(String runnerId, int distance, long time) {
        if (rank.get() != null) {
            return;
        }

        final Instant noBodyMoveStart = this.noBodyMoveStart.get();
        this.runners.compute(runnerId, (i, state) -> {
            final Instant startedTime = started.get();
            final long duration = startedTime != null ? time - startedTime.toEpochMilli() : 0;
            if (state == null) {
                emitEvent(REASSIGN, runnerId);
                return null;
            }
            // inactive user becoming active
            if (!state.active()) {
                emitEvent(NEW_RUNNER);
                return state.runner().initialState();
            }
            // Game is not started
            if (startedTime == null) {
                return state;
            }
            // Timeout
            if (duration > (maxDuration * 1000L) && !state.gameOver()) {
                emitEvent(DEAD, runnerId);
                Log.infof("Runner %s is dead at %s (timeout)", state.runner().name(), state.distance);
                return state.runner().newState(state.distance, maxDuration, RunnerState.Status.dead);
            }
            // Ping
            if (distance == 0) {
                return state;
            }
            // Already over
            if (state.gameOver()) {
                emitEvent(GameEventType.valueOf(state.status.toString()), runnerId);
                return state;
            }
            // Progressing
            final boolean isDetected = isDetected(noBodyMoveStart, time);
            final int newDist = state.distance() + distance;
            if (isDetected) {
                emitEvent(DEAD, runnerId);
                Log.infof("Runner %s is dead after %sm (Time of death: %s)", state.runner().name(), newDist, time);
                return state.runner().newState(newDist, duration, RunnerState.Status.dead);
            }
            if (newDist >= targetDistance) {
                emitEvent(SAVED, runnerId);
                Log.infof("Runner %s is saved in %sms", state.runner().name(), duration);
                return state.runner().newState(targetDistance, duration, RunnerState.Status.saved);
            }
            emitEvent(RUN, runnerId);
            Log.infof("Runner %s moved to %s at %s", state.runner().name(), newDist, time);
            return state.runner().newState(newDist, duration, RunnerState.Status.alive);
        });
    }

    public List<Runner> rank() {
        return rank.get();
    }

    public boolean isGameOver() {
        return rank.get() != null || runners.values().stream().allMatch(RunnerState::gameOver);
    }

    private List<Runner> computeRank() {
        return runners.values().stream()
                .sorted(rankComparator())
                .map(RunnerState::runner)
                .toList();
    }

    public int getRank(String id) {
        final RunnerState state = runners.get(id);
        final List<Runner> rankValue = rank.get();
        if (state == null || rankValue == null) {
            return -1;
        }
        return rankValue.indexOf(state.runner()) + 1;
    }

    static Comparator<RunnerState> rankComparator() {
        return comparing(RunnerState::active).reversed()
                .thenComparing(comparing(RunnerState::saved).reversed())
                .thenComparing(comparing(RunnerState::alive).reversed())
                .thenComparing(comparing(RunnerState::distance).reversed())
                .thenComparing(RunnerState::duration);
    }

    public boolean isStarted() {
        return OFF != rockingStatus.get();
    }

    private boolean isDetected(Instant noBodyMoveStart, long time) {
        if (noBodyMoveStart == null) {
            return false;
        }
        return Instant.ofEpochMilli(time).isAfter(noBodyMoveStart.plus(timeMarginMillis, ChronoUnit.MILLIS));
    }

    public String getRockingDuke() {
        return rockingDuke.get();
    }

    public record Runner(String id, String name) {
        public Runner(String id, int num) {
            this(id, NamesUtil.getNameById(num));
        }

        public Runner(String id) {
            this(id, extractIndex(id));
        }

        public Runner(int num) {
            this(shortId(num), NamesUtil.getNameById(num));
        }

        public RunnerState newState(int distance, long duration, RunnerState.Status status) {
            return new RunnerState(this, distance, duration, status);
        }

        public RunnerState initialState() {
            return new RunnerState(this, 0, 0, RunnerState.Status.alive);
        }

        public RunnerState setInactive() {
            return new RunnerState(this, 0, 0, RunnerState.Status.inactive);
        }

    }

    public int indexPercentage(int index, int max) {
        return (int) Math.floor((index + 0.5) * 100 / max);
    }

    public String boxSize(int max) {
        return max > 40 ? "small" : max > 12 ? "medium" : "large";
    }

    public record RunnerState(Runner runner, int distance, long duration, Status status) {
        public int distancePercentage(int max) {
            return distance * 100 / max;
        }

        public boolean alive() {
            return status == Status.alive;
        }

        public boolean dead() {
            return status == Status.dead;
        }

        public boolean saved() {
            return status == Status.saved;
        }

        public boolean inactive() {
            return status == Status.inactive;
        }

        public boolean gameOver() {
            return !alive();
        }

        public boolean active() {
            return status != Status.inactive;
        }

        public enum Status {dead, alive, saved, inactive}
    }

    public enum RockingStatus {NOBODY_MOVE, ROCKING, WARNING, GAME_OVER, OFF}
}
