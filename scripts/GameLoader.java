///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.3
//DEPS io.vertx:vertx-web-client:4.3.4
//JAVA 17+


import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Command(name = "GameLoader", mixinStandardHelpOptions = true, version = "GameLoader 0.1",
        description = "GameLoader made with jbang")
class GameLoader implements Callable<Integer> {

    private static final Random R = new Random();

    @Parameters(index = "0", description = "The url for the dashboard service", defaultValue = "http://localhost:8079")
    private URL urlDashboard;

    @Parameters(index = "1", description = "The url for the runners service", defaultValue = "http://localhost:8080")
    private URL urlRunners;

    @Option(names = {"-p", "--players"}, description = "The amount of players to assign", defaultValue = "20")
    private int players;

    @Option(names = {"-c", "--clicks"}, description = "How many click each player will trigger", defaultValue = "200")
    private int clicks;

    @Option(names = {"--power"}, description = "Click power", defaultValue = "4")
    private int power;

    @Option(names = {"--delay"}, description = "Delay", defaultValue = "100")
    private int delay;

    @Option(names = {"--rip-factor"}, description = "Rip Facor", defaultValue = "5")
    private int ripPercent = 5;

    private final Random random = new Random();

    public static void main(String... args) {
        int exitCode = new CommandLine(new GameLoader()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...
        load();
        return 0;
    }

    void load() throws InterruptedException {
        boolean ssl = urlRunners.getProtocol().equals("https");
        Vertx vertx = Vertx.vertx();
        WebClient client = WebClient.create(vertx,
                new WebClientOptions().setTrustAll(true).setVerifyHost(false).setMaxPoolSize(500));
        final List<JsonObject> users = Collections.synchronizedList(new ArrayList<>());
        final Set<String> names = new HashSet<>();
        final CountDownLatch latchLogin = new CountDownLatch(players);
        final int portDashboard = urlDashboard.getPort() == -1 ? ssl ? 443 : 80 : urlDashboard.getPort();
        final int portRunners = urlRunners.getPort() == -1 ? ssl ? 443 : 80 : urlRunners.getPort();
        for (int i = 0; i < players; i++) {
            final int index = i;
            client.request(HttpMethod.POST, portDashboard, urlDashboard.getHost(),
                            "/api/game/assign/")
                    .ssl(ssl)
                    .expect(ResponsePredicate.SC_SUCCESS)
                    .send()
                    .onComplete(r -> {
                        if (r.failed()) {
                            r.cause().printStackTrace();
                            return;
                        }
                        try {
                            final JsonObject user = r.result().bodyAsJsonObject();
                            users.add(user);
                            names.add(user.getString("name"));
                            System.out.println("login " + user + " " + index);
                            latchLogin.countDown();
                        } catch (Exception e) {
                            System.out.println(r.result().bodyAsString());
                        }

                    });
        }
        latchLogin.await();
        Thread.sleep(1000);
        System.out.println(users.size() + "users created");
        System.out.println(names.size() + " different names");
        AtomicReference<String> statusRef = new AtomicReference<>("OFF");
        Random random = new Random();
        while (!Objects.equals(statusRef.get(), "GAME_OVER")) {
            client.request(HttpMethod.GET, portDashboard, urlDashboard.getHost(),
                            "/api/game/status")
                    .ssl(ssl)
                    .expect(ResponsePredicate.SC_SUCCESS)
                    .send()
                    .onComplete(r -> {
                        if (r.failed()) {
                            r.cause().printStackTrace();
                            return;
                        }
                        final String status = r.result().bodyAsJson(String.class);
                        System.out.println(status);
                        statusRef.set(status);
                    });

            for (JsonObject user : users) {
                if(statusRef.get().equals("ROCKING") || random.nextInt(100) < ripPercent) {
                    client.request(HttpMethod.POST, portRunners, urlRunners.getHost(),
                                    "/api/run")
                            .expect(ResponsePredicate.SC_SUCCESS)
                            .ssl(ssl)
                            .sendJsonObject(new JsonObject().put("distance", random.nextInt(power)).put("runner", user.getString("id")))
                            .onComplete((r) -> {
                                if (r.failed()) {
                                    r.cause().printStackTrace();
                                }
                            });
                }
            }
            Thread.sleep(delay);

        }
        System.out.println("game started");
    }
}

