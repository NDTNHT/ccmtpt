package com.taskmanagement.adapter.persistence;

import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.enums.ProjectType;
import com.taskmanagement.domain.project.repository.ProjectRepository;
import com.taskmanagement.domain.project.valueobject.ProjectId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite project repository. Stores projects in the same database file as
 * {@link IOSaveToSQLite}.
 */
public class IOSaveToSQLiteProject implements ProjectRepository {
    private static final String CREATE_PROJECTS_SQL = """
        CREATE TABLE IF NOT EXISTS projects (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            description TEXT,
            type TEXT NOT NULL
        )
        """;

    private static final String UPSERT_PROJECT_SQL = """
        INSERT INTO projects (id, name, description, type)
        VALUES (?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
            name = excluded.name,
            description = excluded.description,
            type = excluded.type
        """;

    private final String jdbcUrl;

    public IOSaveToSQLiteProject() {
        this(Path.of(System.getProperty("taskmanagement.db", "data/tasks.db")));
    }

    public IOSaveToSQLiteProject(Path databasePath) {
        Objects.requireNonNull(databasePath, "Database path cannot be null");
        try {
            Path absolutePath = databasePath.toAbsolutePath();
            Path parent = absolutePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            this.jdbcUrl = "jdbc:sqlite:" + absolutePath;
            initializeSchema();
            ensureInboxExists();
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Could not initialize the SQLite project repository.", exception);
        }
    }

    @Override
    public void save(Project project) {
        Objects.requireNonNull(project, "Project cannot be null");
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(UPSERT_PROJECT_SQL)) {
            statement.setString(1, project.getId().toString());
            statement.setString(2, project.getName().value());
            statement.setString(3, project.getDescription());
            statement.setString(4, project.getProjectType().name());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save project " + project.getId() + " to SQLite.", exception);
        }
    }

    @Override
    public Optional<Project> findById(ProjectId id) {
        Objects.requireNonNull(id, "ProjectId cannot be null");
        String sql = "SELECT * FROM projects WHERE id = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapProject(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not find project " + id + " in SQLite.", exception);
        }
    }

    @Override
    public synchronized Project getInbox() {
        String sql = "SELECT * FROM projects WHERE type = ? ORDER BY rowid LIMIT 1";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ProjectType.INBOX.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapProject(resultSet);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load the Inbox project from SQLite.", exception);
        }

        Project inbox = Project.createInbox();
        save(inbox);
        return inbox;
    }

    @Override
    public List<Project> findAll() {
        String sql = "SELECT * FROM projects ORDER BY rowid";
        List<Project> projects = new ArrayList<>();
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                projects.add(mapProject(resultSet));
            }
            return List.copyOf(projects);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not retrieve projects from SQLite.", exception);
        }
    }

    private void initializeSchema() throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_PROJECTS_SQL);
        }
    }

    private void ensureInboxExists() {
        getInbox();
    }

    private Project mapProject(ResultSet resultSet) throws SQLException {
        return new Project(
            ProjectId.of(UUID.fromString(resultSet.getString("id"))),
            resultSet.getString("name"),
            resultSet.getString("description"),
            ProjectType.valueOf(resultSet.getString("type")));
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }
}
