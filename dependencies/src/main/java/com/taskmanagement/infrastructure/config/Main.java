package com.taskmanagement.infrastructure.config;

import com.taskmanagement.adapter.persistence.IOSaveToSQLite;
import com.taskmanagement.adapter.persistence.IOSaveToSQLiteProject;
import com.taskmanagement.adapter.presenter.TaskConsolePresenter;
import com.taskmanagement.application.control.ProjectControl;
import com.taskmanagement.application.control.TaskControl;
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

        new IOConsole(taskControl, projectControl).start();
    }
}
