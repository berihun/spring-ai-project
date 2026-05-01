package com.innovatecksolutions.springaifirstlesson.repo;

import com.innovatecksolutions.springaifirstlesson.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    @Query(value = "SELECT *, 1 - (embedding <=> cast(:embedding as vector)) as similarity " +
            "FROM document_chunk ORDER BY embedding <=> cast(:embedding as vector) LIMIT :limit",
            nativeQuery = true)
    List<DocumentChunk> findNearestNeighbors(@Param("embedding") float[] embedding, @Param("limit") int limit);
}