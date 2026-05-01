package com.innovatecksolutions.springaifirstlesson.services;


import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.ToTextContentHandler;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
@Service
public class DocumentIngestionService {

    private final VectorStore vectorStore;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void ingestDocument(MultipartFile file) throws IOException, TikaException, SAXException {
        String fullText = extractTextFromPdf(file);

        // Use TokenTextSplitter (800 tokens per chunk, 100 token overlap)
        TokenTextSplitter splitter = new TokenTextSplitter(800, 100, 5, 10000, true);

        // Convert text to a Document object then split it
        List<Document> rawDocs = List.of(new Document(fullText, Map.of("source", file.getOriginalFilename())));
        List<Document> chunks = splitter.split(rawDocs);

        // Store in PGVector
        vectorStore.add(chunks);
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException, TikaException, SAXException {
        try (var inputStream = file.getInputStream()) {
            PDFParser parser = new PDFParser();
            ToTextContentHandler handler = new ToTextContentHandler();
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            parser.parse(inputStream, handler, metadata, context);
            return handler.toString();
        }
    }

    private List<String> splitText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start += (chunkSize - overlap);
        }
        return chunks;
    }
}