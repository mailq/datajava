package com.example.datastar;

import java.time.Duration;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;

import dev.datastar.Datastar;
import dev.datastar.WebfluxDatastar;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/")
public class HelloDatastar {

    @GetMapping("hello-world")
    public Flux<ServerSentEvent<String>> sayHello(@RequestParam("datastar") String raw) {
        var signals = readSignals(raw);
        var hello = "Hello, world!";
        var delay = signals.getJsonNumber("delay").longValue();
        return Flux.range(1, hello.length()).map(i -> hello.substring(0, i))
                .map(text -> "<h1 id=\"message\">%s</h1>".formatted(text))
                .map(element -> Datastar.patchElements(element).replaceOuterHtml())
                .map(event -> WebfluxDatastar.buildSseEvent(event))
                .delayElements(Duration.ofMillis(delay));
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
}