package dev.datastar;

import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.SseEvent;
import jakarta.ws.rs.sse.SseEventSink;
import dev.datastar.Datastar.Event;

@Provider
public class JaxRsDatastar implements ParamConverterProvider {
  public static void send(SseEventSink sink, Event event) {
    sink.send(build(event));
  }

  public static void sendAndClose(SseEventSink sink, Event event) {
    send(sink, event);
    sink.close();
  }

  private static JsonObject readSignals(String json) {
    try (var reader = Json.createReader(new StringReader(json))) {
      return reader.readObject();
    }
  }

  public static OutboundSseEvent build(Event event) {
    return new OutboundSseEvent() {
      @Override
      public boolean isReconnectDelaySet() {
        return event.reconnectDelay() != null;
      }

      @Override
      public long getReconnectDelay() {
        return event.reconnectDelay() == null ? SseEvent.RECONNECT_NOT_SET
            : event.reconnectDelay().longValue();
      }

      @Override
      public String getName() {
        return event.name();
      }

      @Override
      public String getId() {
        return event.id();
      }

      @Override
      public String getComment() {
        return null;
      }

      @Override
      public Class<?> getType() {
        return String.class;
      }

      @Override
      public MediaType getMediaType() {
        return MediaType.TEXT_PLAIN_TYPE;
      }

      @Override
      public Type getGenericType() {
        return String.class;
      }

      @Override
      public String getData() {
        return event.data();
      }
    };
  }

  @Override
  public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType,
      Annotation[] annotations) {
    if (rawType == JsonObject.class)
      return (ParamConverter<T>) new ParamConverter<JsonObject>() {
        @Override
        public JsonObject fromString(String value) {
          return readSignals(value);
        }

        @Override
        public String toString(JsonObject value) {
          return value.toString();
        }
      };
    return null;
  }
}
