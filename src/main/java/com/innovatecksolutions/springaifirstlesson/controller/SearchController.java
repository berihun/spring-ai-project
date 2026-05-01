package com.innovatecksolutions.springaifirstlesson.controller;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final VectorStore vectorStore;
    private final ChatModel chatModel; // This will use Ollama locally

    public SearchController(VectorStore vectorStore, ChatModel chatModel) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamSearch(@RequestBody String query) {
        long startTime = System.currentTimeMillis();
        System.out.println("--- Starting Search for: " + query);

        // 1. Reduce topK to 2 to make it faster
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(2).similarityThreshold(0.5).build());

        System.out.println("--- Vector Search took: " + (System.currentTimeMillis() - startTime) + "ms");

        String context = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // 2. Prepare Prompt
        String systemInstructions = """
            You are a Call Center Assistant. 
            Answer the user's question briefly using the context. 
            If the question is a greeting like 'hello', just greet them back.
            
            CONTEXT: {context}
            """;

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemInstructions);
        var systemMessage = systemPromptTemplate.createMessage(Map.of("context", context));
        var userMessage = new UserMessage(query);

        System.out.println("--- Starting Llama Stream...");

        return chatModel.stream(new Prompt(List.of(systemMessage, userMessage)))
                .map(response -> {
                    String text = response.getResult().getOutput().getText();
                    return text;
                });
    }

//    @PostMapping
//    public SearchResponse search(@RequestBody String query) {
//        // 1. SEARCH: Find the top 4 most relevant snippets from your local PGVector
//        List<Document> similarDocuments = vectorStore.similaritySearch(
//                SearchRequest.builder()
//                        .query(query)
//                        .topK(4)
//                        .similarityThreshold(0.5)
//                        .build());
//
//        // 2. CONTEXT: Combine the snippets into one block of text
//        String context = similarDocuments.stream()
//                .map(Document::getText)
//                .collect(Collectors.joining("\n\n"));
//
//        // 3. GENERATE: Create a prompt for your local Llama/Mistral model
//        String systemInstructions = """
//                You are a Call Center Advisor Assistant.
//                Use the following pieces of extracted context to answer the user's question.
//                If you don't know the answer based on the context, say that you don't know.
//                Provide a concise, step-by-step answer that an advisor can read quickly.
//
//                CONTEXT:
//                {context}
//                """;
//
//        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemInstructions);
//        var systemMessage = systemPromptTemplate.createMessage(Map.of("context", context));
//        var userMessage = new org.springframework.ai.chat.messages.UserMessage(query);
//
//        // 4. CALL LOCAL MODEL: Send to Ollama
//        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
//        String assistantAnswer = chatModel.call(prompt).getResult().getOutput().getText();
//
//        return new SearchResponse(assistantAnswer, similarDocuments);
//    }

    // Response DTO to send both the AI answer and the sources to the UI
    public record SearchResponse(String answer, List<Document> sources) {}
}