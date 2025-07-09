package dev.datastar;

import java.io.IOException;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.datastar.Datastar.Event;

public class SpringDatastar {
    public static boolean send(SseEmitter emitter, Event event) {
        var sseEvent = SseEmitter.event();
        if (event.id() != null)
            sseEvent.id(event.id());
        if (event.reconnectDelay() != null)
            sseEvent.reconnectTime(event.reconnectDelay().longValue());
        sseEvent.name(event.name()).data(event.data());
        try {
            emitter.send(sseEvent);
            return true;
        } catch (IllegalStateException | IOException e) {
            // Client gone? Needs more investigation
            return false;
        }
    }

    public static boolean sendAndComplete(SseEmitter emitter, Event event) {
        var ok = send(emitter, event);
        if (ok)
            emitter.complete();
        return ok;
    }

    public static JsonNode readSignals(String signals) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(signals);
    }
}