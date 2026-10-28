package com.taskmanagement.adapter.api;

import com.taskmanagement.adapter.ai.AiConfig;
import com.taskmanagement.adapter.ai.AiProtocol;
import com.taskmanagement.adapter.ai.CompositeAiAdapter;
import com.taskmanagement.adapter.ai.CustomProviderDefinition;
import com.taskmanagement.adapter.api.dto.AiAnalysisRequest;
import com.taskmanagement.adapter.api.dto.AiApplyDeadlineRequest;
import com.taskmanagement.adapter.api.dto.AiApplyDecompositionRequest;
import com.taskmanagement.adapter.api.dto.AiApplyPriorityRequest;
import com.taskmanagement.adapter.api.dto.AiConfigDto;
import com.taskmanagement.adapter.api.dto.ApiResponse;
import com.taskmanagement.adapter.api.dto.DraftSubtasksRequest;
import com.taskmanagement.adapter.api.dto.VerifyProviderRequest;
import com.taskmanagement.adapter.persistence.IOSaveToMemory;
import com.taskmanagement.adapter.persistence.IOSaveToMemoryProject;
import com.taskmanagement.application.boundary.AiTaskInputBoundary;
import com.taskmanagement.application.control.AiTaskControl;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AiTaskApiController Web Adapter")
class AiTaskApiControllerTest {

    private AiTaskApiController controller;
    private AiTaskApiController unconfiguredController;
    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private Task task;

    @BeforeEach
    void setUp() {
        taskRepository = new IOSaveToMemory();
        projectRepository = new IOSaveToMemoryProject();
        InMemoryDomainEventDispatcher dispatcher = new InMemoryDomainEventDispatcher();

        Project project = Project.create("Web Service", "Web REST services");
        projectRepository.save(project);

        task = Task.create(
            new TaskTitle("Implement OAuth2 Provider"),
            "Implement authorization code flow and token endpoint",
            Deadline.of(Instant.now().plus(Duration.ofDays(2))),
            Priority.MEDIUM,
            project.getId()
        );
        taskRepository.save(task);

        AiAssistantPort stub = new AiAssistantPort() {
            @Override
            public AiTaskDecompositionDto decomposeTask(Task task, Project project, String customPrompt) {
                return new AiTaskDecompositionDto(task.getId().value(), List.of("Subtask 1", "Subtask 2"), "Rationale");
            }

            @Override
            public AiTaskDecompositionDto suggestSubTasks(Task task, Project project, String customPrompt) {
                return new AiTaskDecompositionDto(task.getId().value(), List.of("Extra Subtask"), "Rationale");
            }

            @Override
            public AiDeadlineEstimationDto estimateDeadline(Task task, Project project, String customPrompt) {
                return new AiDeadlineEstimationDto(task.getId().value(), 16, Instant.now().plus(Duration.ofDays(3)), "HIGH", "Explanation");
            }

            @Override
            public AiPriorityRecommendationDto analyzePriority(Task task, Project project, Priority domainPriority, String customPrompt) {
                return new AiPriorityRecommendationDto(task.getId().value(), Priority.HIGH, domainPriority, Priority.HIGH, "Reasoning");
            }

            @Override
            public AiTaskSummaryDto generateSummary(Task task, Project project, String customPrompt) {
                return new AiTaskSummaryDto(task.getId().value(), task.getTitle().value(), "Summary text", 50.0, List.of("H1"));
            }

            @Override
            public AiAnalysisResultDto analyzeTaskComprehensive(Task task, Project project, Priority domainPriority, String customPrompt) {
                return new AiAnalysisResultDto(
                    task.getId().value(),
                    decomposeTask(task, project, customPrompt),
                    estimateDeadline(task, project, customPrompt),
                    analyzePriority(task, project, domainPriority, customPrompt),
                    generateSummary(task, project, customPrompt)
                );
            }
        };

        AiTaskInputBoundary aiInput = new AiTaskControl(
            taskRepository,
            projectRepository,
            new CompositeAiAdapter(stub),
            dispatcher
        );
        controller = new AiTaskApiController(aiInput);

        AiTaskInputBoundary unconfiguredAiInput = new AiTaskControl(
            taskRepository,
            projectRepository,
            new CompositeAiAdapter(AiConfig.defaults()),
            dispatcher
        );
        unconfiguredController = new AiTaskApiController(unconfiguredAiInput);
    }

