package dev.datastar;

import java.io.IOException;
import java.time.Duration;

import io.micronaut.http.sse.Event;
import io.micronaut.json.JsonMapper;
import io.micronaut.json.tree.JsonNode;
import reactor.core.publisher.SynchronousSink;

public class MicronautDatastar {
    public static void send(SynchronousSink<Event<String>> emitter, dev.datastar.Datastar.Event event) {
        emitter.next(buildSseEvent(event));
    }

    public static Event<String> buildSseEvent(dev.datastar.Datastar.Event event) {
        var sseEvent = Event.of(event.data());
        sseEvent.name(event.name());
        if (event.id() != null)
            sseEvent.id(event.id());
        if (event.reconnectDelay() != null)
            sseEvent.retry(Duration.ofMillis(event.reconnectDelay().longValue()));
        return sseEvent;
    }

    public static JsonNode readSignals(String signals) throws IOException {
        return JsonMapper.createDefault().readValue(signals, JsonNode.class);
    }
}