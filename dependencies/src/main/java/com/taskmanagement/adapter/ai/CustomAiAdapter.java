package com.taskmanagement.adapter.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.domain.ai.AiCustomProviderConfig;
import com.taskmanagement.domain.ai.AiModelPort;
import com.taskmanagement.domain.ai.AiPriorityProposal;
import com.taskmanagement.domain.ai.AiPromptConfig;
import com.taskmanagement.domain.ai.AiTaskBreakdown;
import com.taskmanagement.domain.ai.AiTaskSummary;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.task.entity.Task;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Universal adapter for user-defined custom AI providers.
 * Seamlessly routes to either OpenAI-compatible wire protocol or Anthropic-compatible wire protocol
 * based on user specification, supporting arbitrary endpoints, custom headers, and models.
 */
public class CustomAiAdapter implements AiModelPort {

    private final String format;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;
    private final int maxTokens;
    private final Map<String, String> customHeaders;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final AiModelPort delegate;

    public CustomAiAdapter(String format,
                           String baseUrl,
                           String apiKey,
                           String model,
                           int timeoutSeconds,
                           int maxTokens,
                           Map<String, String> customHeaders,
                           ObjectMapper objectMapper) {
        this(format, baseUrl, apiKey, model, timeoutSeconds, maxTokens, customHeaders, objectMapper,
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 30)).build());
    }

    public CustomAiAdapter(String format,
                           String baseUrl,
                           String apiKey,
                           String model,
                           int timeoutSeconds,
                           int maxTokens,
                           Map<String, String> customHeaders,
                           ObjectMapper objectMapper,
                           HttpClient httpClient) {
        this.format = (format != null && !format.isBlank()) ? format.trim().toLowerCase() : "openai";
        this.baseUrl = (baseUrl != null) ? baseUrl.trim() : "";
        this.apiKey = (apiKey != null) ? apiKey.trim() : "";
        this.model = (model != null && !model.isBlank()) ? model.trim() : ("anthropic".equalsIgnoreCase(this.format) ? "claude-3-5-sonnet-20241022" : "gpt-4o-mini");
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 30;
        this.maxTokens = maxTokens > 0 ? maxTokens : 4096;
        this.customHeaders = customHeaders != null ? Map.copyOf(customHeaders) : Map.of();
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
        this.httpClient = Objects.requireNonNull(httpClient, "HttpClient cannot be null");

        if ("anthropic".equalsIgnoreCase(this.format)) {
            this.delegate = new AnthropicCompatibleAiAdapter(
                this.apiKey,
                this.baseUrl,
                this.model,
                "2023-06-01",
                this.maxTokens,
                this.timeoutSeconds,
                this.customHeaders,
                this.objectMapper,
                this.httpClient
            );
        } else {
            this.delegate = new OpenAiCompatibleAiAdapter(
                this.apiKey,
                this.baseUrl,
                this.model,
                this.timeoutSeconds,
                this.customHeaders,
                this.objectMapper,
                this.httpClient
            );
        }
    }

    public static CustomAiAdapter fromConfig(AiCustomProviderConfig config, ObjectMapper objectMapper, HttpClient httpClient) {
        Objects.requireNonNull(config, "AiCustomProviderConfig cannot be null");
        return new CustomAiAdapter(
            config.format(),
            config.baseUrl(),
            config.apiKey(),
            config.model(),
            30,
            4096,
            config.headers(),
            objectMapper,
            httpClient
        );
    }

    public boolean isConfigured() {
        return !baseUrl.isBlank();
    }

    public String getFormat() {
        return format;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public AiModelPort getDelegate() {
        return delegate;
    }

    @Override
    public AiTaskBreakdown generateTaskBreakdown(Task task, Project project, AiPromptConfig promptConfig) {
        return delegate.generateTaskBreakdown(task, project, promptConfig);
    }

    @Override
    public AiPriorityProposal suggestPriority(Task task, Project project, AiPromptConfig promptConfig) {
        return delegate.suggestPriority(task, project, promptConfig);
    }

    @Override
    public AiTaskSummary generateTaskSummary(Task task, Project project, AiPromptConfig promptConfig) {
        return delegate.generateTaskSummary(task, project, promptConfig);
    }
}
