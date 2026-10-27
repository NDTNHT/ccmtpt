package com.taskmanagement.infrastructure.config;

import com.taskmanagement.adapter.ai.AiConfig;
import com.taskmanagement.adapter.ai.CompositeAiAdapter;
import com.taskmanagement.adapter.persistence.IOSaveToSQLite;
import com.taskmanagement.adapter.persistence.IOSaveToSQLiteProject;
import com.taskmanagement.adapter.presenter.TaskConsolePresenter;
import com.taskmanagement.application.boundary.AiTaskInputBoundary;
import com.taskmanagement.application.control.AiTaskControl;
import com.taskmanagement.application.control.ProjectControl;
import com.taskmanagement.application.control.TaskControl;
import com.taskmanagement.application.port.AiAssistantPort;
import com.taskmanagement.domain.project.repository.ProjectRepository;
import com.taskmanagement.domain.task.repository.TaskRepository;
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

        AiConfig aiConfig = new AiConfig();
        aiConfig.setApiKey(System.getProperty("ai.api-key", System.getenv().getOrDefault("AI_API_KEY", "")));
        aiConfig.setBaseUrl(System.getProperty("ai.base-url", System.getenv().getOrDefault("AI_BASE_URL", "https://api.openai.com/v1")));
        aiConfig.setModel(System.getProperty("ai.model", System.getenv().getOrDefault("AI_MODEL", "gpt-4o-mini")));
        aiConfig.setProvider(System.getProperty("ai.provider", System.getenv().getOrDefault("AI_PROVIDER", "auto")));

        AiAssistantPort aiAssistantPort = new CompositeAiAdapter(aiConfig);
        AiTaskInputBoundary aiTaskInput = new AiTaskControl(taskRepository, projectRepository, aiAssistantPort, eventDispatcher);

        new IOConsole(taskControl, projectControl, aiTaskInput).start();
    }
}
