package com.taskmanagement.adapter.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("JsonExtractor Unit Tests")
class JsonExtractorTest {

    @Test
    @DisplayName("Extracts JSON enclosed in markdown code fences with 'json' identifier")
    void shouldExtractFromMarkdownJsonFence() {
        String raw = "Here is the response:\n```json\n{\"key\": \"value\"}\n```\nHope it helps!";
        String extracted = JsonExtractor.extractJson(raw);
        assertEquals("{\"key\": \"value\"}", extracted);
    }

    @Test
    @DisplayName("Extracts JSON enclosed in generic markdown code fences without language tag")
    void shouldExtractFromGenericMarkdownFence() {
        String raw = "```\n{\"number\": 42}\n```";
        String extracted = JsonExtractor.extractJson(raw);
        assertEquals("{\"number\": 42}", extracted);
    }

    @Test
    @DisplayName("Extracts raw JSON without code blocks")
    void shouldExtractRawJsonObject() {
        String raw = "  {\"status\": \"OK\"}  ";
        String extracted = JsonExtractor.extractJson(raw);
        assertEquals("{\"status\": \"OK\"}", extracted);
    }

    @Test
    @DisplayName("Extracts raw JSON array")
    void shouldExtractRawJsonArray() {
        String raw = "Some text before [1, 2, 3] and text after";
        String extracted = JsonExtractor.extractJson(raw);
        assertEquals("[1, 2, 3]", extracted);
    }

    @Test
    @DisplayName("Extracts JSON when surrounded by conversational text without fences")
    void shouldExtractJsonSurroundedByText() {
        String raw = "Sure! {\"result\": true, \"count\": 5} That was the result.";
        String extracted = JsonExtractor.extractJson(raw);
        assertEquals("{\"result\": true, \"count\": 5}", extracted);
    }

    @Test
    @DisplayName("Handles empty or null input gracefully")
    void shouldHandleEmptyAndNull() {
        assertEquals("{}", JsonExtractor.extractJson(null));
        assertEquals("{}", JsonExtractor.extractJson(""));
        assertEquals("{}", JsonExtractor.extractJson("   "));
    }
}
