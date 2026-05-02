package com.innovatecksolutions.springaifirstlesson.controller;

import com.innovatecksolutions.springaifirstlesson.entity.KeywordDoc;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final VectorStore vectorStore; // Qdrant
    private final ElasticsearchOperations elasticsearch; // Keyword
    private final ChatModel chatModel;
    private final StringRedisTemplate redisTemplate;

    public SearchController(VectorStore vectorStore, ElasticsearchOperations elasticsearch, ChatModel chatModel, StringRedisTemplate redisTemplate) {
        this.vectorStore = vectorStore;
        this.elasticsearch = elasticsearch;
        this.chatModel = chatModel;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Performs a Hybrid Search (Semantic + Keyword) and streams the answer from the AI.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> hybridSearch(@RequestBody String query) {
        String cleanQuery = query.toLowerCase().trim();
        String cacheKey = "hybrid:" + cleanQuery;

        // 1. Redis Cache Check
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            System.out.println("DEBUG: Returning cached response for: " + cleanQuery);
            return Flux.just(cachedValue);
        }

        // 2. Semantic Search (Qdrant)
        List<Document> semanticDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(3)
                        .build()
        );

        // 3. Keyword Search (Elasticsearch) - Using .is() to handle phrases/spaces correctly
        CriteriaQuery keywordQuery = new CriteriaQuery(new Criteria("content").is(query));
        List<SearchHit<KeywordDoc>> keywordDocs = elasticsearch.search(
                keywordQuery,
                KeywordDoc.class,
                IndexCoordinates.of("manuals")
        ).getSearchHits();

        // 4. Combine and Deduplicate Context
        // Using LinkedHashSet to maintain order but remove duplicate text chunks
        Set<String> contextChunks = new LinkedHashSet<>();
        semanticDocs.forEach(doc -> contextChunks.add(doc.getText()));
        keywordDocs.forEach(hit -> contextChunks.add(hit.getContent().content()));

        String context = String.join("\n---\n", contextChunks);

        // 5. Construct Prompt with Strict Instructions
        String systemInstructions = """
            You are a professional Call Center Assistant. 
            Use the following context to answer the user's question.
            
            STRICT RULES:
            1. Answer ONLY using the provided context.
            2. If the answer is not in the context, say "I'm sorry, I don't have information about that in my manuals."
            3. Do not use your own internal knowledge or add outside facts.
            4. Keep the answer concise and professional.
            
            CONTEXT:
            %s
            """.formatted(context);

        Prompt prompt = new Prompt(systemInstructions + "\n\nUser Question: " + query);

        // 6. Stream Response and Cache the Result
        StringBuilder fullResponse = new StringBuilder();

        return chatModel.stream(prompt)
                .map(response -> {
                    String text = response.getResult().getOutput().getText();
                    fullResponse.append(text);
                    return text;
                })
                .doOnComplete(() -> {
                    // Cache the successful response for 1 hour
                    if (fullResponse.length() > 0) {
                        redisTemplate.opsForValue().set(cacheKey, fullResponse.toString(), 1, TimeUnit.HOURS);
                    }
                });
    }

    /**
     * Utility endpoint to wipe the Redis cache if documents change.
     */
    @GetMapping("/clear-cache")
    public String clearCache() {
        try {
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
            return "✅ Redis cache cleared successfully!";
        } catch (Exception e) {
            return "❌ Error clearing cache: " + e.getMessage();
        }
    }
}