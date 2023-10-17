package rest;

import io.quarkus.logging.Log;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.time.Instant;

@Path("/run")
public class Run {

    @Channel("runners-out") Emitter<TimedRunningEvent> runnersOut;

    @Path("/")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void run(RunningEvent e) {
        if (e.distance > 200) {
            throw new IllegalStateException("Ouch this is too much for me to handle!");
        }
        if (e.distance > 0) {
            Log.infof("Run #%s: %s", e.runner, e.distance);
        }
        runnersOut.send(new TimedRunningEvent(e.runner, e.distance, Instant.now().toEpochMilli()));
    }

    public static record TimedRunningEvent(String runner, int distance, long time) {

    }

    public record RunningEvent(String runner, int distance) {

    }

}
