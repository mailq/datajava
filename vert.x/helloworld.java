//usr/bin/env jbang "$0" "$@" ; exit $?
//SOURCES dev/datastar/*.java
//DEPS io.vertx:vertx-web:5.0.1
//JAVAC_OPTIONS -parameters

import dev.datastar.Datastar;
import dev.datastar.VertxDatastar;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

public class helloworld {
    public static void main(String[] args) {
        var vertx = Vertx.vertx();
        var server = vertx.createHttpServer();
        var router = Router.router(vertx);
        router.get("/hello-world").handler(context -> {
            var signals = VertxDatastar.readSignals(context.request());
            var delay = signals.getLong("delay").longValue();

            VertxDatastar.prepareResponse(context.response());
            
            context.vertx().setTimer(delay, tick -> {
                sendSubstring(1, "Hello, world!", delay, context);
            });
        });
        router.route().handler(StaticHandler.create("."));
        server.requestHandler(router).listen(8080);
    }

    private static void sendSubstring(int currentStep, String hello, long delay, RoutingContext context) {
        if (currentStep > hello.length()) {
            context.end();
            return;
        }
        VertxDatastar.send(context.response(), Datastar.patchElements().replace("""
                <div id="message">%s</div>
                """.formatted(hello.substring(0, currentStep))));
        context.vertx().setTimer(delay, id -> {
            sendSubstring(currentStep + 1, hello, delay, context);
        });
    }
}
