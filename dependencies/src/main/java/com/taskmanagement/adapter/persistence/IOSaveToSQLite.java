package com.taskmanagement.adapter.persistence;

import com.taskmanagement.domain.project.valueobject.ProjectId;
import com.taskmanagement.domain.task.entity.SubTask;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.enums.TaskStatus;
import com.taskmanagement.domain.task.repository.TaskRepository;
import com.taskmanagement.domain.task.valueobject.Deadline;
import com.taskmanagement.domain.task.valueobject.TaskId;
import com.taskmanagement.domain.task.valueobject.TaskTitle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite task repository. Tasks and their subtasks are written in a single transaction.
 */
public class IOSaveToSQLite implements TaskRepository {
    private static final String CREATE_TASKS_SQL = """
        CREATE TABLE IF NOT EXISTS tasks (
            id TEXT PRIMARY KEY,
            title TEXT NOT NULL,
            description TEXT,
            due_date TEXT,
            priority TEXT NOT NULL,
            status TEXT NOT NULL,
            project_id TEXT NOT NULL
        )
        """;

    private static final String CREATE_SUBTASKS_SQL = """
        CREATE TABLE IF NOT EXISTS subtasks (
            task_id TEXT NOT NULL,
            subtask_id TEXT NOT NULL,
            title TEXT NOT NULL,
            completed INTEGER NOT NULL,
            position INTEGER NOT NULL,
            PRIMARY KEY (task_id, subtask_id),
            FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
        )
        """;

    private static final String CREATE_SUBTASKS_INDEX_SQL =
        "CREATE INDEX IF NOT EXISTS idx_subtasks_task_id ON subtasks(task_id)";

    private static final String CREATE_TASKS_PROJECT_INDEX_SQL =
        "CREATE INDEX IF NOT EXISTS idx_tasks_project_id ON tasks(project_id)";

    private static final String UPSERT_TASK_SQL = """
        INSERT INTO tasks (id, title, description, due_date, priority, status, project_id)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
            title = excluded.title,
            description = excluded.description,
            due_date = excluded.due_date,
            priority = excluded.priority,
            status = excluded.status,
            project_id = excluded.project_id
        """;

    private static final String DELETE_SUBTASKS_SQL = "DELETE FROM subtasks WHERE task_id = ?";

    private static final String INSERT_SUBTASK_SQL = """
        INSERT INTO subtasks (task_id, subtask_id, title, completed, position)
        VALUES (?, ?, ?, ?, ?)
        """;

    private final String jdbcUrl;

    public IOSaveToSQLite() {
        this(Path.of(System.getProperty("taskmanagement.db", "data/tasks.db")));
    }

    public IOSaveToSQLite(Path databasePath) {
        Objects.requireNonNull(databasePath, "Database path cannot be null");
        try {
            Path absolutePath = databasePath.toAbsolutePath();
            Path parent = absolutePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            this.jdbcUrl = "jdbc:sqlite:" + absolutePath;
            initializeSchema();
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Could not initialize the SQLite task repository.", exception);
        }
    }

