package com.taskmanagement.adapter.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.domain.ai.AiModelPort;
import com.taskmanagement.domain.ai.AiPriorityProposal;
import com.taskmanagement.domain.ai.AiPromptConfig;
import com.taskmanagement.domain.ai.AiTaskBreakdown;
import com.taskmanagement.domain.ai.AiTaskSummary;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.task.entity.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Composite AI adapter that dynamically routes to the configured provider
 * (OpenAI-compatible, Google Gemini, Anthropic-compatible, or User-defined Custom Provider)
 * and provides transparent, graceful fallback to the heuristic engine on configuration
 * absence or network/API failure.
 *
 * Supports dynamic per-request provider and custom endpoint overrides.
 */
public class CompositeAiModelAdapter implements AiModelPort {
    private static final Logger logger = LoggerFactory.getLogger(CompositeAiModelAdapter.class);

    private final OpenAiCompatibleAiAdapter openAiAdapter;
    private final GeminiAiAdapter geminiAdapter;
    private final AnthropicCompatibleAiAdapter anthropicAdapter;
    private final CustomAiAdapter customAdapter;
    private final FallbackHeuristicAiAdapter heuristicAdapter;
    private final Supplier<String> providerSupplier;
    private final BooleanSupplier enabledSupplier;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * Backward-compatible constructor for existing tests and wiring.
     */
    public CompositeAiModelAdapter(OpenAiCompatibleAiAdapter openAiAdapter,
                                   GeminiAiAdapter geminiAdapter,
                                   FallbackHeuristicAiAdapter heuristicAdapter,
                                   Supplier<String> providerSupplier,
                                   BooleanSupplier enabledSupplier) {
        this(openAiAdapter, geminiAdapter, null, null, heuristicAdapter, providerSupplier, enabledSupplier,
            new ObjectMapper(), HttpClient.newHttpClient());
    }

    public CompositeAiModelAdapter(OpenAiCompatibleAiAdapter openAiAdapter,
                                   GeminiAiAdapter geminiAdapter,
                                   AnthropicCompatibleAiAdapter anthropicAdapter,
                                   CustomAiAdapter customAdapter,
                                   FallbackHeuristicAiAdapter heuristicAdapter,
                                   Supplier<String> providerSupplier,
                                   BooleanSupplier enabledSupplier,
                                   ObjectMapper objectMapper,
                                   HttpClient httpClient) {
        this.openAiAdapter = openAiAdapter;
        this.geminiAdapter = geminiAdapter;
        this.anthropicAdapter = anthropicAdapter;
        this.customAdapter = customAdapter;
        this.heuristicAdapter = Objects.requireNonNull(heuristicAdapter, "Heuristic adapter cannot be null");
        this.providerSupplier = providerSupplier != null ? providerSupplier : () -> "heuristic";
        this.enabledSupplier = enabledSupplier != null ? enabledSupplier : () -> true;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();
    }

    @Override
    public AiTaskBreakdown generateTaskBreakdown(Task task, Project project, AiPromptConfig promptConfig) {
        AiModelPort primary = resolveAdapterForRequest(promptConfig);
        if (primary == heuristicAdapter) {
            return heuristicAdapter.generateTaskBreakdown(task, project, promptConfig);
        }
        try {
            return primary.generateTaskBreakdown(task, project, promptConfig);
        } catch (Exception e) {
            logger.warn("Primary AI provider failed during breakdown ({}: {}). Falling back to Heuristic engine.",
                e.getClass().getSimpleName(), e.getMessage());
            return heuristicAdapter.generateTaskBreakdown(task, project, promptConfig);
        }
    }

    @Override
    public AiPriorityProposal suggestPriority(Task task, Project project, AiPromptConfig promptConfig) {
        AiModelPort primary = resolveAdapterForRequest(promptConfig);
        if (primary == heuristicAdapter) {
            return heuristicAdapter.suggestPriority(task, project, promptConfig);
        }
        try {
            return primary.suggestPriority(task, project, promptConfig);
        } catch (Exception e) {
            logger.warn("Primary AI provider failed during priority evaluation ({}: {}). Falling back to Heuristic engine.",
                e.getClass().getSimpleName(), e.getMessage());
            return heuristicAdapter.suggestPriority(task, project, promptConfig);
        }
    }

    @Override
    public AiTaskSummary generateTaskSummary(Task task, Project project, AiPromptConfig promptConfig) {
        AiModelPort primary = resolveAdapterForRequest(promptConfig);
        if (primary == heuristicAdapter) {
            return heuristicAdapter.generateTaskSummary(task, project, promptConfig);
        }
        try {
            return primary.generateTaskSummary(task, project, promptConfig);
        } catch (Exception e) {
            logger.warn("Primary AI provider failed during summary generation ({}: {}). Falling back to Heuristic engine.",
                e.getClass().getSimpleName(), e.getMessage());
            return heuristicAdapter.generateTaskSummary(task, project, promptConfig);
        }
    }

    private AiModelPort resolveAdapterForRequest(AiPromptConfig promptConfig) {
        if (!enabledSupplier.getAsBoolean()) {
            return heuristicAdapter;
        }

        // 1. Check if the request carries a dynamic on-the-fly custom provider configuration
        if (promptConfig != null && promptConfig.hasCustomProvider()) {
            try {
                return CustomAiAdapter.fromConfig(promptConfig.customProviderConfig(), objectMapper, httpClient);
            } catch (Exception e) {
                logger.warn("Failed to instantiate request-level custom AI provider: {}. Falling back to default.", e.getMessage());
            }
        }

        // 2. Check if the request explicitly overrides the provider name
        String targetProvider = (promptConfig != null && promptConfig.providerOverride() != null)
            ? promptConfig.providerOverride()
            : providerSupplier.get();

        if (targetProvider == null) {
            return heuristicAdapter;
        }

        String normalized = targetProvider.toLowerCase().trim();
        return switch (normalized) {
            case "openai" -> {
                if (openAiAdapter != null && openAiAdapter.isConfigured()) {
                    yield openAiAdapter;
                }
                logger.debug("OpenAI provider not configured. Using heuristic fallback.");
                yield heuristicAdapter;
            }
            case "gemini" -> {
                if (geminiAdapter != null && geminiAdapter.isConfigured()) {
                    yield geminiAdapter;
                }
                logger.debug("Gemini provider not configured. Using heuristic fallback.");
                yield heuristicAdapter;
            }
            case "anthropic" -> {
                if (anthropicAdapter != null && anthropicAdapter.isConfigured()) {
                    yield anthropicAdapter;
                }
                logger.debug("Anthropic provider not configured. Using heuristic fallback.");
                yield heuristicAdapter;
            }
            case "custom" -> {
                if (customAdapter != null && customAdapter.isConfigured()) {
                    yield customAdapter;
                }
                logger.debug("Custom AI provider not configured. Using heuristic fallback.");
                yield heuristicAdapter;
            }
            default -> heuristicAdapter;
        };
    }
}
