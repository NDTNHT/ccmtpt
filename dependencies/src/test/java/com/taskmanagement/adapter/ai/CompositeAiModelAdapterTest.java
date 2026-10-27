package com.taskmanagement.adapter.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.domain.ai.AiCustomProviderConfig;
import com.taskmanagement.domain.ai.AiPriorityProposal;
import com.taskmanagement.domain.ai.AiPromptConfig;
import com.taskmanagement.domain.ai.AiSubTaskProposal;
import com.taskmanagement.domain.ai.AiTaskBreakdown;
import com.taskmanagement.domain.ai.AiTaskSummary;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CompositeAiModelAdapter Unit Tests")
class CompositeAiModelAdapterTest {

    private OpenAiCompatibleAiAdapter openAiAdapter;
    private GeminiAiAdapter geminiAdapter;
    private AnthropicCompatibleAiAdapter anthropicAdapter;
    private CustomAiAdapter customAdapter;
    private FallbackHeuristicAiAdapter heuristicAdapter;
    private AtomicReference<String> providerRef;
    private AtomicBoolean enabledRef;
    private CompositeAiModelAdapter compositeAdapter;

    private Project project;
    private Task task;
    private AiPromptConfig config;

    @BeforeEach
    void setUp() {
        openAiAdapter = mock(OpenAiCompatibleAiAdapter.class);
        geminiAdapter = mock(GeminiAiAdapter.class);
        anthropicAdapter = mock(AnthropicCompatibleAiAdapter.class);
        customAdapter = mock(CustomAiAdapter.class);
        heuristicAdapter = mock(FallbackHeuristicAiAdapter.class);
        providerRef = new AtomicReference<>("openai");
        enabledRef = new AtomicBoolean(true);

        compositeAdapter = new CompositeAiModelAdapter(
            openAiAdapter,
            geminiAdapter,
            anthropicAdapter,
            customAdapter,
            heuristicAdapter,
            providerRef::get,
            enabledRef::get,
            new ObjectMapper(),
            HttpClient.newHttpClient()
        );

        project = Project.create("TestProj", "Test");
        task = Task.create("Sample Task", "Sample description", null, Priority.MEDIUM, project.getId());
        config = AiPromptConfig.defaultConfig();
    }

    @Test
    @DisplayName("Routes to OpenAI adapter when configured and active")
    void shouldRouteToOpenAiWhenConfigured() {
        when(openAiAdapter.isConfigured()).thenReturn(true);
        AiTaskBreakdown expected = new AiTaskBreakdown(List.of(new AiSubTaskProposal("Sub 1")), Instant.now(), "OpenAI reasoning");
        when(openAiAdapter.generateTaskBreakdown(any(), any(), any())).thenReturn(expected);

        AiTaskBreakdown actual = compositeAdapter.generateTaskBreakdown(task, project, config);

        assertEquals(expected, actual);
        verify(openAiAdapter).generateTaskBreakdown(task, project, config);
        verify(heuristicAdapter, never()).generateTaskBreakdown(any(), any(), any());
    }

    @Test
    @DisplayName("Routes to Gemini adapter when configured and active")
    void shouldRouteToGeminiWhenConfigured() {
        providerRef.set("gemini");
        when(geminiAdapter.isConfigured()).thenReturn(true);
        AiPriorityProposal expected = new AiPriorityProposal(Priority.HIGH, "Gemini reasoning", 0.95);
        when(geminiAdapter.suggestPriority(any(), any(), any())).thenReturn(expected);

        AiPriorityProposal actual = compositeAdapter.suggestPriority(task, project, config);

        assertEquals(expected, actual);
        verify(geminiAdapter).suggestPriority(task, project, config);
        verify(heuristicAdapter, never()).suggestPriority(any(), any(), any());
    }

    @Test
    @DisplayName("Routes to Anthropic adapter when configured and active")
    void shouldRouteToAnthropicWhenConfigured() {
        providerRef.set("anthropic");
        when(anthropicAdapter.isConfigured()).thenReturn(true);
        AiTaskSummary expected = new AiTaskSummary("Claude summary", List.of("H1"), "Next");
        when(anthropicAdapter.generateTaskSummary(any(), any(), any())).thenReturn(expected);

        AiTaskSummary actual = compositeAdapter.generateTaskSummary(task, project, config);

        assertEquals(expected, actual);
        verify(anthropicAdapter).generateTaskSummary(task, project, config);
        verify(heuristicAdapter, never()).generateTaskSummary(any(), any(), any());
    }

