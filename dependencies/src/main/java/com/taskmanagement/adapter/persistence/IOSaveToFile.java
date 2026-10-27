package com.taskmanagement.adapter.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * JSON-file task repository.
 *
 * <p>Writes are atomic: the data set is serialized to a temporary file and then moved
 * into place. Intended for single-user deployments; not for concurrent writers.</p>
 */
public class IOSaveToFile implements TaskRepository {
    private final Path file;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<TaskId, Task> cache = new LinkedHashMap<>();

    public IOSaveToFile() {
        this(Path.of(System.getProperty("taskmanagement.file", "data/tasks.json")));
    }

    public IOSaveToFile(Path file) {
        this.file = Objects.requireNonNull(file, "File path cannot be null").toAbsolutePath();
        load();
    }

    @Override
    public synchronized void save(Task task) {
        Objects.requireNonNull(task, "Task cannot be null");
        cache.put(task.getId(), task);
        flush();
    }

    @Override
    public synchronized Optional<Task> findById(TaskId id) {
        return Optional.ofNullable(cache.get(id));
    }

    @Override
    public synchronized List<Task> findAll() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public synchronized List<Task> findByProjectId(ProjectId projectId) {
        Objects.requireNonNull(projectId, "ProjectId cannot be null");
        return cache.values().stream()
            .filter(task -> task.getProjectId().equals(projectId))
            .toList();
    }

    @Override
    public synchronized boolean existsByProjectIdAndTitle(ProjectId projectId, TaskTitle title) {
        Objects.requireNonNull(projectId, "ProjectId cannot be null");
        Objects.requireNonNull(title, "TaskTitle cannot be null");
        return cache.values().stream()
            .anyMatch(task -> task.getProjectId().equals(projectId)
                && task.getTitle().sameValueAs(title));
    }

    @Override
    public synchronized void delete(TaskId id) {
        if (cache.remove(id) != null) {
            flush();
        }
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            if (Files.size(file) == 0) {
                return;
            }
            List<TaskRecord> records = objectMapper.readValue(
                file.toFile(), new TypeReference<List<TaskRecord>>() { });
            for (TaskRecord record : records) {
                Task task = toTask(record);
                cache.put(task.getId(), task);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load tasks from " + file, exception);
        }
    }

    private void flush() {
        Path temp = null;
        try {
            Path parent = file.getParent();
            Path tempDirectory = parent != null ? parent : Path.of(".").toAbsolutePath();
            Files.createDirectories(tempDirectory);
            List<TaskRecord> records = cache.values().stream()
                .map(this::toRecord)
                .toList();

            temp = Files.createTempFile(tempDirectory, "tasks-", ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), records);
            try {
                Files.move(temp, file,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write tasks to " + file, exception);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // Best-effort cleanup of a temporary file that could not be moved.
                }
            }
        }
    }

    private TaskRecord toRecord(Task task) {
        List<SubTaskRecord> subTasks = task.getSubTasks().stream()
            .map(subTask -> new SubTaskRecord(
                subTask.getId().toString(), subTask.getTitle(), subTask.isCompleted()))
            .toList();
        return new TaskRecord(
            task.getId().toString(),
            task.getTitle().value(),
            task.getDescription(),
            task.getDueDate() == null ? null : task.getDueDate().dueDate().toString(),
            task.getPriority().name(),
            task.getStatus().name(),
            task.getProjectId().toString(),
            subTasks);
    }

    private Task toTask(TaskRecord record) {
        Objects.requireNonNull(record, "Task record cannot be null");
        List<SubTask> subTasks = record.subTasks() == null
            ? List.of()
            : record.subTasks().stream()
                .map(subTask -> SubTask.reconstitute(
                    UUID.fromString(subTask.id()), subTask.title(), subTask.completed()))
                .toList();
        return Task.reconstitute(
            TaskId.of(UUID.fromString(record.id())),
            new TaskTitle(record.title()),
            record.description(),
            record.dueDate() == null || record.dueDate().isBlank() ? null : Deadline.of(Instant.parse(record.dueDate())),
            Priority.valueOf(record.priority()),
            TaskStatus.valueOf(record.status()),
            ProjectId.of(UUID.fromString(record.projectId())),
            subTasks);
    }

    public Path getFile() {
        return file;
    }

    private record TaskRecord(
        String id,
        String title,
        String description,
        String dueDate,
        String priority,
        String status,
        String projectId,
        List<SubTaskRecord> subTasks
    ) {
    }

    private record SubTaskRecord(
        String id,
        String title,
        boolean completed
    ) {
    }
}
