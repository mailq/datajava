package dev.datastar;

import dev.datastar.Datastar.Event;
import io.javalin.http.sse.SseClient;

public class JavalinDatastar {
  public static void send(SseClient client, Event event) {
    // The retry field of the event stream can not be set
    client.sendEvent(event.name(), event.data(), event.id());
  }

  public static void sendAndEnd(SseClient client, Event event) {
    send(client, event);
    client.close();
  }
}
