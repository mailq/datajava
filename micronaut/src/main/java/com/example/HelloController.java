package com.example;

import java.io.IOException;
import java.time.Duration;

import dev.datastar.Datastar;
import dev.datastar.MicronautDatastar;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.sse.Event;
import reactor.core.publisher.Flux;

@Controller("/")
public class HelloController {
    @Get(produces = MediaType.TEXT_EVENT_STREAM, uri = "hello-world")
    public Flux<Event<String>> hello(@QueryValue("datastar") String raw) throws IOException {
        var signals = MicronautDatastar.readSignals(raw);
        var hello = "Hello, world!";
        var delay = signals.get("delay").getLongValue();
        return Flux.range(1, hello.length()).map(i -> hello.substring(0, i))
                .map(text -> "<h1 id=\"message\">%s</h1>".formatted(text))
                .map(element -> Datastar.patchElements(element).replaceOuterHtml())
                .map(event -> MicronautDatastar.buildSseEvent(event))
                .delayElements(Duration.ofMillis(delay));
    }

    @Get(produces = MediaType.TEXT_HTML)
    public String index() throws IOException {
        return new String((ClassLoader.getSystemResourceAsStream("public/index.html")).readAllBytes());
    }
}
