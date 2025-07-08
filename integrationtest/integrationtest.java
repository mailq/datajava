//usr/bin/env jbang "$0" "$@" ; exit $?
//SOURCES dev/datastar/*.java
//DEPS io.quarkus.platform:quarkus-bom:3.24.2@pom
//DEPS io.quarkus:quarkus-rest
//JAVAC_OPTIONS -parameters
//JAVA_OPTIONS -Djava.util.logging.manager=org.jboss.logmanager.LogManager

import java.util.LinkedHashMap;

import dev.datastar.Datastar;
import dev.datastar.Datastar.Event;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue.ValueType;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/test")
/**
 * The code you see here is just to fulfill the requirements for the Datastar
 * integration tests.
 * 
 * Do not assume best practices here. THEY AREN'T!! Although it is stated that
 * SSEs are used, these are not long lived connections. See official JAX-RS
 * examples on how to use SSEs the correct way.
 */
public class integrationtest {
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public String getHandler(@QueryParam("datastar") JsonObject signals) {
        return handleSignals(signals);
    }

    @POST
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Consumes(MediaType.APPLICATION_JSON)
    public String postHandler(JsonObject signals) {
        return handleSignals(signals);
    }

    private String handleSignals(JsonObject signals) {
        var inputEvents = signals.getJsonArray("events");
        var output = new StringBuilder();
        for (var inputEvent : inputEvents) {
            var event = (JsonObject) inputEvent;
            output.append(switch (event.getString("type")) {
                case "patchElements" -> patchElements(event);
                case "patchSignals" -> patchSignals(event);
                case "executeScript" -> executeScript(event);
                default -> "NOT IMPLEMENTED YET IN THE JAVA SDK";
            });
        }
        return output.toString();
    }

    private String patchElements(JsonObject signals) {
        var builder = Datastar.patchElements(signals.getString("elements", null));
        if (signals.containsKey("useViewTransition") && signals.getBoolean("useViewTransition"))
            builder.useViewTransition();
        if (signals.containsKey("selector"))
            builder.select(signals.getString("selector"));
        var event = switch (signals.getString("mode", null)) {
            case "append" -> builder.appendToChildren();
            case "remove" -> builder.remove();
            case null -> builder.replaceOuterHtml();
            default -> builder.replaceOuterHtml();
        };
        event = event.withId(signals.getString("eventId", null));
        if (signals.containsKey("retryDuration"))
            event = event.withReconnectDelay(signals.getJsonNumber("retryDuration").longValue());
        return toServerSendEvent(event);
    }

    private String patchSignals(JsonObject signals) {
        String json = signals.getString("signals-raw", null);
        if (json == null)
            json = signals.getJsonObject("signals").toString();
        var builder = Datastar.patchSignals(json);
        if (signals.containsKey("onlyIfMissing") && signals.getBoolean("onlyIfMissing"))
            builder.onlyIfMissing();
        var event = builder.createEvent();
        event = event.withId(signals.getString("eventId", null));
        if (signals.containsKey("retryDuration"))
            event = event.withReconnectDelay(signals.getJsonNumber("retryDuration").longValue());
        return toServerSendEvent(event);
    }

    private String executeScript(JsonObject signals) {
        var builder = Datastar.executeScript();
        var attributes = signals.getJsonObject("attributes");
        if (attributes != null) {
            var map = new LinkedHashMap<String, String>();
            for (var key : attributes.keySet()) {
                var val = attributes.get(key);
                if (val.getValueType() == ValueType.STRING)
                    map.put(key, attributes.getString(key));
                else
                    map.put(key, val.toString());
            }
            builder.withAttributes(map);
        }
        if (signals.containsKey("autoRemove") && !signals.getBoolean("autoRemove"))
            builder.doNotRemove();
        var event = builder.withScript(signals.getString("script"));
        event = event.withId(signals.getString("eventId", null));
        if (signals.containsKey("retryDuration"))
            event = event.withReconnectDelay(signals.getJsonNumber("retryDuration").longValue());
        return toServerSendEvent(event);
    }

    // THIS WORKS. DO NOT USE IN PRODUCTION!
    private String toServerSendEvent(Event event) {
        var result = new StringBuilder();
        result.append("event: ").append(event.name()).append('\n');
        if (event.id() != null)
            result.append("id: ").append(event.id()).append('\n');
        if (event.reconnectDelay() != null)
            result.append("retry: ").append(event.reconnectDelay()).append('\n');
        event.data().lines().map(line -> "data: " + line + '\n').forEach(result::append);
        return result.append("\n").toString();
    }
}
