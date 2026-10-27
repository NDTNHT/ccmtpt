package com.taskmanagement.infrastructure.api;

import com.taskmanagement.adapter.ai.AiConfig;
import com.taskmanagement.adapter.ai.CompositeAiAdapter;
import com.taskmanagement.adapter.persistence.IOSaveToSQLite;
import com.taskmanagement.adapter.persistence.IOSaveToSQLiteProject;
import com.taskmanagement.application.boundary.AiTaskInputBoundary;
import com.taskmanagement.application.control.AiTaskControl;
import com.taskmanagement.application.control.ProjectControl;
import com.taskmanagement.application.control.TaskControl;
import com.taskmanagement.application.port.AiAssistantPort;
import com.taskmanagement.application.port.DomainEventDispatcher;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.enums.ProjectType;
import com.taskmanagement.domain.project.repository.ProjectRepository;
import com.taskmanagement.domain.task.repository.TaskRepository;
import com.taskmanagement.infrastructure.event.InMemoryDomainEventDispatcher;
import com.taskmanagement.infrastructure.listener.TaskActivityLogger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.nio.file.Path;

/**
 * Application entry point for the REST service.
 */
@SpringBootApplication(scanBasePackages = "com.taskmanagement")
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

    @Bean
    TaskRepository taskRepository(Environment environment) {
        return new IOSaveToSQLite(databasePath(environment));
    }

    @Bean
    ProjectRepository projectRepository(Environment environment) {
        return new IOSaveToSQLiteProject(databasePath(environment));
    }

    @Bean
    InMemoryDomainEventDispatcher domainEventDispatcher() {
        return new InMemoryDomainEventDispatcher();
    }

    @Bean
    TaskActivityLogger taskActivityLogger(InMemoryDomainEventDispatcher dispatcher) {
        return new TaskActivityLogger(dispatcher);
    }

    @Bean
    TaskControl taskControl(TaskRepository taskRepository,
                            ProjectRepository projectRepository,
                            DomainEventDispatcher eventDispatcher) {
        return new TaskControl(taskRepository, projectRepository, eventDispatcher);
    }

    @Bean
    ProjectControl projectControl(ProjectRepository projectRepository) {
        return new ProjectControl(projectRepository);
    }

    @Bean
    CommandLineRunner initDefaultRegularProjects(ProjectRepository projectRepository) {
        return args -> {
            boolean hasRegular = projectRepository.findAll().stream()
                .anyMatch(p -> p.getProjectType() == ProjectType.REGULAR);
            if (!hasRegular) {
                projectRepository.save(Project.create(
                    "ALPHA CORE SYSTEMS", "Primary engineering and tactical infrastructure"));
                projectRepository.save(Project.create(
                    "OMEGA CYBERNETICS", "Autonomous robotics and hardware relays"));
            }
        };
    }

    @Bean
    AiConfig aiConfig(Environment environment) {
        AiConfig config = new AiConfig();
        config.setEnabled(environment.getProperty("ai.enabled", Boolean.class, true));
        config.setProvider(environment.getProperty("ai.provider", "auto"));
        config.setBaseUrl(environment.getProperty("ai.base-url", "https://api.openai.com/v1"));
        config.setApiKey(environment.getProperty("ai.api-key", ""));
        config.setModel(environment.getProperty("ai.model", "gpt-4o-mini"));
        config.setTemperature(environment.getProperty("ai.temperature", Double.class, 0.3));
        config.setTimeoutSeconds(environment.getProperty("ai.timeout-seconds", Integer.class, 30));
        String sysPrompt = environment.getProperty("ai.system-prompt");
        if (sysPrompt != null && !sysPrompt.isBlank()) {
            config.setSystemPrompt(sysPrompt);
        }
        return config;
    }

    @Bean
    AiAssistantPort aiAssistantPort(AiConfig aiConfig) {
        return new CompositeAiAdapter(aiConfig);
    }

    @Bean
    AiTaskInputBoundary aiTaskInputBoundary(TaskRepository taskRepository,
                                            ProjectRepository projectRepository,
                                            AiAssistantPort aiAssistantPort,
                                            DomainEventDispatcher eventDispatcher) {
        return new AiTaskControl(taskRepository, projectRepository, aiAssistantPort, eventDispatcher);
    }

    private Path databasePath(Environment environment) {
        return Path.of(environment.getProperty("taskmanagement.db", "data/tasks.db"));
    }
}
