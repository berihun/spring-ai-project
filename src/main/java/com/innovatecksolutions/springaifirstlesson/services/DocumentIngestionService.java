package com.innovatecksolutions.springaifirstlesson.services;


import com.innovatecksolutions.springaifirstlesson.entity.KeywordDoc;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.ToTextContentHandler;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
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
    private final VectorStore vectorStore; // Qdrant
    private final ElasticsearchOperations elasticsearchOperations; // Keyword

    public DocumentIngestionService(VectorStore vectorStore, ElasticsearchOperations elasticsearchOperations) {
        this.vectorStore = vectorStore;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public void ingestDocument(MultipartFile file) throws Exception {
        String fullText = extractTextFromPdf(file);
        TokenTextSplitter splitter = new TokenTextSplitter(800, 100, 5, 10000, true);
        List<Document> chunks = splitter.split(List.of(new Document(fullText, Map.of("source", file.getOriginalFilename()))));

        // 1. Save to Qdrant (Semantic)
        vectorStore.add(chunks);

        // 2. Save to Elasticsearch (Keyword)
        for (Document doc : chunks) {
            IndexQuery indexQuery = new IndexQueryBuilder()
                    .withObject(new KeywordDoc(doc.getText(), (String) doc.getMetadata().get("source")))
                    .build();
            elasticsearchOperations.index(indexQuery, IndexCoordinates.of("manuals"));
        }
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