package com.innovatecksolutions.springaifirstlesson.entity;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.Map;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_chunk")
public class DocumentChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String sourceFile;
    @Column(columnDefinition = "TEXT")
    private String chunkText;

    @Column(columnDefinition = "vector(384)")
    private float[] embedding;   // pgvector maps float[]

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    // getters/setters
}