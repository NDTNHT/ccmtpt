package com.taskmanagement.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.adapter.ai.AiConsolePresenter;
import com.taskmanagement.adapter.ai.AnthropicCompatibleAiAdapter;
import com.taskmanagement.adapter.ai.CompositeAiModelAdapter;
import com.taskmanagement.adapter.ai.CustomAiAdapter;
import com.taskmanagement.adapter.ai.FallbackHeuristicAiAdapter;
import com.taskmanagement.adapter.ai.GeminiAiAdapter;
import com.taskmanagement.adapter.ai.OpenAiCompatibleAiAdapter;
import com.taskmanagement.application.ai.AiTaskAssistantControl;
import com.taskmanagement.application.ai.AiTaskAssistantInputBoundary;
import com.taskmanagement.application.ai.AiTaskEnrichmentListener;
import com.taskmanagement.application.port.DomainEventDispatcher;
import com.taskmanagement.domain.ai.AiModelPort;
import com.taskmanagement.domain.ai.AiPromptConfig;
import com.taskmanagement.domain.project.repository.ProjectRepository;
import com.taskmanagement.domain.task.repository.TaskRepository;
import com.taskmanagement.infrastructure.event.InMemoryDomainEventDispatcher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Spring configuration wiring AI adapters, use case interactor, and event listeners.
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfiguration {

    @Bean
    public FallbackHeuristicAiAdapter fallbackHeuristicAiAdapter() {
        return new FallbackHeuristicAiAdapter();
    }

    @Bean
    public OpenAiCompatibleAiAdapter openAiCompatibleAiAdapter(AiProperties properties, ObjectMapper objectMapper) {
        return new OpenAiCompatibleAiAdapter(
            properties.getOpenai().getApiKey(),
            properties.getOpenai().getBaseUrl(),
            properties.getOpenai().getModel(),
            properties.getOpenai().getTimeoutSeconds(),
            objectMapper
        );
    }

    @Bean
    public GeminiAiAdapter geminiAiAdapter(AiProperties properties, ObjectMapper objectMapper) {
        return new GeminiAiAdapter(
            properties.getGemini().getApiKey(),
            properties.getGemini().getBaseUrl(),
            properties.getGemini().getModel(),
            properties.getGemini().getTimeoutSeconds(),
            objectMapper
        );
    }

    @Bean
    public AnthropicCompatibleAiAdapter anthropicCompatibleAiAdapter(AiProperties properties, ObjectMapper objectMapper) {
        return new AnthropicCompatibleAiAdapter(
            properties.getAnthropic().getApiKey(),
            properties.getAnthropic().getBaseUrl(),
            properties.getAnthropic().getModel(),
            properties.getAnthropic().getVersion(),
            properties.getAnthropic().getMaxTokens(),
            properties.getAnthropic().getTimeoutSeconds(),
            Map.of(),
            objectMapper,
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getAnthropic().getTimeoutSeconds()))
                .build()
        );
    }

    @Bean
    public CustomAiAdapter customAiAdapter(AiProperties properties, ObjectMapper objectMapper) {
        return new CustomAiAdapter(
            properties.getCustom().getFormat(),
            properties.getCustom().getBaseUrl(),
            properties.getCustom().getApiKey(),
            properties.getCustom().getModel(),
            properties.getCustom().getTimeoutSeconds(),
            properties.getCustom().getMaxTokens(),
            properties.getCustom().getHeaders(),
            objectMapper
        );
    }

    @Bean
    @Primary
    public AiModelPort aiModelPort(OpenAiCompatibleAiAdapter openAiAdapter,
                                   GeminiAiAdapter geminiAdapter,
                                   AnthropicCompatibleAiAdapter anthropicAdapter,
                                   CustomAiAdapter customAdapter,
                                   FallbackHeuristicAiAdapter heuristicAdapter,
                                   AiProperties properties,
                                   ObjectMapper objectMapper) {
        return new CompositeAiModelAdapter(
            openAiAdapter,
            geminiAdapter,
            anthropicAdapter,
            customAdapter,
            heuristicAdapter,
            properties::getProvider,
            properties::isEnabled,
            objectMapper,
            HttpClient.newHttpClient()
        );
    }

    @Bean
    public AiTaskAssistantInputBoundary aiTaskAssistantInputBoundary(TaskRepository taskRepository,
                                                                    ProjectRepository projectRepository,
                                                                    AiModelPort aiModelPort,
                                                                    DomainEventDispatcher eventDispatcher,
                                                                    AiProperties properties) {
        AiPromptConfig promptConfig = AiPromptConfig.of(
            properties.getDefaultSystemPrompt(),
            ""
        );
        return new AiTaskAssistantControl(
            taskRepository,
            projectRepository,
            aiModelPort,
            eventDispatcher,
            promptConfig
        );
    }

    @Bean
    public AiTaskEnrichmentListener aiTaskEnrichmentListener(InMemoryDomainEventDispatcher dispatcher,
                                                            AiTaskAssistantInputBoundary aiAssistant,
                                                            AiProperties properties) {
        return new AiTaskEnrichmentListener(
            dispatcher,
            aiAssistant,
            properties.isAutoEnrichOnCreate(),
            Executors.newCachedThreadPool()
        );
    }

    @Bean
    public AiConsolePresenter aiConsolePresenter() {
        return new AiConsolePresenter();
    }
}
