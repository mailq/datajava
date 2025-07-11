//usr/bin/env jbang "$0" "$@" ; exit $?
//SOURCES dev/datastar/*.java
//DEPS io.javalin:javalin:6.7.0
//DEPS org.slf4j:slf4j-simple:2.0.16
//DEPS com.fasterxml.jackson.core:jackson-databind:2.19.1
//JAVAC_OPTIONS -parameters

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import dev.datastar.Datastar;
import dev.datastar.JavalinDatastar;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;

public class helloworld {
    // This code should work, but not tested as is not compatible with Datastar
    // REASON: https://github.com/javalin/javalin/issues/2420
    public static void main(String[] args) {
        Javalin.create(
                config -> {
                    config.staticFiles.add(System.getProperty("user.dir"), Location.EXTERNAL);
                }).sse("/hello-world", context -> {
                    try (context) {
                        JsonNode signals = JavalinJackson.defaultMapper().readTree(
                                context.ctx().queryParam("datastar"));
                        var delay = signals.get("delay").asLong();
                        var hello = "Hello, world!";
                        for (int i = 1; i <= hello.length(); i++) {
                            JavalinDatastar.send(context, Datastar.patchElements("""
                                    <div id="message">%s</div>
                                    """.formatted(hello.substring(0, i))).replaceOuterHtml());
                            try {
                                Thread.sleep(delay);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    } catch (JsonProcessingException e) {
                        throw new BadRequestResponse();
                    }
                })
                .start(8080);
    }
}
