//usr/bin/env jbang "$0" "$@" ; exit $?
//SOURCES dev/datastar/*.java
//DEPS io.quarkus.platform:quarkus-bom:3.24.2@pom
//DEPS io.quarkus:quarkus-rest
//JAVAC_OPTIONS -parameters
//JAVA_OPTIONS -Djava.util.logging.manager=org.jboss.logmanager.LogManager

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;

import dev.datastar.Datastar;
import dev.datastar.JaxRsDatastar;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.SseEventSink;

@ApplicationScoped
@Path("/")
public class helloworld {
    static {
        System.setProperty("quarkus.http.access-log.enabled ", "true");
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Path("hello-world")
    public void helloWorld(@QueryParam("datastar") String raw, @Context SseEventSink sink) throws InterruptedException {
        var signals = readSignals(raw);
        var delay = signals.getJsonNumber("delay").longValue();
        var hello = "Hello, world!";
        for (var i = 1; i <= hello.length(); i++) {
            JaxRsDatastar.send(sink, Datastar
                    .patchElements()
                    .replace("<h1 id=\"message\">%s</h1>".formatted(hello.substring(0, i))));
            Thread.sleep(delay);
        }
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String index() throws IOException {
        // This is not the correct way to serve static files, but it works for
        // hello-world.
        return Files.readString(java.nio.file.Path.of("index.html"));
    }

    private static JsonObject readSignals(String json) {
        try (var reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }
}
