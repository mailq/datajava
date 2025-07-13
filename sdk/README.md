# Datastar Java SDK

## Core

Just copy the `Datastar.java` into your project for core functionality. It allows you to create the Datastar events.

> [!IMPORTANT]
> The core does not contain features to handle Datastar signals.
> This is a design choice as it would enforce a dependency to an external JSON library.

Signal parsing is delegated to the end user, as the end user is free to use any JSON library. And probably there is alredy one present on the classpath of the given framework. If not, we don't enforce a library on the end user. Maybe you don't even use signals...

The same goes for session and state management.

* The core is dependency free 
* Only Java JDK 17 features
* Single file
* Fluent builder pattern interface
* No build tool required
* Copy&Paste. No CD/CI or central code registries

## Jakarta EE REST (formerly JAX-RS) integration

If you use JAX-RS according to JSR 311 (from back in 2008), and want to run your code in any Jakarta EE 10 compliant application server then you also copy the `JaxRsDatastar.java` and everything else will be handled by the application server like Wildfly, Jetty, Tomcat, OpenLiberty, TomEE, GlassFish, Payara, JBoss, Microprofile Core, Quarkus, ...

In case of Jakarta EE <10 the imports have to be backported to `javax`.

Example usage:
```java
public class HelloWorldResource {
    @GET
    @Path("/hello-world")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void hello(@QueryParam("datastar") JsonObject signals,
        @Context SseEventSink sse) {
        JaxRsDatastar.send(sse, Datastar.patchElements("""
            <div id="foo">Hello whoever</div>
            """).replaceOuterHtml());
        JaxRsDatastar.send(sse, Datastar.patchSignals("""
            {"foo": 1, "bar": true, "baz": "solid"}
            """).onlyIfMissing().createEvent());
        JaxRsDatastar.send(sse, Datastar.executeScript()
            .withScript("""
            console.log('The server was here');
            """));
        // Must be closed. Otherwise the connection stays
        // open until the client closes the connection.
        sse.close(); 
    }
}
```

Don't want to write JSON signals by hand? Jakarta EE JSON Binding has you covered.
```java
var jsonAsString = Json.createObjectBuilder()
  .add("foo", 1)
  .add("bar", true)
  .add("baz", "solid")
  .build().toString()
```

* Jakarta EE REST compliant. Depends on compliant application server
* Needs JSON (JSON-B or JSON-P) support; should be already included
* Build-in Datastar signals converter
* No build tool required
* Copy&Paste. No CD/CI or central code registries.
* No configuration. "Just works"

## Spring Boot integration

If you are in the poor place to work in Spring, then copy the `SpringDatastar.java`.
Error handling is clunky, and probably should be reworked by a Spring Boot expert. So use it as a first step, and give constructive feedback.

Example usage:
```java
@RestController
public class HelloWorldController {
    @GetMapping(path = "/hello-world", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter hello(@QueryParam("datastar") String json) {
        SseEmitter sse = new SseEmitter();
        SpringDatastar.send(sse, Datastar.patchElements("""
            <div id="foo">Hello whoever</div>
            """).replaceOuterHtml());
        SpringDatastar.send(sse, Datastar.patchSignals("""
            {"foo": 1, "bar": true, "baz": "solid"}
            """).onlyIfMissing().createEvent());
        SpringDatastar.send(sse, Datastar.executeScript()
            .withScript("""
            console.log('The server was here');
            """));
        return sse;
    }
}
```
Maybe. Refer to Spring documentation, or see the spring hello-world example.

* Depends on Spring Boot ecosystem
* Needs Jackson for Json handling of signals
* Your favorite Spring build tool can handle the rest
* Copy&Paste. No CD/CI or central code registries.
* No configuration. "Just works"

## Vert.x integration

If you use Eclipse Vert.x, then copy the `VertxDatastar.java`, too.

Example usage:
```java
    public static void main(String[] args) {
        var vertx = Vertx.vertx();
        var server = vertx.createHttpServer();
        var router = Router.router(vertx);
        router.get("/hello-world").handler(context -> {
            var signals = VertxDatastar.readSignals(context.request());
            var delay = signals.getLong("delay").longValue();

            VertxDatastar.prepareResponse(context.response());
            VertxDatastar.send(context.response(), Datastar.patchElements("""
                <div id="foo">Hello whoever</div>
                """).replaceOuterHtml());
            VertxDatastar.send(context.response(), Datastar.patchSignals("""
                {"foo": 1, "bar": true, "baz": "solid"}
                """).onlyIfMissing().createEvent());
            VertxDatastar.send(context.response(), Datastar.executeScript()
                .withScript("""
                console.log('The server was here');
                """));
            context.response().end();
        });
        server.requestHandler(router).listen(8080);
    }
```

* Depends on Vert.x ecosystem
* Json signal handling out of the box
* Copy&Paste. No CD/CI or central code registries.
* No configuration. "Just works"

## Servlet integration

If you you are on an old school servlet container and want to use Datastar, then copy `ServletDatastar.java`, too.
You can use the provided `DatastarHeaderFilter` in case you want the Datastar headers applied automatically. Otherwise you have to set them with `ServletDatastar.prepareHeaders()` on every request.

* Works on any servlet container
* You have to use the Json library of your choice to read the signals
* Copy&Paste. No CD/CI or central code registries.
* No configuration. "Just works", if you don't forget to set the response headers.

## Micronaut integration
In case you want to develop for Micronaut, you have to copy `MicronautDatastar.java`.

Example usage:
```java
@Controller("/")
public class HelloController {
    @Get(produces = MediaType.TEXT_EVENT_STREAM, uri = "hello-world")
    public Flux<Event<String>> hello() throws IOException {
        return Flux.just(
            MicronautDatastar.buildSseEvent(Datastar.patchElements("""
                <div id="foo">Hello whoever</div>
                """).replaceOuterHtml()),
            MicronautDatastar.buildSseEvent(Datastar.patchSignals("""
                {"foo": 1, "bar": true, "baz": "solid"}
                """).onlyIfMissing().createEvent()),
            MicronautDatastar.buildSseEvent(Datastar.executeScript()
                .withScript("""
                console.log('The server was here');
                """)));
    }
}
```

* Depends on Micronaut reactive ecosystem
* Json signal handling out of the box
* Copy&Paste. No CD/CI or central code registries.
* No configuration. "Just works"

## Javalin integration

> [!CAUTION]
> Javalin is incompatible with Datastar unless the
> [feature request](https://github.com/javalin/javalin/issues/2420) is implemented

> [!IMPORTANT]
> Javalin does not support SSE delay. So be adviced.

**WIP** Work in progress

## JHipster integration

You are doing it wrong. JavaScript to run a Java application? Stop overcomplicating. Think again and use another integration. If you still can't resist, the Spring integration should work with JHipster.

## Other integrations

Not implemented yet. But the conversion from `Datastar.Event` to standard SSE events should be easy. Look at `VertxDatastar.java` for inspiration.

Which integration is missing?