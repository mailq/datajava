package com.example.datastar;

import java.time.Duration;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import dev.datastar.Datastar;
import dev.datastar.WebfluxDatastar;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/")
public class HelloDatastar {

    @GetMapping("hello-world")
    public Flux<ServerSentEvent<String>> sayHello(@RequestParam("datastar") String raw) throws JsonMappingException, JsonProcessingException {
        var signals = readSignals(raw);
        var hello = "Hello, world!";
        var delay = signals.get("delay").longValue();
        return Flux.range(1, hello.length()).map(i -> hello.substring(0, i))
                .map(text -> "<h1 id=\"message\">%s</h1>".formatted(text))
                .map(element -> Datastar.patchElements().replace(element))
                .map(event -> WebfluxDatastar.buildSseEvent(event))
                .delayElements(Duration.ofMillis(delay));
    }

    public static JsonNode readSignals(String signals) throws JsonMappingException, JsonProcessingException {
        // Needs com.fasterxml.jackson.datatype:jackson-datatype-jakarta-jsonp
        ObjectMapper objectMapper = JsonMapper.builder().build();
        return objectMapper.readTree(signals);
    }
}