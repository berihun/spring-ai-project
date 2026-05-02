# Call Advisor Assistant

A Spring Boot and Spring AI application for building a local call center advisor assistant. The app ingests PDF manuals, stores document chunks in Qdrant and Elasticsearch, then answers questions with a hybrid retrieval augmented generation flow.

## What It Does

- Streams direct chat responses from a local Ollama model.
- Uploads PDF manuals and extracts text with Apache Tika.
- Splits manuals into token-based chunks for retrieval.
- Stores semantic vectors in Qdrant.
- Stores searchable text chunks in Elasticsearch.
- Combines semantic search and keyword search before prompting the AI model.
- Caches repeated hybrid search answers in Redis for faster follow-up responses.

## Tech Stack

- Java 21
- Spring Boot 3.3.4
- Spring AI 1.0.1
- Ollama for chat and embeddings
- Qdrant for vector search
- Elasticsearch for keyword search
- Redis for response caching
- Apache Tika for PDF text extraction
- Maven Wrapper

## Prerequisites

Install or run these before starting the application:

- Java 21
- Docker, or local installations of Qdrant, Redis, and Elasticsearch
- Ollama running on `http://localhost:11434`
- Ollama models:
  - `llama3.2`
  - `nomic-embed-text`

Pull the Ollama models:

```bash
ollama pull llama3.2
ollama pull nomic-embed-text
```

## Required Local Services

The default configuration expects these services:

| Service | URL / Port | Purpose |
| --- | --- | --- |
| Ollama | `http://localhost:11434` | Chat and embedding models |
| Qdrant | `localhost:6334` | Vector storage over gRPC |
| Redis | `localhost:6379` | Response cache |
| Elasticsearch | `http://localhost:9200` | Keyword index |

Example Docker commands:

```bash
docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant
docker run -p 6379:6379 redis:latest
docker run -p 9200:9200 -e "discovery.type=single-node" -e "xpack.security.enabled=false" elasticsearch:8.15.0
```

## Configuration

Main settings are in `src/main/resources/application.properties`.

Important defaults:

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2
spring.ai.ollama.embedding.model=nomic-embed-text

spring.ai.vectorstore.qdrant.host=localhost
spring.ai.vectorstore.qdrant.port=6334
spring.ai.vectorstore.qdrant.collection-name=advisor_manuals

spring.data.redis.host=localhost
spring.data.redis.port=6379

spring.elasticsearch.uris=http://localhost:9200
```

The Qdrant collection is created automatically if it does not already exist. It uses cosine distance with vector size `768`, which matches `nomic-embed-text`.

## Run the Application

From the project root:

```bash
.\mvnw.cmd spring-boot:run
```

On Linux or macOS:

```bash
./mvnw spring-boot:run
```

The application starts on the default Spring Boot port:

```text
http://localhost:8080
```

## API Endpoints

### Stream a Direct Chat Response

```http
GET /chat/ask?question={question}
```

Example:

```bash
curl "http://localhost:8080/chat/ask?question=What%20is%20Spring%20AI?"
```

### Stream Chat With POST Body

```http
POST /chat
Content-Type: text/plain
```

Example:

```bash
curl -N -X POST http://localhost:8080/chat \
  -H "Content-Type: text/plain" \
  --data "Explain retrieval augmented generation."
```

### Upload a PDF Manual

```http
POST /api/documents/upload
Content-Type: multipart/form-data
```

Example:

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@manual.pdf"
```

The upload endpoint:

1. Extracts text from the PDF.
2. Splits the text into chunks.
3. Stores chunks in Qdrant for semantic search.
4. Stores chunks in Elasticsearch index `manuals` for keyword search.

### Ask a Question Against Uploaded Manuals

```http
POST /api/search/stream
Content-Type: text/plain
```

Example:

```bash
curl -N -X POST http://localhost:8080/api/search/stream \
  -H "Content-Type: text/plain" \
  --data "What is the refund process?"
```

The search endpoint:

1. Checks Redis for a cached answer.
2. Searches Qdrant for similar chunks.
3. Searches Elasticsearch for keyword matches.
4. Deduplicates the retrieved context.
5. Streams an AI-generated answer based only on the retrieved manual context.
6. Caches the completed answer for one hour.

### Clear Redis Cache

```http
GET /api/search/clear-cache
```

Example:

```bash
curl http://localhost:8080/api/search/clear-cache
```

Use this after changing or re-uploading manuals so old answers are not reused.

## Project Structure

```text
src/main/java/com/innovatecksolutions/springaifirstlesson
+-- config
|   +-- AppConfig.java
|   +-- QdrantConfig.java
+-- controller
|   +-- ChatbotController.java
|   +-- DocumentIngestionController.java
|   +-- SearchController.java
+-- entity
|   +-- KeywordDoc.java
+-- services
|   +-- DocumentIngestionService.java
|   +-- DocumentService.java
+-- SpringAiFirstLessonApplication.java
```

## Development Commands

Run tests:

```bash
.\mvnw.cmd test
```

Build the application:

```bash
.\mvnw.cmd clean package
```

## Notes

- Uploaded files are limited to `20MB` by default.
- `/api/search/stream` allows CORS from `http://localhost:5173`.
- The hybrid search prompt instructs the model to answer only from retrieved context.
- If the answer is not present in uploaded manuals, the assistant should say it does not have that information.
