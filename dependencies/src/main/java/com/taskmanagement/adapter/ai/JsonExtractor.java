package com.taskmanagement.adapter.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to extract clean JSON payloads from LLM responses that may contain
 * markdown code blocks or conversational preludes/postscripts.
 */
public final class JsonExtractor {
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
        "```(?:json)?\\s*([\\s\\S]*?)\\s*```",
        Pattern.CASE_INSENSITIVE
    );

    private JsonExtractor() {}

    /**
     * Extracts the innermost JSON object or array from the given raw response.
     */
    public static String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        String trimmed = raw.trim();

        // 1. Try markdown code block regex
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            String content = matcher.group(1).trim();
            if (!content.isEmpty()) {
                return content;
            }
        }

        // 2. Look for outermost JSON brackets: '{' ... '}' or '[' ... ']'
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        int firstBracket = trimmed.indexOf('[');
        int lastBracket = trimmed.lastIndexOf(']');

        boolean hasBraces = firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace;
        boolean hasBrackets = firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket;

        if (hasBraces && hasBrackets) {
            // Determine which bracket comes first
            if (firstBrace < firstBracket && lastBrace > lastBracket) {
                return trimmed.substring(firstBrace, lastBrace + 1).trim();
            } else if (firstBracket < firstBrace && lastBracket > lastBrace) {
                return trimmed.substring(firstBracket, lastBracket + 1).trim();
            } else if (firstBrace < firstBracket) {
                return trimmed.substring(firstBrace, lastBrace + 1).trim();
            } else {
                return trimmed.substring(firstBracket, lastBracket + 1).trim();
            }
        } else if (hasBraces) {
            return trimmed.substring(firstBrace, lastBrace + 1).trim();
        } else if (hasBrackets) {
            return trimmed.substring(firstBracket, lastBracket + 1).trim();
        }

        return trimmed;
    }
}
