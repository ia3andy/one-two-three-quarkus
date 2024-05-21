package rest;

import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.logging.Log;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import model.GameEvent;
import model.GameEvent.GameEventType;
import org.jboss.resteasy.reactive.RestStreamElementType;
import service.GameService;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This defines a REST controller, each method will be available under the "Classname/method" URI by convention
 */
@Path("/")
public class Dashboard extends HxController {

    @Inject GameService gameService;

    @CheckedTemplate
    public static class Templates {

        public static native TemplateInstance index();

        public static native TemplateInstance index$game();

        public static native TemplateInstance board();

        public static native TemplateInstance controls();
    }

    @Path("/")
    public TemplateInstance index() {
        if (isHxRequest()) {
            return Templates.index$game();
        }
        return Templates.index();
    }

    public TemplateInstance controls() {
        onlyHxRequest();
        Log.debugf("Watcher status: %s", gameService.watchStatus());
        return Templates.controls();
    }

    public TemplateInstance board() {
        onlyHxRequest();
        return Templates.board();
    }

    @POST
    public void startGame() {
        gameService.start();
    }

    @POST
    public void reset() {
        gameService.reset();
    }

    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<OutboundSseEvent> boardEvents(@Context Sse sse) {
        return gameService.events()
                .filter(e -> e.type().sseEventName().equals("BoardUpdate") || e.type().sseEventName().equals("GameUpdate"))
                .group().intoLists()
                .every(Duration.ofMillis(250))
                .map(g -> sse.newEvent("BoardUpdate", compactHtml(Templates.board().render())));
    }

    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<OutboundSseEvent> controlsEvents(@Context Sse sse) {
        return gameService.events()
                .filter(e -> e.type().sseEventName().equals("ControlsUpdate") || e.type().sseEventName().equals("GameUpdate"))
                .map(g -> sse.newEvent("ControlsUpdate", compactHtml(Templates.controls().render())));
    }

    private static String compactHtml(String html) {
        return html.replaceAll("(\\s+|\\v)", " ");
    }

    private String resolveEventData(String eventName) {
       switch (eventName) {
              case "BoardUpdate":
                return Templates.board().render();
              case "ControlsUpdate":
                return Templates.controls().render();
              default:
                return Templates.index$game().render();
       }
    }

    private static String resolveSseEventName(List<GameEvent> g) {
        final Set<String> events = g.stream().map(GameEvent::type).map(GameEventType::sseEventName).filter(Objects::nonNull).collect(Collectors.toSet());
        if (events.size() == 1) {
            return events.iterator().next();
        }
        return "GameUpdate";
    }

    @POST
    public void stopGame() {
        gameService.stop();
    }

}