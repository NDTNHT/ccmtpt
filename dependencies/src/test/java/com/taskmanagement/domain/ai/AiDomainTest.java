package com.taskmanagement.domain.ai;

import com.taskmanagement.domain.task.enums.Priority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AI Domain Objects Unit Tests")
class AiDomainTest {

    @Test
    @DisplayName("AiPromptConfig validates and defaults properly")
    void testPromptConfigDefaults() {
        AiPromptConfig defaultConfig = AiPromptConfig.defaultConfig();
        assertNotNull(defaultConfig.systemPrompt());
        assertEquals("", defaultConfig.additionalInstructions());
        assertEquals(0.3, defaultConfig.temperature());

        // Null or blank falls back to default system prompt
        AiPromptConfig blankConfig = new AiPromptConfig(null, null, null);
        assertEquals(AiPromptConfig.DEFAULT_SYSTEM_PROMPT, blankConfig.systemPrompt());
        assertEquals("", blankConfig.additionalInstructions());
        assertEquals(0.3, blankConfig.temperature());

        // Temperature bounds
        AiPromptConfig outOfBounds = new AiPromptConfig("custom", "instr", 5.0);
        assertEquals(0.3, outOfBounds.temperature());

        // with methods
        AiPromptConfig modified = defaultConfig.withAdditionalInstructions("new instructions")
            .withSystemPrompt("new system prompt");
        assertEquals("new instructions", modified.additionalInstructions());
        assertEquals("new system prompt", modified.systemPrompt());
    }

    @Test
    @DisplayName("AiSubTaskProposal validates title and explanation")
    void testSubTaskProposal() {
        AiSubTaskProposal proposal = new AiSubTaskProposal("Design database schema", "Analyze relations");
        assertEquals("Design database schema", proposal.title());
        assertEquals("Analyze relations", proposal.explanation());

        // Empty title throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> new AiSubTaskProposal("   "));
        assertThrows(NullPointerException.class, () -> new AiSubTaskProposal(null));
    }

    @Test
    @DisplayName("AiTaskBreakdown creates immutable subtasks list")
    void testTaskBreakdown() {
        Instant deadline = Instant.now().plusSeconds(86400);
        AiTaskBreakdown breakdown = new AiTaskBreakdown(
            List.of(new AiSubTaskProposal("Subtask 1")),
            deadline,
            "Standard breakdown"
        );

        assertEquals(1, breakdown.subtasks().size());
        assertEquals(deadline, breakdown.estimatedDeadline());
        assertEquals("Standard breakdown", breakdown.reasoning());

        // Null subtasks handled safely
        AiTaskBreakdown emptyBreakdown = new AiTaskBreakdown(null, null, null);
        assertTrue(emptyBreakdown.subtasks().isEmpty());
        assertEquals("", emptyBreakdown.reasoning());
    }

    @Test
    @DisplayName("AiPriorityProposal validates priority and confidence bounds")
    void testPriorityProposal() {
        AiPriorityProposal proposal = new AiPriorityProposal(Priority.HIGH, "Critical production bug", 1.5);
        assertEquals(Priority.HIGH, proposal.priority());
        assertEquals("Critical production bug", proposal.reasoning());
        assertEquals(1.0, proposal.confidence()); // capped at 1.0

        AiPriorityProposal negative = new AiPriorityProposal(Priority.LOW, "Non urgent", -0.5);
        assertEquals(0.0, negative.confidence()); // floored at 0.0

        assertThrows(NullPointerException.class, () -> new AiPriorityProposal(null, "reason"));
    }

    @Test
    @DisplayName("AiTaskSummary handles nulls gracefully")
    void testTaskSummary() {
        AiTaskSummary summary = new AiTaskSummary("Executive summary", List.of("Point A", "Point B"), "Start step 1");
        assertEquals("Executive summary", summary.summary());
        assertEquals(2, summary.keyHighlights().size());
        assertEquals("Start step 1", summary.suggestedNextStep());

        AiTaskSummary nullSummary = new AiTaskSummary(null, null, null);
        assertEquals("", nullSummary.summary());
        assertTrue(nullSummary.keyHighlights().isEmpty());
        assertEquals("", nullSummary.suggestedNextStep());
    }

    @Test
    @DisplayName("AiCustomProviderConfig validates format, URLs and headers")
    void testCustomProviderConfig() {
        AiCustomProviderConfig openAiConfig = AiCustomProviderConfig.openAi("https://custom-llm.corp.net/v1", "secret-key", "my-model");
        assertEquals("openai", openAiConfig.format());
        assertEquals("https://custom-llm.corp.net/v1", openAiConfig.baseUrl());
        assertEquals("secret-key", openAiConfig.apiKey());
        assertEquals("my-model", openAiConfig.model());
        assertTrue(openAiConfig.isOpenAiFormat());
        assertFalse(openAiConfig.isAnthropicFormat());
        assertTrue(openAiConfig.isConfigured());

        AiCustomProviderConfig anthropicConfig = AiCustomProviderConfig.anthropic("https://api.anthropic.com/v1", "sk-ant-test", "claude-3-5-sonnet");
        assertEquals("anthropic", anthropicConfig.format());
        assertTrue(anthropicConfig.isAnthropicFormat());
        assertFalse(anthropicConfig.isOpenAiFormat());

        AiPromptConfig promptWithCustom = AiPromptConfig.defaultConfig().withCustomProvider(anthropicConfig);
        assertTrue(promptWithCustom.hasCustomProvider());
        assertEquals("custom", promptWithCustom.providerOverride());
        assertEquals("anthropic", promptWithCustom.customProviderConfig().format());
    }
}
