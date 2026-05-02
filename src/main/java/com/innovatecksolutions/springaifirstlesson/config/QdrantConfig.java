package com.innovatecksolutions.springaifirstlesson.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ExecutionException;

@Configuration
public class QdrantConfig {

    @Value("${spring.ai.vectorstore.qdrant.host:localhost}")
    private String host;

    @Value("${spring.ai.vectorstore.qdrant.port:6334}")
    private int port;

    @Value("${spring.ai.vectorstore.qdrant.collection-name}")
    private String collectionName;

    @PostConstruct
    public void init() {
        try {
            // Updated connection method
            QdrantClient client = new QdrantClient(
                    QdrantGrpcClient.newBuilder(host, port, false).build()
            );

            // Check if collection exists
            boolean exists = client.listCollectionsAsync().get().contains(collectionName);

            if (!exists) {
                System.out.println("--- Creating Qdrant Collection: " + collectionName + " ---");
                client.createCollectionAsync(collectionName,
                        VectorParams.newBuilder()
                                .setDistance(Distance.Cosine)
                                .setSize(768) // Matches nomic-embed-text
                                .build()
                ).get();
            }
            client.close();
        } catch (Exception e) {
            System.err.println("Failed to initialize Qdrant: " + e.getMessage());
        }
    }
}