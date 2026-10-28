package com.taskmanagement.infrastructure.ui;

import com.taskmanagement.adapter.ai.AiConfig;
import com.taskmanagement.adapter.ai.AiProtocol;
import com.taskmanagement.adapter.ai.CompositeAiAdapter;
import com.taskmanagement.adapter.ai.CustomProviderDefinition;
import com.taskmanagement.adapter.persistence.IOSaveToMemory;
import com.taskmanagement.adapter.persistence.IOSaveToMemoryProject;
import com.taskmanagement.application.boundary.AiTaskInputBoundary;
import com.taskmanagement.application.boundary.ProjectInputBoundary;
import com.taskmanagement.application.boundary.TaskInputBoundary;
import com.taskmanagement.application.control.AiTaskControl;
import com.taskmanagement.application.control.ProjectControl;
import com.taskmanagement.application.control.TaskControl;
import com.taskmanagement.application.dto.ai.AiAnalysisResultDto;
import com.taskmanagement.application.dto.ai.AiDeadlineEstimationDto;
import com.taskmanagement.application.dto.ai.AiPriorityRecommendationDto;
import com.taskmanagement.application.dto.ai.AiTaskDecompositionDto;
import com.taskmanagement.application.dto.ai.AiTaskSummaryDto;
import com.taskmanagement.application.dto.ai.ProviderVerificationResultDto;
import com.taskmanagement.application.port.AiAssistantPort;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.project.repository.ProjectRepository;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
import com.taskmanagement.domain.task.repository.TaskRepository;
import com.taskmanagement.domain.task.valueobject.Deadline;
import com.taskmanagement.domain.task.valueobject.TaskTitle;
import com.taskmanagement.infrastructure.event.InMemoryDomainEventDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IOConsoleTest {

    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private InMemoryDomainEventDispatcher dispatcher;
    private TaskInputBoundary taskInput;
    private ProjectInputBoundary projectInput;
    private AiTaskInputBoundary aiTaskInput;
    private Task task;

    @BeforeEach
    void setUp() {
        taskRepository = new IOSaveToMemory();
        projectRepository = new IOSaveToMemoryProject();
        dispatcher = new InMemoryDomainEventDispatcher();

        Project project = Project.create("Console AI Project", "Test console");
        projectRepository.save(project);

        task = Task.create(
            new TaskTitle("Test AI via Console"),
            "Interactive AI console testing",
            Deadline.of(Instant.now().plus(Duration.ofDays(3))),
            Priority.MEDIUM,
            project.getId()
        );
        taskRepository.save(task);

        AiAssistantPort stub = new AiAssistantPort() {
            @Override
            public AiTaskDecompositionDto decomposeTask(Task task, Project project, String customPrompt) {
                return new AiTaskDecompositionDto(task.getId().value(), List.of("Subtask 1"), "Rationale");
            }
            @Override
            public AiTaskDecompositionDto suggestSubTasks(Task task, Project project, String customPrompt) {
                return new AiTaskDecompositionDto(task.getId().value(), List.of("Subtask 2"), "Rationale");
            }
            @Override
            public AiDeadlineEstimationDto estimateDeadline(Task task, Project project, String customPrompt) {
                return new AiDeadlineEstimationDto(task.getId().value(), 8, Instant.now().plus(Duration.ofDays(2)), "HIGH", "Reason");
            }
            @Override
            public AiPriorityRecommendationDto analyzePriority(Task task, Project project, Priority domainPriority, String customPrompt) {
                return new AiPriorityRecommendationDto(task.getId().value(), Priority.HIGH, domainPriority, Priority.HIGH, "Reason");
            }
            @Override
            public AiTaskSummaryDto generateSummary(Task task, Project project, String customPrompt) {
                return new AiTaskSummaryDto(task.getId().value(), task.getTitle().value(), "Summary", 100.0, List.of("Point 1"));
            }
            @Override
            public AiAnalysisResultDto analyzeTaskComprehensive(Task task, Project project, Priority domainPriority, String customPrompt) {
                return new AiAnalysisResultDto(task.getId().value(), decomposeTask(task, project, customPrompt), estimateDeadline(task, project, customPrompt), analyzePriority(task, project, domainPriority, customPrompt), generateSummary(task, project, customPrompt));
            }
            @Override
            public ProviderVerificationResultDto verifyAndDiscoverModels(String baseUrl, String apiKey, AiProtocol protocol, int timeoutSeconds) {
                return ProviderVerificationResultDto.success(200, List.of("model-alpha", "model-beta"), "OPENAI");
            }
        };

        taskInput = new TaskControl(taskRepository, projectRepository, dispatcher);
        projectInput = new ProjectControl(projectRepository);
        aiTaskInput = new AiTaskControl(taskRepository, projectRepository, new CompositeAiAdapter(stub), dispatcher);
    }

    @Test
    @DisplayName("Option 7 triggers AI assistant flow and applies subtasks")
    void testOption7AiAssistantFlow() {
        // User inputs:
        // 7 (Choose AI Assistant from main menu)
        // 2 (Boc tach checklist from AI menu)
        // <task UUID> (Enter task UUID)
        // [Enter] (Default system prompt)
        // y (Confirm apply subtasks)
        // 0 (Back / exit)
        String simulatedInput = String.join("\n",
            "7",
            "2",
            task.getId().value().toString(),
            "",
            "y",
            "0"
        ) + "\n";

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outContent);
        ConsoleInputReader inputReader = new ConsoleInputReader(new Scanner(simulatedInput));
        ConsoleTableRenderer tableRenderer = new ConsoleTableRenderer(printStream);

        IOConsole console = new IOConsole(taskInput, projectInput, aiTaskInput, inputReader, tableRenderer);
        console.start();

        String output = outContent.toString();
        assertTrue(output.contains("TRO LY AI TASK ASSISTANT"));
        assertTrue(output.contains("DA THEM SUBTASKS VAO TASK THANH CONG"));
        assertTrue(output.contains("Cam on ban da su dung chuong trinh"));

        Task reloaded = taskRepository.findById(task.getId()).orElseThrow();
        assertTrue(reloaded.getSubTasks().size() > 0);
    }

    @Test
    @DisplayName("Option 7 subchoice 8 updates AI configuration interactively")
    void testOption7ConfigureAi() {
        String simulatedInput = String.join("\n",
            "7",
            "8",
            "y",
            "https://gen.pollinations.ai/v1",
            "qwen/qwen3.7-flash",
            "dummy-pollinations-key",
            "0"
        ) + "\n";

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outContent);
        ConsoleInputReader inputReader = new ConsoleInputReader(new Scanner(simulatedInput));
        ConsoleTableRenderer tableRenderer = new ConsoleTableRenderer(printStream);

        IOConsole console = new IOConsole(taskInput, projectInput, aiTaskInput, inputReader, tableRenderer);
        console.start();

        String output = outContent.toString();
        assertTrue(output.contains("CAU HINH HIEN TAI"));
        assertTrue(output.contains("CAP NHAT CAU HINH"));

        AiConfig current = aiTaskInput.getAiConfig();
        assertNotNull(current);
        assertEquals("https://gen.pollinations.ai/v1", current.getBaseUrl());
        assertEquals("qwen/qwen3.7-flash", current.getModel());
        assertEquals("dummy-pollinations-key", current.getApiKey());
    }

    @Test
    @DisplayName("Option 7 subchoice 7 manages custom AI providers interactively")
    void testOption7ManageCustomProviders() {
        String simulatedInput = String.join("\n",
            "7",
            "7",
            "2",
            "custom-claude",
            "My Enterprise Claude",
            "2",
            "https://api.anthropic.com/v1",
            "claude-3-5-sonnet-20241022",
            "sk-ant-test-key-999",
            "y",
            "0"
        ) + "\n";

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outContent);
        ConsoleInputReader inputReader = new ConsoleInputReader(new Scanner(simulatedInput));
        ConsoleTableRenderer tableRenderer = new ConsoleTableRenderer(printStream);

        IOConsole console = new IOConsole(taskInput, projectInput, aiTaskInput, inputReader, tableRenderer);
        console.start();

        String output = outContent.toString();
        assertTrue(output.contains("DANH SACH PROVIDERS DA DANG KY"));
        assertTrue(output.contains("DA DANG KY PROVIDER MOI THANH CONG: custom-claude [ANTHROPIC]"));
        assertTrue(output.contains("DA CHUYEN ACTIVE PROVIDER SANG: custom-claude"));

        CustomProviderDefinition active = aiTaskInput.getActiveProvider();
        assertNotNull(active);
        assertEquals("custom-claude", active.id());
        assertEquals(AiProtocol.ANTHROPIC, active.protocol());
    }

    @Test
    @DisplayName("Option 7 subchoice 7 option 3 tests registered provider models and updates default model")
    void testOption7VerifyAndDiscoverModels() {
        String simulatedInput = String.join("\n",
            "7",
            "7",
            "3",
            "2",
            "1",
            "https://api.test.com/v1",
            "test-key-123",
            "y",
            "custom-verified",
            "Custom Verified",
            "2",
            "y",
            "0"
        ) + "\n";

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outContent);
        ConsoleInputReader inputReader = new ConsoleInputReader(new Scanner(simulatedInput));
        ConsoleTableRenderer tableRenderer = new ConsoleTableRenderer(printStream);

        IOConsole console = new IOConsole(taskInput, projectInput, aiTaskInput, inputReader, tableRenderer);
        console.start();

        String output = outContent.toString();
        assertTrue(output.contains("KET QUA KIEM TRA PROVIDER & MODELS"));
        assertTrue(output.contains("HOP LE / THANH CONG"));
        assertTrue(output.contains("DA LUU VA DANG KY PROVIDER MOI THANH CONG: custom-verified voi Model: model-beta"));

        var providers = aiTaskInput.getProviders();
        var custom = providers.stream().filter(p -> "custom-verified".equalsIgnoreCase(p.id())).findFirst().orElseThrow();
        assertEquals("model-beta", custom.defaultModel());
        assertEquals("custom-verified", aiTaskInput.getActiveProvider().id());
    }
}
