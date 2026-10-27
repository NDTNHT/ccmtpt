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
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AiTaskApiController Web Adapter")
class AiTaskApiControllerTest {

    private AiTaskApiController controller;
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

        AiTaskInputBoundary aiInput = new AiTaskControl(
            taskRepository,
            projectRepository,
            new CompositeAiAdapter(AiConfig.defaults()),
            dispatcher
        );
        controller = new AiTaskApiController(aiInput);
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
        assertTrue(listResponse.data().size() >= 5);

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
    @DisplayName("POST /api/tasks/ai/providers/verify should verify provider and discover models")
    void testVerifyProviderEndpoint() {
        VerifyProviderRequest request = new VerifyProviderRequest(
            "https://api.openai.com/v1",
            "",
            AiProtocol.SIMULATED,
            5
        );
        ApiResponse<ProviderVerificationResultDto> response = controller.verifyProvider(request);
        assertNotNull(response);
        assertTrue(response.success());
        assertNotNull(response.data());
        assertTrue(response.data().valid());
        assertEquals(200, response.data().statusCode());
        assertFalse(response.data().availableModels().isEmpty());
    }

    @Test
    @DisplayName("POST /api/tasks/ai/providers/{providerId}/verify should verify registered provider")
    void testVerifyRegisteredProviderEndpoint() {
        ApiResponse<ProviderVerificationResultDto> response = controller.verifyRegisteredProvider("simulated");
        assertNotNull(response);
        assertTrue(response.success());
        assertNotNull(response.data());
        assertTrue(response.data().valid());

        ApiResponse<ProviderVerificationResultDto> notFoundResponse = controller.verifyRegisteredProvider("non-existent");
        assertNotNull(notFoundResponse);
        assertNotNull(notFoundResponse.data());
        assertFalse(notFoundResponse.data().valid());
        assertEquals(404, notFoundResponse.data().statusCode());
    }

    @Test
    @DisplayName("POST /api/tasks/{taskId}/ai/decompose should return checklist")
    void testDecomposeEndpoint() {
        AiAnalysisRequest request = new AiAnalysisRequest("Custom system prompt");
        ApiResponse<AiTaskDecompositionDto> response = controller.decompose(task.getId().value(), request);

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
