package com.innovatecksolutions.springaifirstlesson.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "manuals")
public record KeywordDoc(
        @Id String id,
        @Field(type = FieldType.Text) String content,
        @Field(type = FieldType.Keyword) String source
) {
    // Custom constructor to make creating objects easier
    public KeywordDoc(String content, String source) {
        this(null, content, source);
    }
}