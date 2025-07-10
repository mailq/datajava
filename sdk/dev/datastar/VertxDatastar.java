package dev.datastar;

import java.util.stream.Collectors;

import dev.datastar.Datastar.Event;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class VertxDatastar {
  public static void prepareResponse(HttpServerResponse response) {
    response.setChunked(true);
    response.putHeader("Content-Type", "text/event-stream");
    response.putHeader("Connection", "keep-alive");
    response.putHeader("Cache-Control", "no-cache");
  }

  public static void send(HttpServerResponse response, Event event) {
    response.write(build(event));
  }

  public static void sendAndEnd(HttpServerResponse response, Event event) {
    send(response, event);
    response.end();
  }

  private static String build(Event event) {
    var sseEvent = new StringBuilder();
    sseEvent.append("event:").append(event.name()).append('\n');
    if (event.id() != null)
      sseEvent.append("id:").append(event.id()).append('\n');
    if (event.reconnectDelay() != null)
      sseEvent.append("retry:").append(event.reconnectDelay()).append('\n');
    sseEvent.append(event.data().lines().map(data -> "data:" + data).collect(Collectors.joining("\n"))).append("\n\n");
    return sseEvent.toString();
  }

  public static JsonObject readSignals(HttpServerRequest request) {
    if (request.method() == HttpMethod.GET)
      return (JsonObject) Json.decodeValue(request.getParam("datastar"));
    else
      return (JsonObject) Json.decodeValue(request.body().await().toString());
  }
}
