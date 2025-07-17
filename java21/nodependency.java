import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Locale;
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
                var delay = ((Long) handler.getSignals().get("delay")).intValue();
                var hello = "Hello, world!";
                for (int i = 1; i <= hello.length(); i++) {
                    handler.send(Datastar.patchElements()
                            .replace("<h1 id=\"message\">%s</h1>".formatted(hello.substring(0, i))));
                    Thread.sleep(delay);
                }
            } catch (InterruptedException | IOException | RuntimeException e) {
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
                var uri = exchange.getRequestURI();
                var query = readQueryParameters(uri);
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

    public static Map<String, Object> readSignals(String json) {
        if (json == null)
            return Map.of();
        return new SimpleJsonParser().parse(json);
    }

    // THIS DOES NOT WHAT YOU EXPECT! THIS IS NOT A JSON PARSER.
    // THESE AREN'T THE DROIDS YOU'RE LOOKING FOR
    private static class SimpleJsonParser {
        private int index;
        private String json;

        public Map<String, Object> parse(String jsonInput) throws IllegalArgumentException {
            this.json = jsonInput;
            this.index = 0;
            skipWhitespace();
            if (json.charAt(0) != '{') {
                throw new IllegalArgumentException("Input must start with '{'");
            }
            return parseObject();
        }

        private Map<String, Object> parseObject() {
            var result = new HashMap<String, Object>();
            index++; // Skip opening '{'
            skipWhitespace();

            while (index < json.length() && json.charAt(index) != '}') {
                var key = parseString();
                if (index >= json.length() || json.charAt(index) != ':') {
                    throw new IllegalArgumentException("Expected ':' after key at position " + index);
                }
                index++; // Skip ':'

                Object value;
                if (index < json.length() && json.charAt(index) == '{') {
                    value = parseObject();
                } else if (json.startsWith("null", index)) {
                    value = null;
                    index += 4;
                } else if (json.startsWith("true", index)) {
                    value = Boolean.TRUE;
                    index += 4;
                } else if (json.startsWith("false", index)) {
                    value = Boolean.FALSE;
                    index += 5;
                } else if (json.charAt(index) == '"') {
                    value = parseString();
                } else {
                    value = parseNumber();
                }

                result.put(key, value);

                if (index < json.length() && json.charAt(index) == ',') {
                    index++; // Skip ','
                } else if (index < json.length() && json.charAt(index) != '}') {
                    throw new IllegalArgumentException("Expected ',' or '}' at position " + index);
                }
            }

            if (index >= json.length() || json.charAt(index) != '}') {
                throw new IllegalArgumentException("Expected '}' at position " + index);
            }
            index++; // Skip closing '}'
            return result;
        }

        private String parseString() {
            if (index >= json.length() || json.charAt(index) != '"') {
                throw new IllegalArgumentException("Expected '\"' at position " + index);
            }
            index++; // Skip opening quote

            var sb = new StringBuilder();
            while (index < json.length() && json.charAt(index) != '"') {
                var c = json.charAt(index);
                if (c == '\\') {
                    index++;
                    if (index >= json.length()) {
                        throw new IllegalArgumentException("Unexpected end of input after escape");
                    }
                    c = json.charAt(index);
                    // Handle basic escape sequences
                    switch (c) {
                        case '"':
                        case '\\':
                        case '/':
                            sb.append(c);
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid escape sequence at position " + index);
                    }
                } else {
                    sb.append(c);
                }
                index++;
            }

            if (index >= json.length() || json.charAt(index) != '"') {
                throw new IllegalArgumentException("Expected closing '\"' at position " + index);
            }
            index++; // Skip closing quote
            return sb.toString();
        }

        private Object parseNumber() {
            var sb = new StringBuilder();
            while (index < json.length()) {
                var c = json.charAt(index);
                if (json.charAt(index) == ',' || json.charAt(index) == '}')
                    break;
                sb.append(c);
                index++;
            }
            try {
                return NumberFormat.getInstance(Locale.UK).parseObject( sb.toString());
            } catch (ParseException e) {
                throw new IllegalArgumentException("Expected number at position " + index);
            }
        }

        private void skipWhitespace() {
            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }
    }
}