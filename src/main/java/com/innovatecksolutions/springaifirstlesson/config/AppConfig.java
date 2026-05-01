package com.innovatecksolutions.springaifirstlesson.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /**
     * Singleton TokenTextSplitter — created once at startup and reused for every
     * document upload. Avoids repeated object allocation on the hot path.
     *
     * Settings:
     *  - 800  tokens per chunk  (good balance of context vs embedding speed)
     *  - 100  token overlap     (preserves continuity between chunks)
     *  - 5    min chunk size    (discard very small fragments)
     *  - 10000 max chunk size   (safety cap)
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter(800, 100, 5, 10000, true);
    }
}