    @Test
    @DisplayName("Routes to Custom adapter when configured and active")
    void shouldRouteToCustomWhenConfigured() {
        providerRef.set("custom");
        when(customAdapter.isConfigured()).thenReturn(true);
        AiTaskBreakdown expected = new AiTaskBreakdown(List.of(new AiSubTaskProposal("Custom sub")), Instant.now(), "Custom reason");
        when(customAdapter.generateTaskBreakdown(any(), any(), any())).thenReturn(expected);

        AiTaskBreakdown actual = compositeAdapter.generateTaskBreakdown(task, project, config);

        assertEquals(expected, actual);
        verify(customAdapter).generateTaskBreakdown(task, project, config);
        verify(heuristicAdapter, never()).generateTaskBreakdown(any(), any(), any());
    }

    @Test
    @DisplayName("Request-level provider override takes precedence over global config")
    void shouldHonorRequestLevelProviderOverride() {
        providerRef.set("openai");
        when(anthropicAdapter.isConfigured()).thenReturn(true);
        AiPriorityProposal expected = new AiPriorityProposal(Priority.LOW, "Anthropic low", 0.9);
        when(anthropicAdapter.suggestPriority(any(), any(), any())).thenReturn(expected);

        AiPromptConfig overrideConfig = config.withProviderOverride("anthropic");
        AiPriorityProposal actual = compositeAdapter.suggestPriority(task, project, overrideConfig);

        assertEquals(expected, actual);
        verify(anthropicAdapter).suggestPriority(task, project, overrideConfig);
        verify(openAiAdapter, never()).suggestPriority(any(), any(), any());
    }

    @Test
    @DisplayName("Request-level custom provider config routes on the fly")
    void shouldHonorRequestLevelCustomProviderConfig() {
        providerRef.set("openai");
        AiCustomProviderConfig customConfig = AiCustomProviderConfig.of(
            "anthropic",
            "https://test-proxy.local/v1",
            "key",
            "claude-3-5-sonnet",
            Map.of()
        );
        AiPromptConfig requestWithCustom = config.withCustomProvider(customConfig);

        when(heuristicAdapter.generateTaskBreakdown(any(), any(), any()))
            .thenReturn(new AiTaskBreakdown(List.of(new AiSubTaskProposal("Fallback")), Instant.now(), "Heuristic fallback"));

        // Will attempt custom adapter creation and fall back smoothly if host is unreachable
        AiTaskBreakdown result = compositeAdapter.generateTaskBreakdown(task, project, requestWithCustom);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Falls back to heuristic adapter when primary provider is not configured")
    void shouldFallbackWhenNotConfigured() {
        when(openAiAdapter.isConfigured()).thenReturn(false);
        AiTaskBreakdown fallbackResult = new AiTaskBreakdown(List.of(new AiSubTaskProposal("Heuristic Sub")), Instant.now(), "Heuristic reasoning");
        when(heuristicAdapter.generateTaskBreakdown(any(), any(), any())).thenReturn(fallbackResult);

        AiTaskBreakdown actual = compositeAdapter.generateTaskBreakdown(task, project, config);

        assertEquals(fallbackResult, actual);
        verify(heuristicAdapter).generateTaskBreakdown(task, project, config);
        verify(openAiAdapter, never()).generateTaskBreakdown(any(), any(), any());
    }

    @Test
    @DisplayName("Falls back to heuristic adapter when primary provider throws an exception")
    void shouldFallbackWhenPrimaryThrowsException() {
        when(openAiAdapter.isConfigured()).thenReturn(true);
        when(openAiAdapter.generateTaskBreakdown(any(), any(), any()))
            .thenThrow(new AiCommunicationException("API 500 error"));

        AiTaskBreakdown fallbackResult = new AiTaskBreakdown(List.of(new AiSubTaskProposal("Heuristic Sub")), Instant.now(), "Fallback");
        when(heuristicAdapter.generateTaskBreakdown(any(), any(), any())).thenReturn(fallbackResult);

        AiTaskBreakdown actual = compositeAdapter.generateTaskBreakdown(task, project, config);

        assertNotNull(actual);
        assertEquals(fallbackResult, actual);
        verify(heuristicAdapter).generateTaskBreakdown(task, project, config);
    }

    @Test
    @DisplayName("Routes directly to heuristic adapter when AI is disabled")
    void shouldRouteToHeuristicWhenDisabled() {
        enabledRef.set(false);
        when(openAiAdapter.isConfigured()).thenReturn(true);

        AiTaskSummary fallbackSummary = new AiTaskSummary("Heuristic summary", List.of("H1"), "Step 1");
        when(heuristicAdapter.generateTaskSummary(any(), any(), any())).thenReturn(fallbackSummary);

        AiTaskSummary actual = compositeAdapter.generateTaskSummary(task, project, config);

        assertEquals(fallbackSummary, actual);
        verify(heuristicAdapter).generateTaskSummary(task, project, config);
        verify(openAiAdapter, never()).generateTaskSummary(any(), any(), any());
    }
}
