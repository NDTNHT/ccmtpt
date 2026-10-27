package com.taskmanagement.infrastructure.api;

import com.taskmanagement.adapter.persistence.IOSaveToSQLite;
import com.taskmanagement.adapter.persistence.IOSaveToSQLiteProject;
import com.taskmanagement.application.control.ProjectControl;
import com.taskmanagement.application.control.TaskControl;
import com.taskmanagement.application.port.DomainEventDispatcher;
import com.taskmanagement.domain.project.repository.ProjectRepository;
import com.taskmanagement.domain.task.repository.TaskRepository;
import com.taskmanagement.infrastructure.event.InMemoryDomainEventDispatcher;
import com.taskmanagement.infrastructure.listener.TaskActivityLogger;
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

    private Path databasePath(Environment environment) {
        return Path.of(environment.getProperty("taskmanagement.db", "data/tasks.db"));
    }
}
