package com.taskmanagement.infrastructure.ui;

import com.taskmanagement.adapter.ai.AiConfig;
import com.taskmanagement.adapter.ai.AiProtocol;
import com.taskmanagement.adapter.ai.CompositeAiAdapter;
import com.taskmanagement.adapter.persistence.IOSaveToMemory;
import com.taskmanagement.adapter.persistence.IOSaveToMemoryProject;
import com.taskmanagement.application.boundary.AiTaskInputBoundary;
import com.taskmanagement.application.boundary.ProjectInputBoundary;
import com.taskmanagement.application.boundary.TaskInputBoundary;
import com.taskmanagement.application.control.AiTaskControl;
import com.taskmanagement.application.control.ProjectControl;
import com.taskmanagement.application.control.TaskControl;
import com.taskmanagement.domain.project.entity.Project;
import com.taskmanagement.domain.task.entity.Task;
import com.taskmanagement.domain.task.enums.Priority;
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
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("IOConsole Interactive Client")
class IOConsoleTest {
    private IOSaveToMemory taskRepository;
    private IOSaveToMemoryProject projectRepository;
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

        taskInput = new TaskControl(taskRepository, projectRepository, dispatcher);
        projectInput = new ProjectControl(projectRepository);
        aiTaskInput = new AiTaskControl(taskRepository, projectRepository, new CompositeAiAdapter(AiConfig.defaults()), dispatcher);
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
        // 0 (Exit program from main menu)
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
        // User inputs:
        // 7 (Choose AI Assistant)
        // 8 (Xem / Thay doi cau hinh)
        // y (Confirm change)
        // https://gen.pollinations.ai/v1 (Base URL)
        // qwen/qwen3.7-flash (Model ID)
        // dummy-pollinations-key (API key)
        // 0 (Exit program)
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
        assertTrue(output.contains("DA CAP NHAT CAU HINH AI THANH CONG"));
        assertTrue(output.contains("https://gen.pollinations.ai/v1"));
        assertTrue(output.contains("qwen/qwen3.7-flash"));

        var cfg = aiTaskInput.getAiConfig();
        assertEquals("https://gen.pollinations.ai/v1", cfg.getBaseUrl());
        assertEquals("qwen/qwen3.7-flash", cfg.getModel());
        assertEquals("dummy-pollinations-key", cfg.getApiKey());
    }

    @Test
    @DisplayName("Option 7 subchoice 7 registers and activates custom Anthropic provider interactively")
    void testOption7RegisterCustomAnthropicProvider() {
        // User inputs:
        // 7 (AI menu)
        // 7 (Quan ly Providers)
        // 2 (Them Provider tuy chinh moi)
        // my-claude (ID)
        // My Claude 3.5 (Name)
        // 2 (Anthropic standard)
        // https://api.anthropic.com/v1 (Base URL)
        // claude-3-5-sonnet-20241022 (Model)
        // sk-ant-test-key-12345 (Key)
        // y (Activate now)
        // 0 (Exit program)
        String simulatedInput = String.join("\n",
            "7",
            "7",
            "2",
            "my-claude",
            "My Claude 3.5",
            "2",
            "https://api.anthropic.com/v1",
            "claude-3-5-sonnet-20241022",
            "sk-ant-test-key-12345",
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
        assertTrue(output.contains("DA DANG KY PROVIDER MOI THANH CONG: my-claude [ANTHROPIC]"));
        assertTrue(output.contains("DA CHUYEN ACTIVE PROVIDER SANG: my-claude"));

        var active = aiTaskInput.getActiveProvider();
        assertNotNull(active);
        assertEquals("my-claude", active.id());
        assertEquals(AiProtocol.ANTHROPIC, active.protocol());
    }

    @Test
    @DisplayName("Option 7 subchoice 7 option 3 tests registered provider models and updates default model")
    void testOption7VerifyAndDiscoverModels() {
        // User inputs:
        // 7 (AI menu)
        // 7 (Quan ly Providers)
        // 3 (Kiem tra Key & Lay danh sach Models)
        // 1 (Kiem tra Provider da dang ky)
        // simulated (ID Provider can kiem tra)
        // y (Confirm cap nhat model mac dinh)
        // 2 (Chon model thu 2 trong danh sach: heuristic-fast)
        // 0 (Exit program)
        String simulatedInput = String.join("\n",
            "7",
            "7",
            "3",
            "1",
            "simulated",
            "y",
            "2",
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
        assertTrue(output.contains("DA CAP NHAT MODEL MAC DINH CUA simulated THANH: heuristic-fast"));

        var providers = aiTaskInput.getProviders();
        var simulated = providers.stream().filter(p -> "simulated".equalsIgnoreCase(p.id())).findFirst().orElseThrow();
        assertEquals("heuristic-fast", simulated.defaultModel());
    }
}