    @Override
    public void save(Task task) {
        Objects.requireNonNull(task, "Task cannot be null");
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                writeTask(connection, task);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save task " + task.getId() + " to SQLite.", exception);
        }
    }

    @Override
    public Optional<Task> findById(TaskId id) {
        Objects.requireNonNull(id, "TaskId cannot be null");
        String sql = "SELECT * FROM tasks WHERE id = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                TaskRow row = readRow(resultSet);
                List<SubTask> subTasks = loadSubTasks(connection, List.of(id)).getOrDefault(id, List.of());
                return Optional.of(row.toTask(subTasks));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not find task " + id + " in SQLite.", exception);
        }
    }

    @Override
    public List<Task> findAll() {
        String sql = "SELECT * FROM tasks ORDER BY rowid";
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            List<TaskRow> rows = readRows(resultSet);
            Map<TaskId, List<SubTask>> subTasks = loadSubTasks(connection, idsOf(rows));
            return rows.stream()
                .map(row -> row.toTask(subTasks.getOrDefault(row.id(), List.of())))
                .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not retrieve tasks from SQLite.", exception);
        }
    }

    @Override
    public List<Task> findByProjectId(ProjectId projectId) {
        Objects.requireNonNull(projectId, "ProjectId cannot be null");
        String sql = "SELECT * FROM tasks WHERE project_id = ? ORDER BY rowid";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, projectId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<TaskRow> rows = readRows(resultSet);
                Map<TaskId, List<SubTask>> subTasks = loadSubTasks(connection, idsOf(rows));
                return rows.stream()
                    .map(row -> row.toTask(subTasks.getOrDefault(row.id(), List.of())))
                    .toList();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not query tasks by project in SQLite.", exception);
        }
    }

    @Override
    public boolean existsByProjectIdAndTitle(ProjectId projectId, TaskTitle title) {
        Objects.requireNonNull(projectId, "ProjectId cannot be null");
        Objects.requireNonNull(title, "TaskTitle cannot be null");
        String sql = "SELECT title FROM tasks WHERE project_id = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, projectId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    TaskTitle storedTitle = new TaskTitle(resultSet.getString("title"));
                    if (storedTitle.sameValueAs(title)) {
                        return true;
                    }
                }
                return false;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not check task title uniqueness in SQLite.", exception);
        }
    }

    @Override
    public void delete(TaskId id) {
        Objects.requireNonNull(id, "TaskId cannot be null");
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteSubTasks = connection.prepareStatement(DELETE_SUBTASKS_SQL);
                 PreparedStatement deleteTask = connection.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
                deleteSubTasks.setString(1, id.toString());
                deleteSubTasks.executeUpdate();
                deleteTask.setString(1, id.toString());
                deleteTask.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete task " + id + " from SQLite.", exception);
        }
    }

    private void writeTask(Connection connection, Task task) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_TASK_SQL)) {
            statement.setString(1, task.getId().toString());
            statement.setString(2, task.getTitle().value());
            statement.setString(3, task.getDescription());
            statement.setString(4, task.getDueDate() == null ? null : task.getDueDate().dueDate().toString());
            statement.setString(5, task.getPriority().name());
            statement.setString(6, task.getStatus().name());
            statement.setString(7, task.getProjectId().toString());
            statement.executeUpdate();
        }

        try (PreparedStatement deleteSubTasks = connection.prepareStatement(DELETE_SUBTASKS_SQL)) {
            deleteSubTasks.setString(1, task.getId().toString());
            deleteSubTasks.executeUpdate();
        }

        try (PreparedStatement insertSubTask = connection.prepareStatement(INSERT_SUBTASK_SQL)) {
            int position = 0;
            for (SubTask subTask : task.getSubTasks()) {
                insertSubTask.setString(1, task.getId().toString());
                insertSubTask.setString(2, subTask.getId().toString());
                insertSubTask.setString(3, subTask.getTitle());
                insertSubTask.setInt(4, subTask.isCompleted() ? 1 : 0);
                insertSubTask.setInt(5, position++);
                insertSubTask.addBatch();
            }
            insertSubTask.executeBatch();
        }
    }

    private List<TaskRow> readRows(ResultSet resultSet) throws SQLException {
        List<TaskRow> rows = new ArrayList<>();
        while (resultSet.next()) {
            rows.add(readRow(resultSet));
        }
        return rows;
    }

    private TaskRow readRow(ResultSet resultSet) throws SQLException {
        String dueDate = resultSet.getString("due_date");
        return new TaskRow(
            TaskId.of(UUID.fromString(resultSet.getString("id"))),
            new TaskTitle(resultSet.getString("title")),
            resultSet.getString("description"),
            dueDate == null ? null : Deadline.of(Instant.parse(dueDate)),
            Priority.valueOf(resultSet.getString("priority")),
            TaskStatus.valueOf(resultSet.getString("status")),
            ProjectId.of(UUID.fromString(resultSet.getString("project_id"))));
    }

    private List<TaskId> idsOf(List<TaskRow> rows) {
        return rows.stream().map(TaskRow::id).toList();
    }

    private Map<TaskId, List<SubTask>> loadSubTasks(Connection connection, Collection<TaskId> taskIds)
        throws SQLException {
        if (taskIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = String.join(",", Collections.nCopies(taskIds.size(), "?"));
        String sql = """
            SELECT task_id, subtask_id, title, completed
            FROM subtasks
            WHERE task_id IN (%s)
            ORDER BY task_id, position
            """.formatted(placeholders);

        Map<TaskId, List<SubTask>> result = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (TaskId taskId : taskIds) {
                statement.setString(index++, taskId.toString());
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    TaskId taskId = TaskId.of(UUID.fromString(resultSet.getString("task_id")));
                    SubTask subTask = SubTask.reconstitute(
                        UUID.fromString(resultSet.getString("subtask_id")),
                        resultSet.getString("title"),
                        resultSet.getInt("completed") != 0);
                    result.computeIfAbsent(taskId, ignored -> new ArrayList<>()).add(subTask);
                }
            }
        }
        return result;
    }

    private void initializeSchema() throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute(CREATE_TASKS_SQL);
            statement.execute(CREATE_SUBTASKS_SQL);
            statement.execute(CREATE_SUBTASKS_INDEX_SQL);
            statement.execute(CREATE_TASKS_PROJECT_INDEX_SQL);
        }
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    private record TaskRow(
        TaskId id,
        TaskTitle title,
        String description,
        Deadline dueDate,
        Priority priority,
        TaskStatus status,
        ProjectId projectId
    ) {
        Task toTask(List<SubTask> subTasks) {
            return Task.reconstitute(id, title, description, dueDate, priority, status, projectId, subTasks);
        }
    }
}
