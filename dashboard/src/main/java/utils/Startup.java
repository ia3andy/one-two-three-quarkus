package utils;

import entity.Poll;
import entity.PollAnswer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class Startup {
    @Transactional
    public void start(@Observes StartupEvent evt) {
        // in DEV mode we seed some data
        if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            Poll poll = new Poll("What's your favorite Java framework?")
                    .addAnswer("Spring forever")
                    .addAnswer("I am starting to fall for Quarkus")
                    .addAnswer("I am a Micronaut")
                    .addAnswer("Long live Quarkus");
            poll.persist();
        }
    }
}