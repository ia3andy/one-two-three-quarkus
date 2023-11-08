package rest;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import model.GameEvent.GameEventType;
import model.GameState;
import model.GameState.GameStatus;
import model.RunningEvent;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestStreamElementType;
import service.GameService;
import model.GameEvent;
import service.GameService.Runner;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This defines a REST controller, each method will be available under the "Classname/method" URI by convention
 */
@Path("/api/game")
public class Game {

    private static final Set<GameEventType> RUNNER_EVENTS = Set.of(
            GameEventType.START,
            GameEventType.STOP,
            GameEventType.DEAD,
            GameEventType.SAVED
    );

    @Inject GameService gameService;

    @Incoming("runners-in")
    public void onRunningEvent(RunningEvent e) {
        try {
            gameService.run(e.runner(), e.distance(), e.time());
        } catch (RuntimeException r) {

        }
    }

    @Path("/assign/{id}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Runner newRunner(@RestPath String id) {
        return gameService.newRunner(id);
    }

    @Path("/assign")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Runner newRunner() {
        return gameService.newRunner(null);
    }

    @Path("/start")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void start() {
        gameService.start();
    }

    @Path("/stop")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void stop() {
        gameService.stop();
    }

    @Path("/state/{runnerId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GameState state(@RestPath String runnerId) {
        if (!gameService.isStarted()) {
            return new GameState(GameStatus.off, Map.of());
        }
        final GameService.RunnerState runner = gameService.getRunner(runnerId);
        if (runner == null) {
            return new GameState(GameStatus.off, Map.of());
        }
        return new GameState(GameStatus.valueOf(runner.status().toString()), Map.of());
    }

    @Path("/events/{runnerId}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<GameEvent> events(@RestPath String runnerId) {
        return gameService.events()
                .filter(g -> RUNNER_EVENTS.contains(g.type()))
                .filter(g -> g.forRunner(runnerId));
    }

}