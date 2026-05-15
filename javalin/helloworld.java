//usr/bin/env jbang "$0" "$@" ; exit $?
//SOURCES dev/datastar/*.java
//DEPS io.javalin:javalin:7.2.0
//DEPS org.slf4j:slf4j-simple:2.0.18
//DEPS com.fasterxml.jackson.core:jackson-databind:2.21.3
//JAVAC_OPTIONS -parameters

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import dev.datastar.Datastar;
import dev.datastar.JavalinDatastar;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.staticfiles.Location;

public class helloworld {
    public static void main(String[] args) {
        Javalin.create(
                config -> {
                    config.staticFiles.add(System.getProperty("user.dir"), Location.EXTERNAL);
                    config.routes.sse("/hello-world", sse -> {
                        try (sse) {
                            JsonNode signals = JavalinDatastar.readSignals(
                                    sse.ctx().queryParam("datastar"));
                            var delay = signals.get("delay").asLong();
                            var hello = "Hello, world!";
                            for (var i = 1; i <= hello.length(); i++) {
                                JavalinDatastar.send(sse, Datastar.patchElements()
                                        .replace("""
                                                <div id="message">%s</div>
                                                """.formatted(hello.substring(0, i))));
                                try {
                                    Thread.sleep(delay);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (JsonProcessingException e) {
                            throw new BadRequestResponse();
                        }
                    });
                })
                .start(8080);
    }
}
