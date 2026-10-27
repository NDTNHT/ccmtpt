package com.taskmanagement.adapter.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.application.dto.ai.ProviderVerificationResultDto;
import com.taskmanagement.application.port.AiAssistantPort;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registry holding pre-configured standard providers (OpenAI, Anthropic, Pollinations, Ollama, Simulated)
 * and allowing users to register, configure, test, and activate custom OpenAI- or Anthropic-compatible providers.
 */
public class AiProviderRegistry {
    private final Map<String, CustomProviderDefinition> providers = new LinkedHashMap<>();
    private String activeProviderId = "auto";
    private final AiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AiProviderRegistry(AiConfig config) {
        this(config, HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(config != null ? config.getTimeoutSeconds() : 30))
            .build(), new ObjectMapper());
    }

    public AiProviderRegistry(AiConfig config, HttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config != null ? config : AiConfig.defaults();
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        registerDefaults();
    }

    private void registerDefaults() {
        // 1. OpenAI Standard
        registerProvider(new CustomProviderDefinition(
            "openai",
            "OpenAI Official",
            AiProtocol.OPENAI,
            "https://api.openai.com/v1",
            config.getApiKey(),
            config.getModel() != null ? config.getModel() : "gpt-4o-mini",
            config.getTimeoutSeconds()
        ));

        // 2. Anthropic Standard
        registerProvider(new CustomProviderDefinition(
            "anthropic",
            "Anthropic Claude",
            AiProtocol.ANTHROPIC,
            "https://api.anthropic.com/v1",
            "",
            "claude-3-5-sonnet-20241022",
            config.getTimeoutSeconds()
        ));

        // 3. Pollinations AI (OpenAI Compatible)
        registerProvider(new CustomProviderDefinition(
            "pollinations",
            "Pollinations AI",
            AiProtocol.OPENAI,
            "https://gen.pollinations.ai/v1",
            "",
            "qwen/qwen3.7-flash",
            config.getTimeoutSeconds()
        ));

        // 4. Ollama Local (OpenAI Compatible)
        registerProvider(new CustomProviderDefinition(
            "ollama",
            "Ollama Local Server",
            AiProtocol.OPENAI,
            "http://localhost:11434/v1",
            "ollama",
            "llama3.2",
            config.getTimeoutSeconds()
        ));

        // 5. Offline Simulation
        registerProvider(new CustomProviderDefinition(
            "simulated",
            "Simulated Offline Heuristics",
            AiProtocol.SIMULATED,
            "",
            "",
            "heuristic-simulated",
            1
        ));

        this.activeProviderId = "auto";
    }

    public synchronized void registerProvider(CustomProviderDefinition definition) {
        Objects.requireNonNull(definition, "Provider definition cannot be null");
        providers.put(definition.id().toLowerCase(), definition);
    }

    public synchronized List<CustomProviderDefinition> listProviders() {
        return new ArrayList<>(providers.values());
    }

    public synchronized CustomProviderDefinition getProvider(String id) {
        if (id == null) {
            return null;
        }
        return providers.get(id.toLowerCase());
    }

    public synchronized String getActiveProviderId() {
        return activeProviderId;
    }

    public synchronized void setActiveProviderId(String id) {
        if (id == null || id.isBlank() || "auto".equalsIgnoreCase(id)) {
            this.activeProviderId = "auto";
            return;
        }
        String normalized = id.toLowerCase();
        if (!providers.containsKey(normalized)) {
            throw new IllegalArgumentException("Provider not registered: " + id);
        }
        this.activeProviderId = normalized;
    }

    public synchronized CustomProviderDefinition resolveActiveDefinition() {
        if (!"auto".equalsIgnoreCase(activeProviderId)) {
            CustomProviderDefinition selected = providers.get(activeProviderId);
            if (selected != null) {
                return selected;
            }
        }

        // In 'auto' mode: if config has API key, map config; otherwise fallback to simulated
        if (config.hasValidApiKey()) {
            return new CustomProviderDefinition(
                "active-configured",
                "Configured Active Provider",
                AiProtocol.OPENAI,
                config.getBaseUrl(),
                config.getApiKey(),
                config.getModel(),
                config.getTimeoutSeconds()
            );
        }

        return providers.get("simulated");
    }

    public ProviderVerificationResultDto verifyAndDiscoverModels(String baseUrl, String apiKey, AiProtocol protocol, int timeoutSeconds) {
        if (protocol == AiProtocol.SIMULATED) {
            return ProviderVerificationResultDto.success(200, List.of("heuristic-default", "heuristic-fast", "heuristic-deep"), "SIMULATED");
        }
        if (protocol == AiProtocol.ANTHROPIC) {
            return AnthropicCompatibleAiAdapter.verifyAnthropicModels(this.httpClient, this.objectMapper, baseUrl, apiKey, timeoutSeconds);
        }
        return OpenAiCompatibleAiAdapter.verifyOpenAiModels(this.httpClient, this.objectMapper, baseUrl, apiKey, timeoutSeconds);
    }

    public synchronized ProviderVerificationResultDto verifyRegisteredProvider(String providerId) {
        CustomProviderDefinition definition = getProvider(providerId);
        if (definition == null) {
            return ProviderVerificationResultDto.failure(404, "Provider khong ton tai trong he thong: " + providerId, "UNKNOWN");
        }
        ProviderVerificationResultDto result = verifyAndDiscoverModels(
            definition.baseUrl(),
            definition.apiKey(),
            definition.protocol(),
            definition.timeoutSeconds()
        );
        if (result.valid() && !result.availableModels().isEmpty()) {
            CustomProviderDefinition updated = definition.withAvailableModels(result.availableModels());
            providers.put(definition.id().toLowerCase(), updated);
        }
        return result;
    }

    public synchronized void updateProviderDefaultModel(String providerId, String newDefaultModel) {
        CustomProviderDefinition definition = getProvider(providerId);
        if (definition != null && newDefaultModel != null && !newDefaultModel.isBlank()) {
            CustomProviderDefinition updated = definition.withDefaultModel(newDefaultModel.trim());
            providers.put(definition.id().toLowerCase(), updated);
        }
    }

    public AiAssistantPort createAdapterFor(CustomProviderDefinition definition) {
        if (definition == null || definition.protocol() == AiProtocol.SIMULATED || !definition.hasValidApiKey()) {
            return new SimulatedAiAdapter(config);
        }
        if (definition.protocol() == AiProtocol.ANTHROPIC) {
            return new AnthropicCompatibleAiAdapter(definition, config, httpClient, objectMapper, new SimulatedAiAdapter(config));
        }
        // Default to OpenAI standard
        AiConfig providerConfig = new AiConfig();
        providerConfig.setBaseUrl(definition.baseUrl());
        providerConfig.setModel(definition.defaultModel());
        providerConfig.setApiKey(definition.apiKey());
        providerConfig.setTimeoutSeconds(definition.timeoutSeconds());
        return new OpenAiCompatibleAiAdapter(providerConfig, httpClient, objectMapper, new SimulatedAiAdapter(config));
    }
}
