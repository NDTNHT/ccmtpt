package com.taskmanagement.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.adapter.ai.AiConsolePresenter;
import com.taskmanagement.adapter.ai.CompositeAiModelAdapter;
import com.taskmanagement.adapter.ai.FallbackHeuristicAiAdapter;
import com.taskmanagement.adapter.ai.GeminiAiAdapter;
import com.taskmanagement.adapter.ai.OpenAiCompatibleAiAdapter;
import com.taskmanagement.adapter.persistence.IOSaveToSQLite;
import com.taskmanagement.adapter.persistence.IOSaveToSQLiteProject;
import com.taskmanagement.adapter.presenter.TaskConsolePresenter;
import com.taskmanagement.application.ai.AiTaskAssistantControl;
import com.taskmanagement.application.ai.AiTaskEnrichmentListener;
import com.taskmanagement.application.control.ProjectControl;
import com.taskmanagement.application.control.TaskControl;
import com.taskmanagement.domain.ai.AiModelPort;
import com.taskmanagement.domain.project.repository.ProjectRepository;
import com.taskmanagement.domain.task.repository.TaskRepository;
import com.taskmanagement.infrastructure.ai.AiProperties;
import com.taskmanagement.infrastructure.event.InMemoryDomainEventDispatcher;
import com.taskmanagement.infrastructure.listener.TaskActivityLogger;
import com.taskmanagement.infrastructure.ui.IOConsole;

import java.nio.file.Path;

/**
 * Application entry point for the console client.
 */
public class Main {
    public static void main(String[] args) {
        Path databasePath = Path.of(System.getProperty("taskmanagement.db", "data/tasks.db"));

        TaskRepository taskRepository = new IOSaveToSQLite(databasePath);
        ProjectRepository projectRepository = new IOSaveToSQLiteProject(databasePath);
        InMemoryDomainEventDispatcher eventDispatcher = new InMemoryDomainEventDispatcher();
        new TaskActivityLogger(eventDispatcher);

        TaskControl taskControl = new TaskControl(
            taskRepository,
            projectRepository,
            eventDispatcher,
            new TaskConsolePresenter());
        ProjectControl projectControl = new ProjectControl(projectRepository);

        // Configure AI Assistant
        ObjectMapper objectMapper = new ObjectMapper();
        AiProperties aiProperties = new AiProperties();
        String openAiKey = System.getenv("OPENAI_API_KEY");
        String geminiKey = System.getenv("GEMINI_API_KEY");
        String aiProvider = System.getenv("AI_PROVIDER");
        if (openAiKey != null && !openAiKey.isBlank()) {
            aiProperties.getOpenai().setApiKey(openAiKey);
        }
        if (geminiKey != null && !geminiKey.isBlank()) {
            aiProperties.getGemini().setApiKey(geminiKey);
        }
        if (aiProvider != null && !aiProvider.isBlank()) {
            aiProperties.setProvider(aiProvider);
        }

        OpenAiCompatibleAiAdapter openAiAdapter = new OpenAiCompatibleAiAdapter(
            aiProperties.getOpenai().getApiKey(),
            aiProperties.getOpenai().getBaseUrl(),
            aiProperties.getOpenai().getModel(),
            aiProperties.getOpenai().getTimeoutSeconds(),
            objectMapper
        );
        GeminiAiAdapter geminiAdapter = new GeminiAiAdapter(
            aiProperties.getGemini().getApiKey(),
            aiProperties.getGemini().getBaseUrl(),
            aiProperties.getGemini().getModel(),
            aiProperties.getGemini().getTimeoutSeconds(),
            objectMapper
        );
        FallbackHeuristicAiAdapter heuristicAdapter = new FallbackHeuristicAiAdapter();
        AiModelPort aiModelPort = new CompositeAiModelAdapter(
            openAiAdapter,
            geminiAdapter,
            heuristicAdapter,
            aiProperties::getProvider,
            aiProperties::isEnabled
        );

        AiTaskAssistantControl aiAssistant = new AiTaskAssistantControl(
            taskRepository,
            projectRepository,
            aiModelPort,
            eventDispatcher
        );
        new AiTaskEnrichmentListener(eventDispatcher, aiAssistant, aiProperties.isAutoEnrichOnCreate());

        AiConsolePresenter aiPresenter = new AiConsolePresenter();
        new IOConsole(taskControl, projectControl, aiAssistant, aiPresenter, null, null).start();
    }
}
