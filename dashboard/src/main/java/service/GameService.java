package service;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import model.GameEvent;
import model.GameEvent.GameEventType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import qute.RockingDukeExtensions;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static model.GameEvent.GameEventType.DEAD;
import static model.GameEvent.GameEventType.NEW_RUNNER;
import static model.GameEvent.GameEventType.RUN;
import static model.GameEvent.GameEventType.SAVED;
import static model.GameEvent.GameEventType.START;
import static model.GameEvent.GameEventType.START_WATCH;
import static model.GameEvent.GameEventType.STOP;
import static model.GameEvent.GameEventType.STOP_WATCH;
import static model.GameEvent.GameEventType.WARN_START_WATCH;
import static qute.RockingDukeExtensions.randomName;
import static service.GameService.WatchStatus.NOT_WATCHING;
import static service.GameService.WatchStatus.OFF;
import static service.GameService.WatchStatus.WARNING;
import static service.GameService.WatchStatus.WATCHING;

@Singleton
@Named("game")
public class GameService {

    private final AtomicReference<MultiEmitter<? super GameEvent>> eventsEmitter = new AtomicReference<>();

    private final Multi<GameEvent> events = Multi.createFrom()
            .<GameEvent>emitter(this.eventsEmitter::set).broadcast().toAllSubscribers();
    private final Map<String, RunnerState> runners = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> started = new AtomicReference<>();
    private final AtomicReference<Instant> watching = new AtomicReference<>();

    private final AtomicReference<String> rockingDuke = new AtomicReference<>(randomName());

    private final AtomicReference<WatchStatus> watchStatus = new AtomicReference<>(OFF);

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

    public void start() {
        started.set(Instant.now());
        watching.set(null);
        watchStatus.set(NOT_WATCHING);
        emitEvent(START);
        final AtomicLong next = new AtomicLong();
        final Random r = new Random();
        next.set(3);
        Multi.createFrom().ticks().every(Duration.ofMillis(1000))
                .subscribe().with(Unchecked.consumer(t -> {
                    if (!isStarted()) {
                        throw new RuntimeException("Stopped");
                    }
                    if (t > next.get()) {
                        long c;
                        if (isWatching()) {
                            c = r.nextLong(rockMaxDuration - rockMinDuration) + rockMinDuration;
                            stopWatch();
                        } else {
                            c = r.nextLong(watchMaxDuration - watchMinDuration) + watchMinDuration;
                            startWatch();
                        }
                        next.set(c + t);
                        Log.infof("waiting: %ss", c);
                        next.set(c + t);
                    } else if (t > (next.get() - warnWatchDuration) && watchStatus() == NOT_WATCHING) {
                        warnWatch();
                    }
                }), t -> {
                });
    }

    public void stop() {
        started.set(null);
        watching.set(null);
        watchStatus.set(OFF);
        runners.replaceAll((r, v) -> v.runner().initialState());
        emitEvent(STOP);
    }

    public void warnWatch() {
        watchStatus.set(WARNING);
        emitEvent(WARN_START_WATCH);
    }

    public void startWatch() {
        watching.set(Instant.now());
        watchStatus.set(WATCHING);
        rockingDuke.set(randomName());
        emitEvent(START_WATCH);
    }

    public void stopWatch() {
        watching.set(null);
        watchStatus.set(NOT_WATCHING);
        emitEvent(STOP_WATCH);
    }

    private void emitEvent(GameEventType type) {
        this.emitEvent(type, null);
    }

    private void emitEvent(GameEventType type, String runnerId) {
        Log.debugf("game-event: %s -> %s", type, runnerId == null ? "*" : runnerId);
        final MultiEmitter<? super GameEvent> emitter = eventsEmitter.get();
        if (emitter != null) {
            emitter.emit(new GameEvent(type, runnerId, Map.of()));
        }
    }

    public Multi<GameEvent> events() {
        return events;
    }

    public Collection<RunnerState> runners() {
        return runners.values();
    }

    public WatchStatus watchStatus() {
        return watchStatus.get();
    }

    public boolean isWatching() {
        return watching.get() != null;
    }

    public void reset() {
        watchStatus.set(OFF);
        started.set(null);
        runners.clear();
    }

    public Runner newRunner(String prevId) {
        final Runner runner;
        if (prevId != null && runners.containsKey(prevId)) {
            runner = runners.get(prevId).runner();
        } else {
            runner = new Runner(runners.size() + 1);
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
        final Instant watchingValue = watching.get();

        this.runners.compute(runnerId, (i, state) -> {
            final long duration = time - started.get().toEpochMilli();
            if (state == null) {
                return new RunnerState(new Runner(runnerId, runners.size() + 1), distance, time, RunnerState.Status.alive);
            }
            if (distance == 0) {
                return state;
            }
            if (state.dead() || state.saved()) {
                emitEvent(GameEventType.valueOf(state.status.toString()), runnerId);
                return state;
            }
            final boolean isDetected = isDetected(watchingValue, time);
            final int newDist = state.distance() + distance;
            if (isDetected) {
                emitEvent(DEAD, runnerId);
                Log.infof("Runner %s is dead at %s", state.runner().name(), newDist);
                return state.runner().newState(newDist, duration, RunnerState.Status.dead);
            }
            if (newDist >= targetDistance) {
                emitEvent(SAVED, runnerId);
                Log.infof("Runner %s is saved in %sms", state.runner().name(), time);
                return state.runner().newState(newDist, duration, RunnerState.Status.saved);
            }
            emitEvent(RUN, runnerId);
            Log.infof("Runner %s moved to %s", state.runner().name(), newDist);
            return state.runner().newState(newDist, duration, RunnerState.Status.alive);
        });
    }

    public boolean isStarted() {
        return OFF != watchStatus.get();
    }

    private boolean isDetected(Instant watching, long time) {
        if (watching == null) {
            return false;
        }
        return watching.isAfter(Instant.ofEpochMilli(time).minus(timeMarginMillis, ChronoUnit.MILLIS));
    }

    public String getRockingDuke() {
        return rockingDuke.get();
    }

    public record Runner(String id, String name) {
        public Runner(String id, int num) {
            this(id, "#" + num);
        }

        public Runner(String id) {
            this(id, extractIndex(id));
        }

        public Runner(int num) {
            this(shortId(num), num);
        }

        public RunnerState newState(int distance, long duration, RunnerState.Status status) {
            return new RunnerState(this, distance, duration, status);
        }

        public RunnerState initialState() {
            return new RunnerState(this, 0, 0, RunnerState.Status.alive);
        }

    }

    public int indexPercentage(int index) {
        return (int) Math.floor((index + 0.5) * 100 / runners().size());
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

        public enum Status {dead, alive, saved}
    }

    public enum WatchStatus {WATCHING, NOT_WATCHING, WARNING, OFF}
}