    @Test
    @DisplayName("GET /api/tasks/ai/config and POST /api/tasks/ai/config should retrieve and update configuration")
    void testConfigEndpoints() {
        ApiResponse<AiConfigDto> initial = controller.getConfig();
        assertNotNull(initial);
        assertTrue(initial.success());

        AiConfigDto updateReq = new AiConfigDto(
            null,
            "https://gen.pollinations.ai/v1",
            "qwen/qwen3.7-flash",
            "test-secret-key-12345",
            30
        );
        ApiResponse<AiConfigDto> updated = controller.updateConfig(updateReq);

        assertNotNull(updated);
        assertTrue(updated.success());
        assertEquals("https://gen.pollinations.ai/v1", updated.data().baseUrl());
        assertEquals("qwen/qwen3.7-flash", updated.data().model());
        assertTrue(updated.data().apiKey().contains("..."));
    }

    @Test
    @DisplayName("GET /api/tasks/ai/providers, POST /api/tasks/ai/providers, and activate provider")
    void testProvidersEndpoints() {
        ApiResponse<List<CustomProviderDefinition>> listResponse = controller.getProviders();
        assertNotNull(listResponse);
        assertTrue(listResponse.success());
        assertEquals(5, listResponse.data().size());

        // Register custom Anthropic provider
        CustomProviderDefinition customAnthropic = new CustomProviderDefinition(
            "custom-claude",
            "Custom Claude 3.5",
            AiProtocol.ANTHROPIC,
            "https://api.anthropic.com/v1",
            "sk-ant-test-key",
            "claude-3-5-sonnet-20241022",
            30
        );
        ApiResponse<CustomProviderDefinition> regResponse = controller.registerProvider(customAnthropic);
        assertNotNull(regResponse);
        assertTrue(regResponse.success());
        assertEquals("custom-claude", regResponse.data().id());
        assertEquals(AiProtocol.ANTHROPIC, regResponse.data().protocol());

        // Activate custom provider
        ApiResponse<CustomProviderDefinition> activateResponse = controller.activateProvider("custom-claude");
        assertNotNull(activateResponse);
        assertTrue(activateResponse.success());
        assertEquals("custom-claude", activateResponse.data().id());
    }

    @Test
    @DisplayName("POST /api/tasks/ai/providers/verify requires an API key")
    void testVerifyProviderEndpointRequiresKey() {
        VerifyProviderRequest request = new VerifyProviderRequest(
            "https://api.openai.com/v1",
            "",
            AiProtocol.OPENAI,
            5
        );
        ApiResponse<ProviderVerificationResultDto> response = unconfiguredController.verifyProvider(request);
        assertNotNull(response);
        assertNotNull(response.data());
        assertFalse(response.data().valid());
        assertEquals(400, response.data().statusCode());
    }

    @Test
    @DisplayName("POST /api/tasks/ai/providers/{providerId}/verify returns 404 when not found")
    void testVerifyRegisteredProviderNotFound() {
        ApiResponse<ProviderVerificationResultDto> notFoundResponse = controller.verifyRegisteredProvider("non-existent");
        assertNotNull(notFoundResponse);
        assertNotNull(notFoundResponse.data());
        assertFalse(notFoundResponse.data().valid());
        assertEquals(404, notFoundResponse.data().statusCode());
    }

