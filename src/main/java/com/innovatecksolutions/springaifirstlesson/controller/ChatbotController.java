package com.innovatecksolutions.springaifirstlesson.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatbotController {
    private final ChatClient chatClient;
    private final ChatModel chatModel;
    public ChatbotController(ChatClient.Builder chatClientBuilder, ChatModel chatModel) {
        this.chatClient = chatClientBuilder.build();
        this.chatModel = chatModel;

    }
    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return chatModel.call(question);
    }
    @PostMapping()
    public String chat(@RequestBody String input) {
        // 1. Call the AI and get the content directly
        String message = chatClient.prompt(input)
                .call()
                .content(); // This gets just the text reply from the AI

        // 2. Print the text to your console
        System.out.println("AI Response: " + message);

        // 3. Return the text to the user
        return message;
    }
}
