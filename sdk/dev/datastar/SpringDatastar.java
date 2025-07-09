package dev.datastar;

import java.io.IOException;
import java.io.StringReader;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;

import dev.datastar.Datastar.Event;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;

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

    public static JsonObject readSignals(String signals) {
        // Needs com.fasterxml.jackson.datatype:jackson-datatype-jakarta-jsonp
        // and org.glassfish:jakarta.json
        ObjectMapper objectMapper = JsonMapper.builder().addModule(new JSONPModule()).build();
        try {
            return objectMapper.readValue(signals, JsonObject.class);
        } catch (JsonProcessingException e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static JsonObject readSignals2(String signals) {
        // Just needs org.glassfish:jakarta.json
        try (var reader = Json.createReader(new StringReader(signals))) {
            return reader.readObject();
        }
    }

    public static JsonNode readSignals3(String signals) throws JsonMappingException, JsonProcessingException {
        // Needs only Jackson, which is present in Spring
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(signals);
    }
}