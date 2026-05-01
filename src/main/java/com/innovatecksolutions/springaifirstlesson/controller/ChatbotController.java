package com.innovatecksolutions.springaifirstlesson.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatbotController {

    private final ChatClient chatClient;
    private final ChatModel chatModel;

    public ChatbotController(ChatClient.Builder chatClientBuilder, ChatModel chatModel) {
        this.chatClient = chatClientBuilder.build();
        this.chatModel = chatModel;
    }

    /**
     * Streaming GET endpoint — tokens arrive to the client as they are generated,
     * so the user sees output immediately instead of waiting for the full response.
     */
    @GetMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> ask(@RequestParam String question) {
        return chatModel.stream(new Prompt(List.of(new UserMessage(question))))
                .map(response -> response.getResult().getOutput().getText());
    }

    /**
     * Streaming POST endpoint — same streaming behaviour for body-based requests.
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody String input) {
        return chatClient.prompt(input)
                .stream()
                .content()
                .doOnNext(token -> System.out.print(token))
                .doOnComplete(() -> System.out.println("\n[Stream complete]"));
    }
}