    @Test
    @DisplayName("Calling AI features without active AI provider throws IllegalStateException")
    void testCallingAiFeaturesWithoutConnectedProviderThrows() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> unconfiguredController.decompose(task.getId().value(), null));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("POST /api/tasks/{taskId}/ai/decompose should return checklist when AI is connected")
    void testDecomposeEndpoint() {
        AiAnalysisRequest request = new AiAnalysisRequest("Custom system prompt");
        ApiResponse<AiTaskDecompositionDto> response = controller.decompose(task.getId().value(), request);

        assertNotNull(response);
        assertTrue(response.success());
        assertNotNull(response.data());
        assertFalse(response.data().subTasks().isEmpty());
    }

    @Test
    @DisplayName("POST /api/tasks/ai/draft-subtasks should decompose unpersisted draft")
    void testDraftSubtasksEndpoint() {
        DraftSubtasksRequest req = new DraftSubtasksRequest("Setup Redis Cache", "Use redisson", null, null);
        ApiResponse<AiTaskDecompositionDto> response = controller.draftSubtasks(req);

        assertNotNull(response);
        assertTrue(response.success());
        assertNotNull(response.data());
        assertFalse(response.data().subTasks().isEmpty());
    }

    @Test
    @DisplayName("POST /api/tasks/{taskId}/ai/suggest-subtasks should return suggested subtasks")
    void testSuggestSubtasksEndpoint() {
        ApiResponse<AiTaskDecompositionDto> response = controller.suggestSubTasks(task.getId().value(), null);

        assertNotNull(response);
        assertTrue(response.success());
        assertFalse(response.data().subTasks().isEmpty());
    }

    @Test
    @DisplayName("POST /api/tasks/{taskId}/ai/estimate-deadline should return estimated deadline")
    void testEstimateDeadlineEndpoint() {
        ApiResponse<AiDeadlineEstimationDto> response = controller.estimateDeadline(task.getId().value(), null);

        assertNotNull(response);
        assertTrue(response.success());
        assertTrue(response.data().estimatedHours() > 0);
        assertNotNull(response.data().recommendedDeadline());
    }

    @Test
    @DisplayName("POST /api/tasks/{taskId}/ai/priority should return priority analysis")
    void testPriorityEndpoint() {
        ApiResponse<AiPriorityRecommendationDto> response = controller.analyzePriority(task.getId().value(), null);

        assertNotNull(response);
        assertTrue(response.success());
        assertNotNull(response.data().effectivePriority());
    }

    @Test
    @DisplayName("POST /api/tasks/{taskId}/ai/summary should return task summary")
    void testSummaryEndpoint() {
        ApiResponse<AiTaskSummaryDto> response = controller.generateSummary(task.getId().value(), null);

        assertNotNull(response);
        assertTrue(response.success());
        assertEquals("Implement OAuth2 Provider", response.data().title());
    }

    @Test
    @DisplayName("POST /api/tasks/{taskId}/ai/analyze should return comprehensive analysis")
    void testAnalyzeComprehensiveEndpoint() {
        ApiResponse<AiAnalysisResultDto> response = controller.analyze(task.getId().value(), null);

        assertNotNull(response);
        assertTrue(response.success());
        assertNotNull(response.data().decomposition());
        assertNotNull(response.data().deadlineEstimation());
        assertNotNull(response.data().priorityRecommendation());
        assertNotNull(response.data().summary());
    }

    @Test
    @DisplayName("POST /api/tasks/{taskId}/ai/apply-decomposition should apply subtasks to task")
    void testApplyDecompositionEndpoint() {
        AiApplyDecompositionRequest req = new AiApplyDecompositionRequest(List.of("Token endpoint", "Refresh token"));
        ApiResponse<Void> response = controller.applyDecomposition(task.getId().value(), req);

        assertNotNull(response);
        assertTrue(response.success());

        Task reloaded = taskRepository.findById(task.getId()).orElseThrow();
        assertEquals(2, reloaded.getSubTasks().size());
    }

    @Test
    @DisplayName("POST /api/tasks/{taskId}/ai/apply-deadline should reschedule task")
    void testApplyDeadlineEndpoint() {
        Instant newDeadline = Instant.now().plus(Duration.ofDays(7));
        AiApplyDeadlineRequest req = new AiApplyDeadlineRequest(newDeadline);
        ApiResponse<Void> response = controller.applyDeadline(task.getId().value(), req);

        assertNotNull(response);
        assertTrue(response.success());

        Task reloaded = taskRepository.findById(task.getId()).orElseThrow();
        assertEquals(newDeadline, reloaded.getDueDate().dueDate());
    }

    @Test
    @DisplayName("POST /api/tasks/{taskId}/ai/apply-priority should change task priority")
    void testApplyPriorityEndpoint() {
        AiApplyPriorityRequest req = new AiApplyPriorityRequest(Priority.HIGH);
        ApiResponse<Void> response = controller.applyPriority(task.getId().value(), req);

        assertNotNull(response);
        assertTrue(response.success());

        Task reloaded = taskRepository.findById(task.getId()).orElseThrow();
        assertEquals(Priority.HIGH, reloaded.getPriority());
    }
}
