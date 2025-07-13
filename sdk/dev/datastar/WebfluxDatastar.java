package dev.datastar;

import java.time.Duration;

import org.springframework.http.codec.ServerSentEvent;

import reactor.core.publisher.SynchronousSink;

public class WebfluxDatastar {
    public static void send(SynchronousSink<ServerSentEvent<String>> emitter, dev.datastar.Datastar.Event event) {
        emitter.next(buildSseEvent(event));
    }

    public static ServerSentEvent<String> buildSseEvent(dev.datastar.Datastar.Event event) {
        var sseEvent = ServerSentEvent.builder(event.data());
        sseEvent.event(event.name());
        if (event.id() != null)
            sseEvent.id(event.id());
        if (event.reconnectDelay() != null)
            sseEvent.retry(Duration.ofMillis(event.reconnectDelay().longValue()));
        return sseEvent.build();
    }
}
