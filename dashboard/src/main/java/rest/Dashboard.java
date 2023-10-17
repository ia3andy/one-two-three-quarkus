package rest;

import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import org.jboss.resteasy.reactive.RestStreamElementType;
import service.GameService;

import java.time.Duration;

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
    }


    // This overrides the convention and makes this method available at "/renarde"
    @Path("/")
    public TemplateInstance index() {
        if(isHxRequest()) {
            return  Templates.index$game();
        }
        return Templates.index();
    }

    @POST
    public void startGame() {
        gameService.start();
    }

    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<OutboundSseEvent> events(@Context Sse sse) {
        return gameService.events()
                .group().intoLists()
                .every(Duration.ofMillis(200))
                .map(g -> sse.newEvent("game", ""));
    }

    @POST
    public void stopGame() {
        gameService.stop();
    }

}