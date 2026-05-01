package com.innovatecksolutions.springaifirstlesson.controller;

import com.innovatecksolutions.springaifirstlesson.services.DocumentIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentIngestionController {

    private final DocumentIngestionService ingestionService;

    public DocumentIngestionController(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            ingestionService.ingestDocument(file);
            return ResponseEntity.ok("Document ingested successfully: " + file.getOriginalFilename());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}