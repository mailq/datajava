import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Consumer;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;

import dev.datastar.Datastar;
import dev.datastar.Datastar.Event;

//IN CASE YOU TRY TO USE THIS CODE IN PRODUCTION, YOU ARE DOOMED!!
//THIS IS A VERY UGLY PROOF OF CONCEPT.
public class nodependency {
    public static void main(String[] args) {
        InetSocketAddress address = new InetSocketAddress(8080);
        Path path = Path.of(".").toAbsolutePath();
        HttpServer server = SimpleFileServer.createFileServer(address, path, SimpleFileServer.OutputLevel.VERBOSE);
        server.createContext("/hello-world", sseResponse(handler -> {
            try {
                var delay = ((Integer) handler.getSignals().get("delay")).intValue();
                var hello = "Hello, world!";
                for (int i = 1; i <= hello.length(); i++) {
                    handler.send(Datastar.patchElements()
                            .replace("<h1 id=\"message\">%s</h1>".formatted(hello.substring(0, i))));
                    Thread.sleep(delay);
                }
            } catch (InterruptedException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }));
        server.start();
        System.out.println("Server started on port 8080");
    }

    public static class ServerSentEventHandler {
        private OutputStream stream;
        private Map<String, Object> signals;

        public ServerSentEventHandler(Map<String, Object> signals, OutputStream stream) {
            this.signals = signals;
            this.stream = stream;
        }

        public Map<String, Object> getSignals() {
            return signals;
        }

        public void send(Event event) throws IOException {
            var sb = new StringBuilder();
            sb.append("event:").append(event.name()).append('\n');
            sb.append("data:").append(event.data()).append('\n');
            if (event.id() != null)
                sb.append("data:").append(event.id()).append('\n');
            if (event.reconnectDelay() != null)
                sb.append("retry:").append(event.reconnectDelay()).append('\n');
            sb.append('\n');
            stream.write(sb.toString().getBytes());
            stream.flush();
        }
    }

    public static HttpHandler sseResponse(Consumer<ServerSentEventHandler> handler) {
        return exchange -> {
            try (exchange) {
                var x = exchange.getRequestURI();
                var query = readQueryParameters(x);
                var json = query.get("datastar");
                var signals = readSignals(json);
                exchange.getRequestBody().readAllBytes();
                if (exchange.getRequestMethod().equals("HEAD")) {
                    exchange.getResponseHeaders().set("Content-Length", Integer.toString(0));
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                    exchange.sendResponseHeaders(200, 0);
                    handler.accept(new ServerSentEventHandler(signals, exchange.getResponseBody()));
                }
            }
        };
    }

    public static Map<String, String> readQueryParameters(URI uri) {
        var query = uri.getRawQuery();
        if (query == null)
            return Map.of();
        var parser = new StringTokenizer(query, "&");
        var result = new HashMap<String, String>();
        while (parser.hasMoreTokens()) {
            var subParser = new StringTokenizer(parser.nextToken(), "=");
            result.put(subParser.nextToken(), URLDecoder.decode(subParser.nextToken(), StandardCharsets.UTF_8));
        }
        return result;
    }

    // THIS DOES NOT, WHAT YOU EXPECT! THIS IS NOT A JSON PARSER.
    public static Map<String, Object> readSignals(String json) {
        if (json == null)
            return Map.of();
        var delay = json.replace("{\"delay\":", "").replace("}", "");
        return Map.of("delay", Integer.valueOf(delay));
    }
}