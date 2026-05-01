package com.innovatecksolutions.springaifirstlesson.services;


import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    private final EmbeddingModel embeddingModel;

    public DocumentService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] embedText(String text) {
        // In your Spring AI version, embed() returns float[]
        return embeddingModel.embed(text);
    }
}