package dev.datastar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import dev.datastar.Datastar.Event;
import io.javalin.http.sse.SseClient;
import io.javalin.json.JavalinJackson;

public class JavalinDatastar {
  public static void send(SseClient client, Event event) {
    // The retry field of the event stream can not be set
    client.sendEvent(event.name(), event.data(), event.id());
  }

  public static void sendAndEnd(SseClient client, Event event) {
    send(client, event);
    client.close();
  }

  public static JsonNode readSignals(String json) throws JsonMappingException, JsonProcessingException {
    return JavalinJackson.defaultMapper().readTree(
        json);
  }
}
