package com.example.datastar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import dev.datastar.Datastar;
import dev.datastar.SpringDatastar;

@RestController
@RequestMapping("/")
public class HelloDatastar {
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @GetMapping(path = "/hello-world", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sayHello(@RequestParam("datastar") String datastar)
            throws JsonMappingException, JsonProcessingException {
        var signals = SpringDatastar.readSignals(datastar);
        SseEmitter emitter = new SseEmitter();
        executorService.execute(() -> {
            try {
                var hello = "Hello, world!";
                for (int i = 0; i <= hello.length(); i++) {
                    var ok = SpringDatastar.send(emitter,
                            Datastar.patchElements("<div id=\"message\">" + hello.substring(0, i) + "</h1>")
                                    .replaceOuterHtml());
                    if (!ok)
                        break;
                    Thread.sleep(signals.get("delay").asLong());
                }
                emitter.complete();
            } catch (InterruptedException e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}